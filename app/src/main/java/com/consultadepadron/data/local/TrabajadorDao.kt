package com.consultadepadron.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Fila del dashboard Bento: los 4 KPIs resueltos en una sola consulta SQL. */
data class KpisDto(
    val total: Int,
    val permitidos: Int,
    val evaluar: Int,
    val alerta: Int
)

/** Institución única del periodo con su peso en el padrón. */
data class InstitucionDto(
    val nombre: String,
    val total: Int
)

@Dao
abstract class TrabajadorDao {

    // ---------------------------------------------------------------- escritura

    /** Inserción por lotes: Room envuelve cada llamada en su propia transacción. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarLote(items: List<TrabajadorEntity>)

    @Query("DELETE FROM trabajador WHERE periodo = :periodo")
    abstract suspend fun borrarTrabajadoresDe(periodo: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPeriodo(periodo: PeriodoEntity)

    @Query("UPDATE periodos SET totalRegistros = :total WHERE nombre = :nombre")
    abstract suspend fun actualizarTotalPeriodo(nombre: String, total: Int)

    @Query("DELETE FROM periodos WHERE nombre = :nombre")
    abstract suspend fun borrarPeriodo(nombre: String)

    /** Reimportar el mismo periodo reemplaza el padrón anterior (no lo duplica). */
    @Transaction
    open suspend fun limpiarPeriodo(periodo: String) {
        borrarTrabajadoresDe(periodo)
        borrarPeriodo(periodo)
    }

    @Query("DELETE FROM trabajador")
    abstract suspend fun borrarTodoLosTrabajadores()

    @Query("DELETE FROM periodos")
    abstract suspend fun borrarTodosLosPeriodos()

    // ---------------------------------------------------------------- lectura

    /** Observable para que el combo box de periodos se actualice solo tras cada import. */
    @Query("SELECT * FROM periodos ORDER BY fechaCarga DESC, id DESC")
    abstract fun observarPeriodos(): Flow<List<PeriodoEntity>>

    @Query("SELECT COALESCE(SUM(totalRegistros), 0) FROM periodos")
    abstract suspend fun contarRegistrosAlmacenados(): Int

    /**
     * KPIs del dashboard sin cargar filas en memoria.
     * [institucion] null = todo el periodo.
     */
    @Query(
        """
        SELECT
            COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN estado = 'PERMITIDO' THEN 1 ELSE 0 END), 0) AS permitidos,
            COALESCE(SUM(CASE WHEN estado = 'EVALUAR'   THEN 1 ELSE 0 END), 0) AS evaluar,
            COALESCE(SUM(CASE WHEN estado = 'ALERTA'    THEN 1 ELSE 0 END), 0) AS alerta
        FROM trabajador
        WHERE periodo = :periodo
          AND (:institucion IS NULL OR institucion = :institucion)
        """
    )
    abstract suspend fun obtenerKpis(periodo: String, institucion: String?): KpisDto

    @Query(
        """
        SELECT institucion AS nombre, COUNT(*) AS total
        FROM trabajador
        WHERE periodo = :periodo AND TRIM(institucion) <> ''
        GROUP BY institucion
        ORDER BY institucion COLLATE NOCASE ASC
        """
    )
    abstract suspend fun listarInstituciones(periodo: String): List<InstitucionDto>

    /**
     * Buscador inteligente: DNI/código modular por coincidencia parcial y apellidos/nombres
     * en la misma consulta, con los tres filtros combinados y LIMIT para 60 FPS.
     */
    @Query(
        """
        SELECT * FROM trabajador
        WHERE periodo = :periodo
          AND (:institucion IS NULL OR institucion = :institucion)
          AND (:estado IS NULL OR estado = :estado)
          AND (
                :query = ''
             OR codMod  LIKE '%' || :query || '%'
             OR paterno LIKE '%' || :query || '%'
             OR materno LIKE '%' || :query || '%'
             OR nombres LIKE '%' || :query || '%'
          )
        ORDER BY
            CASE estado WHEN 'ALERTA' THEN 0 WHEN 'EVALUAR' THEN 1 ELSE 2 END ASC,
            paterno COLLATE NOCASE ASC,
            paterno ASC
        LIMIT :limite
        """
    )
    abstract suspend fun buscar(
        periodo: String,
        institucion: String?,
        estado: String?,
        query: String,
        limite: Int
    ): List<TrabajadorEntity>
}