package com.consultadepadron.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.consultadepadron.ConsultaPadronApp
import com.consultadepadron.data.ExcelImporter
import com.consultadepadron.data.Kpis
import com.consultadepadron.data.PadronRepository
import com.consultadepadron.data.local.InstitucionDto
import com.consultadepadron.data.local.PeriodoEntity
import com.consultadepadron.data.local.TrabajadorEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Filtros combinados del buscador. */
data class Filtro(
    val institucion: String? = null,
    val estado: String? = null,
    val query: String = ""
)

/** Estado en vivo del diálogo de importación. */
data class ProgresoImportacion(
    val periodo: String,
    val archivo: String = "",
    val leidas: Int = 0,
    val guardadas: Int = 0
)

data class PadronUiState(
    val periodos: List<PeriodoEntity> = emptyList(),
    val periodoSeleccionado: String? = null,
    val instituciones: List<InstitucionDto> = emptyList(),
    val filtro: Filtro = Filtro(),
    val kpis: Kpis = Kpis(),
    val resultados: List<TrabajadorEntity> = emptyList(),
    val cargando: Boolean = false,
    val importacion: ProgresoImportacion? = null
) {
    val hayPadrones: Boolean get() = periodos.isNotEmpty()
    val resultadosVisibles: Int get() = resultados.size
    val limiteAlcanzado: Boolean get() = resultados.size >= LIMITE_BUSQUEDA

    companion object {
        const val LIMITE_BUSQUEDA = 100
    }
}

private data class Base(
    val periodos: List<PeriodoEntity>,
    val periodo: String?,
    val instituciones: List<InstitucionDto>,
    val kpis: Kpis,
    val resultados: List<TrabajadorEntity>
)

private data class Panel(
    val filtro: Filtro,
    val cargando: Boolean,
    val importacion: ProgresoImportacion?
)

/** Consulta combinada: cualquier cambio de periodo, filtro o versión dispara una recarga. */
private data class Consulta(
    val periodo: String,
    val filtro: Filtro,
    val version: Long
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
class PadronViewModel(
    private val application: Application,
    private val repositorio: PadronRepository
) : AndroidViewModel(application) {

    private val _periodos = MutableStateFlow<List<PeriodoEntity>>(emptyList())
    private val _periodo = MutableStateFlow<String?>(null)
    private val _instituciones = MutableStateFlow<List<InstitucionDto>>(emptyList())
    private val _kpis = MutableStateFlow(Kpis())
    private val _filtro = MutableStateFlow(Filtro())
    private val _resultados = MutableStateFlow<List<TrabajadorEntity>>(emptyList())
    private val _cargando = MutableStateFlow(false)
    private val _importacion = MutableStateFlow<ProgresoImportacion?>(null)

    /**
     * Se incrementa al terminar una importación para forzar la recarga aunque el periodo
     * seleccionado siga siendo el mismo. Sin esto, el primer import seleccionaba el periodo
     * vacío antes de insertar las filas y la UI se quedaba en 0 hasta reiniciar la app.
     */
    private val _recarga = MutableStateFlow(0L)

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    val uiState: StateFlow<PadronUiState> = combine(
        combine(_periodos, _periodo, _instituciones, _kpis, _resultados) { periodos, periodo, inst, kpis, res ->
            Base(periodos, periodo, inst, kpis, res)
        },
        combine(_filtro, _cargando, _importacion) { filtro, cargando, importacion ->
            Panel(filtro, cargando, importacion)
        }
    ) { base, panel ->
        PadronUiState(
            periodos = base.periodos,
            periodoSeleccionado = base.periodo,
            instituciones = base.instituciones,
            filtro = panel.filtro,
            kpis = base.kpis,
            resultados = base.resultados,
            cargando = panel.cargando,
            importacion = panel.importacion
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PadronUiState())

    init {
        // 1. Cabecera de periodos: alimenta el combo box y auto-selecciona el más reciente.
        viewModelScope.launch {
            repositorio.periodosFlow.collect { lista ->
                _periodos.value = lista

                // Mientras se importa no se auto-selecciona el periodo recién registrado:
                // todavía no tiene filas y la UI mostraría 0. Al terminar, [importar] lo
                // selecciona y fuerza la recarga con los datos completos.
                if (_importacion.value != null) return@collect

                val actual = _periodo.value
                val seleccionado = when {
                    actual != null && lista.any { it.nombre == actual } -> actual
                    else -> lista.firstOrNull()?.nombre
                }

                if (seleccionado != actual) {
                    _filtro.value = Filtro()
                    _periodo.value = seleccionado

                    if (seleccionado == null) {
                        _instituciones.value = emptyList()
                        _kpis.value = Kpis()
                        _resultados.value = emptyList()
                        _cargando.value = false
                    }
                }
            }
        }

        // 2. Instituciones del periodo (se refrescan al cambiar de periodo o tras importar).
        viewModelScope.launch {
            combine(_periodo.filterNotNull(), _recarga) { periodo, version -> periodo to version }
                .distinctUntilChanged()
                .collectLatest { (periodo, _) ->
                    _instituciones.value = repositorio.instituciones(periodo)
                }
        }

        // 3. KPIs + resultados: una sola consulta combinada con debounce de 250 ms.
        viewModelScope.launch {
            combine(_periodo, _filtro, _recarga) { periodo, filtro, version ->
                periodo?.let { Consulta(it, filtro, version) }
            }
                .filterNotNull()
                .debounce { consulta -> if (consulta.filtro.query.isBlank()) 0L else 250L }
                .distinctUntilChanged()
                .collectLatest { consulta ->
                    _cargando.value = true
                    try {
                        _kpis.value = repositorio.kpis(
                            consulta.periodo,
                            consulta.filtro.institucion
                        )
                        _resultados.value = repositorio.buscar(
                            periodo = consulta.periodo,
                            institucion = consulta.filtro.institucion,
                            estado = consulta.filtro.estado,
                            query = consulta.filtro.query,
                            limite = PadronUiState.LIMITE_BUSQUEDA
                        )
                    } finally {
                        _cargando.value = false
                    }
                }
        }
    }

    // ------------------------------------------------------------------ acciones

    fun buscar(texto: String) {
        _filtro.update { it.copy(query = texto) }
    }

    fun filtrarPorEstado(estado: String?) {
        _filtro.update { it.copy(estado = estado) }
    }

    fun filtrarPorInstitucion(institucion: String?) {
        _filtro.update { it.copy(institucion = institucion) }
    }

    fun limpiarFiltros() {
        _filtro.value = Filtro()
    }

    fun seleccionarPeriodo(nombre: String) {
        if (_periodo.value == nombre) return
        _filtro.value = Filtro()
        _periodo.value = nombre
    }

    fun sugerirPeriodo(nombreArchivo: String): String = ExcelImporter.sugerirPeriodo(nombreArchivo)

    fun importar(uri: Uri, periodo: String) {
        if (_importacion.value != null) return
        viewModelScope.launch {
            _importacion.value = ProgresoImportacion(periodo = periodo)
            try {
                val resultado = repositorio.importarExcel(
                    context = getApplication(),
                    uri = uri,
                    periodo = periodo
                ) { leidas, guardadas ->
                    _importacion.value = (_importacion.value ?: ProgresoImportacion(periodo))
                        .copy(leidas = leidas, guardadas = guardadas)
                }
                _filtro.value = Filtro()
                _periodo.value = periodo

                // Aunque el periodo ya estuviera seleccionado (p. ej. una reimportación),
                // la versión fuerza a leer de nuevo KPIs, instituciones y resultados.
                _recarga.update { it + 1 }

                _mensajes.emit(
                    "Importación completa · ${resultado.filasGuardadas} registros guardados"
                )
            } catch (e: Exception) {
                // Si el libro se leyó pero falló a medias, el periodo queda incompleto: se limpia.
                runCatching { repositorio.eliminarPeriodo(periodo) }
                if (_periodo.value == periodo) {
                    _periodo.value = _periodos.value
                        .firstOrNull { it.nombre != periodo }
                        ?.nombre
                }
                _mensajes.emit(e.message ?: "No se pudo importar el archivo.")
            } finally {
                _importacion.value = null
            }
        }
    }

    fun eliminarPeriodo(periodo: String) {
        viewModelScope.launch {
            repositorio.eliminarPeriodo(periodo)
            _mensajes.emit("Se eliminó el padrón \"$periodo\".")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as ConsultaPadronApp
                PadronViewModel(app, app.repository)
            }
        }
    }
}