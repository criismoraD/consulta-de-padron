package com.consultadepadron.data

import android.content.Context
import android.net.Uri
import com.consultadepadron.data.local.InstitucionDto
import com.consultadepadron.data.local.KpisDto
import com.consultadepadron.data.local.PeriodoEntity
import com.consultadepadron.data.local.TrabajadorDao
import com.consultadepadron.data.local.TrabajadorEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** KPIs del dashboard ya con los porcentajes calculados para pintar la UI. */
data class Kpis(
    val total: Int = 0,
    val permitidos: Int = 0,
    val evaluar: Int = 0,
    val alerta: Int = 0
) {
    val porcentajePermitidos: Int get() = porcentaje(permitidos)
    val porcentajeEvaluar: Int get() = porcentaje(evaluar)
    val porcentajeAlerta: Int get() = porcentaje(alerta)

    private fun porcentaje(valor: Int): Int = if (total <= 0) 0 else (valor * 100.0 / total).toInt()
}

/**
 * Contrato de la capa de datos. Hoy solo existe una implementación local (Room);
 * cuando se sume la nube basta con añadir una fuente remota y componerla en
 * [PadronRepository] sin tocar ni el ViewModel ni la UI.
 */
interface PadronDataSource {
    fun observarPeriodos(): Flow<List<PeriodoEntity>>
    suspend fun contarRegistrosAlmacenados(): Int
    suspend fun instituciones(periodo: String): List<InstitucionDto>
    suspend fun kpis(periodo: String, institucion: String?): Kpis
    suspend fun buscar(
        periodo: String,
        institucion: String?,
        estado: String?,
        query: String,
        limite: Int = 100
    ): List<TrabajadorEntity>

    suspend fun prepararPeriodo(periodo: String)
    suspend fun registrarPeriodo(periodo: PeriodoEntity)
    suspend fun insertarLote(lote: List<TrabajadorEntity>)
    suspend fun finalizarPeriodo(nombre: String, total: Int)
    suspend fun eliminarPeriodo(periodo: String)
    suspend fun borrarTodo()
}

/** Implementación local: SQLite (Room) en el almacenamiento interno privado del dispositivo. */
class PadronLocalDataSource(private val dao: TrabajadorDao) : PadronDataSource {

    override fun observarPeriodos(): Flow<List<PeriodoEntity>> = dao.observarPeriodos()

    override suspend fun contarRegistrosAlmacenados(): Int = dao.contarRegistrosAlmacenados()

    override suspend fun instituciones(periodo: String): List<InstitucionDto> =
        dao.listarInstituciones(periodo)

    override suspend fun kpis(periodo: String, institucion: String?): Kpis =
        dao.obtenerKpis(periodo, institucion).let {
            Kpis(total = it.total, permitidos = it.permitidos, evaluar = it.evaluar, alerta = it.alerta)
        }

    override suspend fun buscar(
        periodo: String,
        institucion: String?,
        estado: String?,
        query: String,
        limite: Int
    ): List<TrabajadorEntity> = dao.buscar(periodo, institucion, estado, query.trim(), limite)

    override suspend fun prepararPeriodo(periodo: String) = dao.limpiarPeriodo(periodo)

    override suspend fun registrarPeriodo(periodo: PeriodoEntity) = dao.upsertPeriodo(periodo)

    override suspend fun insertarLote(lote: List<TrabajadorEntity>) = dao.insertarLote(lote)

    override suspend fun finalizarPeriodo(nombre: String, total: Int) =
        dao.actualizarTotalPeriodo(nombre, total)

    override suspend fun eliminarPeriodo(periodo: String) = dao.limpiarPeriodo(periodo)

    override suspend fun borrarTodo() {
        dao.borrarTodoLosTrabajadores()
        dao.borrarTodosLosPeriodos()
    }
}

/**
 * Punto único de entrada a los datos. Delega hoy en la fuente local; más adelante
 * puede envolver una fuente remota (nube) para sincronizar padrones descargados.
 */
class PadronRepository(
    private val local: PadronDataSource
) : PadronDataSource by local {

    /** Flujo completo de importación: validar archivo -> limpiar -> lotes -> cerrar periodo. */
    suspend fun importarExcel(
        context: Context,
        uri: Uri,
        periodo: String,
        onProgreso: (leidas: Int, guardadas: Int) -> Unit
    ): ExcelImporter.Resultado = ExcelImporter.importar(
        context = context,
        uri = uri,
        periodo = periodo,
        alValidarArchivo = {
            prepararPeriodo(periodo)
            registrarPeriodo(
                PeriodoEntity(
                    nombre = periodo,
                    archivoOrigen = uri.lastPathSegment ?: "",
                    fechaCarga = System.currentTimeMillis(),
                    totalRegistros = 0
                )
            )
        },
        onProgreso = onProgreso,
        guardarLote = { lote -> insertarLote(lote) }
    ).also { resultado -> finalizarPeriodo(periodo, resultado.filasGuardadas) }

    suspend fun totalAlmacenado(): Int = contarRegistrosAlmacenados()

    val periodosFlow: Flow<List<PeriodoEntity>> = local.observarPeriodos()
        .map { it.sortedByDescending(PeriodoEntity::fechaCarga) }
}