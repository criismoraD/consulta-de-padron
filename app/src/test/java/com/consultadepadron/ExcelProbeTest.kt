package com.consultadepadron

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.Test
import java.io.File
import java.io.FileInputStream

/**
 * Sonda de verificación: lee el padrón real del proyecto e imprime la hoja "BASE",
 * los valores de las 9 columnas por índice y los valores distintos de ESTADO y % DSCTO.
 * No forma parte de la app; sirve para confirmar que los índices implementados son correctos.
 */
class ExcelProbeTest {

    private val columnas = listOf(
        3 to "D TRABAJA", 4 to "E CODMOD", 7 to "H PATERNO", 8 to "I MATERNO", 9 to "J NOMBRES",
        45 to "AT BRUTO", 47 to "AV MONTO", 50 to "AY DSCTO", 51 to "AZ ESTADO"
    )

    private fun archivo(): File? {
        val candidatos = listOf(
            File("../PADRON DE CONSULTA SETIEMBRE 2026.xls"),
            File("PADRON DE CONSULTA SETIEMBRE 2026.xls"),
            File("../../PADRON DE CONSULTA SETIEMBRE 2026.xls")
        )
        return candidatos.firstOrNull { it.exists() }
    }

    @Test
    fun inspeccionarPadronReal() {
        val archivo = archivo()
        if (archivo == null) {
            println("PROBE: archivo no encontrado, se omite")
            return
        }
        println("PROBE: ${archivo.absolutePath} (${archivo.length() / 1024 / 1024} MB)")

        FileInputStream(archivo).use { entrada ->
            WorkbookFactory.create(entrada).use { libro ->
                println("PROBE hojas: " + (0 until libro.numberOfSheets).joinToString(", ") {
                    "${libro.getSheetName(it)}[${libro.getSheetAt(it).lastRowNum + 1}]"
                })

                val base = libro.getSheet("BASE")
                if (base == null) {
                    println("PROBE: NO EXISTE la hoja BASE")
                    return
                }
                println("PROBE BASE: firstRowNum=${base.firstRowNum} lastRowNum=${base.lastRowNum}")

                val fmt = DataFormatter()

                for (r in 0..2) {
                    val fila = base.getRow(r) ?: continue
                    println(
                        "PROBE fila $r :: " + columnas.joinToString(" | ") { (indice, nombre) ->
                            val celda = fila.getCell(indice)
                            "$nombre[$indice]=${describir(celda, fmt)}"
                        }
                    )
                }

                // Encabezado completo para validar nombres de columna.
                val encabezado = base.getRow(0)
                if (encabezado != null) {
                    val nombres = (0 until encabezado.lastCellNum).mapNotNull { indice ->
                        val texto = fmt.formatCellValue(encabezado.getCell(indice)).trim()
                        if (texto.isEmpty()) null else "$indice=$texto"
                    }
                    println("PROBE encabezados: " + nombres.joinToString(" ; "))
                }

                // Distribución de ESTADO (AZ) y % DSCTO (AY) leyendo el VALOR EN CACHÉ
                // de las fórmulas, que es exactamente lo que consume el importador.
                val estados = mutableMapOf<String, Int>()
                val descuentos = mutableListOf<Double>()
                val tiposAy = mutableMapOf<String, Int>()
                val limite = minOf(base.lastRowNum, 5000)
                var sinEstado = 0
                for (r in 1..limite) {
                    val fila = base.getRow(r) ?: continue

                    val celdaAz = fila.getCell(51)
                    val valorAz = valorEfectivo(celdaAz)
                    if (valorAz.isBlank()) sinEstado++
                    estados[valorAz] = (estados[valorAz] ?: 0) + 1

                    val celdaAy = fila.getCell(50)
                    tiposAy[celdaAy?.let { tipoCache(it) } ?: "<vacia>"] =
                        (tiposAy[celdaAy?.let { tipoCache(it) } ?: "<vacia>"] ?: 0) + 1
                    if (celdaAy != null) {
                        val tipo = tipoCache(celdaAy)
                        if (tipo == "NUMERIC") descuentos += celdaAy.numericCellValue
                    }
                }
                println("PROBE filas con ESTADO vacio: $sinEstado")
                println("PROBE AZ distintos (cache): " + estados.entries.sortedByDescending { it.value }
                    .joinToString(" ; ") { "[${it.key}]=${it.value}" })
                println("PROBE AY tipos: " + tiposAy.entries.joinToString(" ; ") { "[${it.key}]=${it.value}" })
                if (descuentos.isNotEmpty()) {
                    println(
                        "PROBE AY min=${descuentos.min()} max=${descuentos.max()} " +
                            "promedio=${"%.4f".format(descuentos.average())}"
                    )
                }

                val instituciones = mutableSetOf<String>()
                for (r in 1..limite) {
                    val valor = fmt.formatCellValue(base.getRow(r)?.getCell(3)).trim()
                    if (valor.isNotEmpty()) instituciones.add(valor)
                }
                println("PROBE instituciones (muestra): " + instituciones.take(8).joinToString(" ; "))
                println("PROBE total instituciones: ${instituciones.size}")
            }
        }
    }

    private fun tipoCache(celda: org.apache.poi.ss.usermodel.Cell): String {
        val tipo = if (celda.cellType == CellType.FORMULA) celda.cachedFormulaResultType else celda.cellType
        return tipo.name
    }

    /** Valor ya resuelto (caché de la fórmula), igual que lo lee ExcelImporter. */
    private fun valorEfectivo(celda: org.apache.poi.ss.usermodel.Cell?): String {
        if (celda == null) return "<vacia>"
        return when (tipoCache(celda)) {
            "STRING" -> celda.stringCellValue.trim()
            "NUMERIC" -> celda.numericCellValue.toString()
            "BOOLEAN" -> celda.booleanCellValue.toString()
            else -> "<${tipoCache(celda)}>"
        }
    }

    private fun describir(celda: org.apache.poi.ss.usermodel.Cell?, fmt: DataFormatter): String {
        if (celda == null) return "<vacia>"
        return when (celda.cellType) {
            CellType.STRING -> celda.stringCellValue.trim()
            CellType.NUMERIC -> "${celda.numericCellValue} | fmt='${fmt.formatCellValue(celda)}'"
            CellType.FORMULA -> "FORMULA(${celda.cellFormula}) -> ${fmt.formatCellValue(celda)}"
            CellType.BLANK -> "<vacia>"
            CellType.BOOLEAN -> celda.booleanCellValue.toString()
            else -> celda.cellType.name
        }
    }
}