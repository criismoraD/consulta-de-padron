package com.consultadepadron.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cabecera de cada padrón importado. Permite el combo box multi-periodo,
 * ordenar el más reciente arriba y borrar padrones antiguos para liberar espacio.
 */
@Entity(
    tableName = "periodos",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class PeriodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nombre: String,
    val archivoOrigen: String,
    val fechaCarga: Long,
    val totalRegistros: Int = 0
)