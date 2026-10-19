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
                val actual = _periodo.value
                if (actual == null || lista.none { it.nombre == actual }) {
                    _periodo.value = lista.firstOrNull()?.nombre
                }
            }
        }

        // 2. Cambio de periodo: recarga instituciones + KPIs + listado base.
        viewModelScope.launch {
            _periodo.filterNotNull().distinctUntilChanged().collectLatest { periodo ->
                _filtro.value = Filtro()
                _cargando.value = true
                _instituciones.value = repositorio.instituciones(periodo)
                _kpis.value = repositorio.kpis(periodo, null)
                _resultados.value = repositorio.buscar(periodo, null, null, "")
                _cargando.value = false
            }
        }

        // 3. Filtros combinados con debounce de 250 ms para escribir sin parpadear.
        viewModelScope.launch {
            _filtro
                .debounce { filtro -> if (filtro.query.isBlank()) 0L else 250L }
                .distinctUntilChanged()
                .collectLatest { filtro ->
                    val periodo = _periodo.value ?: return@collectLatest
                    _cargando.value = true
                    _kpis.value = repositorio.kpis(periodo, filtro.institucion)
                    _resultados.value = repositorio.buscar(
                        periodo = periodo,
                        institucion = filtro.institucion,
                        estado = filtro.estado,
                        query = filtro.query,
                        limite = PadronUiState.LIMITE_BUSQUEDA
                    )
                    _cargando.value = false
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
                _periodo.value = periodo
                _mensajes.emit(
                    "Importación completa · ${resultado.filasGuardadas} registros guardados"
                )
            } catch (e: Exception) {
                // Si el libro se leyó pero falló a medias, el periodo queda incompleto: se limpia.
                runCatching { repositorio.eliminarPeriodo(periodo) }
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