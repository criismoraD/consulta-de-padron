package com.consultadepadron.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila del padrón. Las columnas D, E, H, I, J, AT, AV, AY y AZ del Excel "BASE"
 * se mapean aquí; el resto de las +50 columnas del archivo se descartan en la importación.
 *
 * Los indices cubren los accesos reales de la app: cambio de periodo, búsqueda por
 * código/DNI, listado por institución, búsqueda por apellido y conteo por estado.
 */
@Entity(
    tableName = "trabajador",
    indices = [
        Index(value = ["periodo"]),
        Index(value = ["codMod"]),
        Index(value = ["institucion"]),
        Index(value = ["paterno"]),
        Index(value = ["estado"]),
        Index(value = ["periodo", "estado"]),
        Index(value = ["periodo", "institucion"]),
        Index(value = ["periodo", "paterno"])
    ]
)
data class TrabajadorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Periodo asignado por el asesor, p. ej. "Setiembre 2026". */
    val periodo: String,
    /** Columna D - TRABAJA: institución / unidad de gestión. */
    val institucion: String,
    /** Columna E - CODMOD: DNI (8) o código modular (10), siempre como texto. */
    val codMod: String,
    /** Columna H - PATERNO. */
    val paterno: String,
    /** Columna I - MATERNO. */
    val materno: String,
    /** Columna J - NOMBRES. */
    val nombres: String,
    /** Columna AT - BRUTO. */
    val bruto: Double,
    /** Columna AV - MONTO: neto líquido disponible. */
    val monto: Double,
    /** Columna AY - % DSCTO, normalizado a escala 0..100. */
    val descuento: Double,
    /** Columna AZ - ESTADO, normalizado a PERMITIDO / EVALUAR / ALERTA. */
    val estado: String
)

/** "PATERNO MATERNO, NOMBRES" para el encabezado de la tarjeta. */
val TrabajadorEntity.nombreCompleto: String
    get() = listOf(paterno, materno, nombres)
        .filter { it.isNotBlank() }
        .joinToString(" ")