package com.consultadepadron.data

import java.text.Normalizer

/**
 * Los tres estados limpios que consume la UI. Todo lo que llega en la columna AZ
 * del Excel se colapsa aquí; el valor por defecto es EVALUAR (nunca "apto" por descuido).
 */
object EstadoTrabajador {

    const val PERMITIDO = "PERMITIDO"
    const val EVALUAR = "EVALUAR"
    const val ALERTA = "ALERTA"

    val todos: List<String> = listOf(PERMITIDO, EVALUAR, ALERTA)

    fun normalizar(texto: String?): String {
        val t = limpiar(texto)
        if (t.isBlank()) return EVALUAR

        return when {
            contiene(t, "NO APTO", "NOAPTO", "NO APT", "ALERTA", "RECHAZ", "DENEG",
                "IMPI", "OBSERV", "SUSPEND", "DEUDA", "NO PROCED") -> ALERTA

            contiene(t, "EVALUAR", "EVAL", "REVIS", "PENDIENT", "PEND", "CONDIC",
                "VERIFIC", "OBSERVAR", "PREVIA") -> EVALUAR

            contiene(t, "PERMIT", "APTO", "APROB", "ACTIVO", "HABILIT", "VIGENTE",
                "CUMPLE", "PROCE") -> PERMITIDO

            else -> when (t) {
                "P", "1", "S", "SI", "OK", "A", "APT" -> PERMITIDO
                "E", "2", "D" -> EVALUAR
                "X", "3", "N", "NO", "NP" -> ALERTA
                else -> EVALUAR
            }
        }
    }

    private fun contiene(texto: String, vararg literales: String): Boolean =
        literales.any { texto.contains(it) }

    /** Mayúsculas, sin tildes y sin puntuación (la columna AZ trae "11 %", "-", etc.). */
    private fun limpiar(texto: String?): String {
        if (texto.isNullOrBlank()) return ""
        val sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return sinTildes
            .uppercase()
            .replace(Regex("[^A-Z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}