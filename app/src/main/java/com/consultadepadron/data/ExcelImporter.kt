package com.consultadepadron.data

import android.content.Context
import android.net.Uri
import com.consultadepadron.data.local.TrabajadorEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.NumberToTextConverter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedInputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * Lector de padrón de bajo consumo de memoria.
 *
 * Reglas de diseño (archivos de ~16 MB con +55 columnas y 4 hojas):
 *  1. Se abre SOLO la hoja "BASE"; las demás ni se tocan.
 *  2. Se accede a las 9 columnas necesarias por índice, sin recorrer la fila entera.
 *  3. Se inserta en lotes de 500 y se libera la lista inmediatamente, de modo que la
 *     RAM no crece con el tamaño del padrón.
 */
object ExcelImporter {

    private const val HOJA = "BASE"
    private const val LOTE = 500
    private const val BUFFER = 64 * 1024

    // Índices 0-based de las columnas del Excel.
    const val COL_INSTITUCION = 3   // D  - TRABAJA
    const val COL_CODMOD = 4        // E  - CODMOD
    const val COL_PATERNO = 7       // H  - PATERNO
    const val COL_MATERNO = 8       // I  - MATERNO
    const val COL_NOMBRES = 9       // J  - NOMBRES
    const val COL_BRUTO = 45        // AT - BRUTO
    const val COL_MONTO = 47        // AV - MONTO
    const val COL_DESCUENTO = 50    // AY - % DSCTO
    const val COL_ESTADO = 51       // AZ - ESTADO

    data class Resultado(
        val periodo: String,
        val archivo: String,
        val hoja: String,
        val filasLeidas: Int,
        val filasGuardadas: Int,
        val omitidas: Int
    )

    /**
     * Importa el padrón completo.
     *
     * @param alValidarArchivo se invoca cuando el libro ya está abierto y la hoja "BASE"
     *   existe: es el momento seguro para borrar el periodo anterior, porque hasta
     *   entonces un Excel inválido no destruye el padrón que ya está en el teléfono.
     * @param onProgreso se invoca en el hilo de IO con (filas leídas, filas guardadas).
     */
    suspend fun importar(
        context: Context,
        uri: Uri,
        periodo: String,
        alValidarArchivo: suspend () -> Unit,
        onProgreso: (leidas: Int, guardadas: Int) -> Unit,
        guardarLote: suspend (List<TrabajadorEntity>) -> Unit
    ): Resultado = withContext(Dispatchers.IO) {
        val nombreArchivo = nombreDelArchivo(context, uri)
        onProgreso(0, 0)

        conLibro(context, uri) { libro ->
            val hoja = libro.getSheet(HOJA) ?: throw IOException(
                "El archivo no contiene la hoja \"$HOJA\". " +
                    "Hojas disponibles: ${libro.numberOfSheets.let { n ->
                        (0 until n).joinToString(", ") { libro.getSheetName(it) }
                    }}"
            )

            val inicioDatos = detectarInicioDeDatos(hoja)
            alValidarArchivo()
            var leidas = 0
            var guardadas = 0
            var omitidas = 0
            val lote = ArrayList<TrabajadorEntity>(LOTE)

            for (indice in inicioDatos..hoja.lastRowNum) {
                val fila = hoja.getRow(indice) ?: continue
                leidas++

                val filaDatos = leerFila(fila, periodo) ?: run {
                    omitidas++
                    continue
                }
                lote.add(filaDatos)

                if (lote.size >= LOTE) {
                    // Se pasa una copia y se limpia la original de inmediato.
                    guardarLote(ArrayList(lote))
                    guardadas += lote.size
                    lote.clear()
                    onProgreso(leidas, guardadas)
                }
            }

            if (lote.isNotEmpty()) {
                guardarLote(ArrayList(lote))
                guardadas += lote.size
                lote.clear()
            }
            onProgreso(leidas, guardadas)

            Resultado(
                periodo = periodo,
                archivo = nombreArchivo,
                hoja = hoja.sheetName,
                filasLeidas = leidas,
                filasGuardadas = guardadas,
                omitidas = omitidas
            )
        }
    }

    // ------------------------------------------------------------------helpers

    /**
     * Abre el flujo del archivo y garantiza su cierre (el workbook no siempre lo hace,
     * sobre todo con .xlsx), tanto en éxito como en error.
     */
    private suspend fun <T> conLibro(context: Context, uri: Uri, bloque: suspend (Workbook) -> T): T {
        val entrada = context.contentResolver.openInputStream(uri)
            ?: throw IOException("No se pudo abrir el archivo seleccionado.")
        BufferedInputStream(entrada, BUFFER).use { buffer ->
            return abrirLibro(buffer).use { libro -> bloque(libro) }
        }
    }

    /**
     * Abre el libro según su firma binaria: HSSFWorkbook para .xls y XSSFWorkbook para .xlsx.
     * Se inspeccionan los primeros bytes en vez de dejar que POI recorra el archivo dos veces.
     */
    private fun abrirLibro(buffer: BufferedInputStream): Workbook {
        buffer.mark(8)
        val cabecera = ByteArray(8)
        var leidos = 0
        while (leidos < cabecera.size) {
            val n = buffer.read(cabecera, leidos, cabecera.size - leidos)
            if (n <= 0) break
            leidos += n
        }
        buffer.reset()

        val esOoxml = leidos >= 2 &&
            cabecera[0] == 'P'.code.toByte() &&
            cabecera[1] == 'K'.code.toByte()

        return try {
            if (esOoxml) XSSFWorkbook(buffer) else HSSFWorkbook(buffer)
        } catch (e: Throwable) {
            throw IOException(
                "No se pudo interpretar el Excel. Si el archivo está cifrado o fue generado " +
                    "con una versión muy reciente de Office, vuelve a guardarlo como .xls " +
                    "(Excel 97-2003) e inténtalo de nuevo.",
                e
            )
        }
    }

    /** Detecta la fila de encabezado para no importar filas de título ni subtítulos. */
    private fun detectarInicioDeDatos(hoja: Sheet): Int {
        val limite = minOf(hoja.lastRowNum, hoja.firstRowNum + 12)
        var indice = hoja.firstRowNum
        while (indice <= limite) {
            val fila = hoja.getRow(indice)
            if (fila != null) {
                val d = textoDe(fila, COL_INSTITUCION).uppercase()
                val e = textoDe(fila, COL_CODMOD).uppercase()
                if (d.contains("TRABAJA") || d.contains("INSTITUCION") || e.contains("CODMOD")) {
                    return indice + 1
                }
            }
            indice++
        }
        return hoja.firstRowNum + 1
    }

    /** Devuelve null si la fila está vacía (no es un trabajador). */
    private fun leerFila(fila: Row, periodo: String): TrabajadorEntity? {
        val institucion = textoDe(fila, COL_INSTITUCION)
        val codMod = leerCodigo(fila, COL_CODMOD)
        val paterno = textoDe(fila, COL_PATERNO)
        val materno = textoDe(fila, COL_MATERNO)
        val nombres = textoDe(fila, COL_NOMBRES)

        if (paterno.isBlank() && materno.isBlank() && nombres.isBlank() && codMod.isBlank()) return null

        return TrabajadorEntity(
            periodo = periodo,
            institucion = institucion,
            codMod = codMod,
            paterno = paterno,
            materno = materno,
            nombres = nombres,
            bruto = leerMonto(fila, COL_BRUTO),
            monto = leerMonto(fila, COL_MONTO),
            descuento = leerPorcentaje(fila, COL_DESCUENTO),
            estado = EstadoTrabajador.normalizar(textoDe(fila, COL_ESTADO))
        )
    }

    private fun celda(fila: Row, indice: Int): Cell? =
        fila.getCell(indice, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)

    private fun tipoEfectivo(celda: Cell): CellType =
        if (celda.cellType == CellType.FORMULA) celda.cachedFormulaResultType else celda.cellType

    private fun textoDe(fila: Row, indice: Int): String {
        val celda = celda(fila, indice) ?: return ""
        return when (tipoEfectivo(celda)) {
            CellType.STRING -> celda.stringCellValue
            CellType.NUMERIC -> NumberToTextConverter.toText(celda.numericCellValue)
            CellType.BOOLEAN -> if (celda.booleanCellValue) "VERDADERO" else "FALSO"
            else -> ""
        }.replace('\u00A0', ' ').trim()
    }

    /**
     * DNI (8 dígitos) o código modular (10). Se convierte a entero largo cuando la celda
     * es numérica para evitar la notación científica ("1.022E9") y luego se filtran los
     * dígitos por si el origen trae separadores de miles o decimales.
     */
    private fun leerCodigo(fila: Row, indice: Int): String {
        val celda = celda(fila, indice) ?: return ""
        val crudo = when (tipoEfectivo(celda)) {
            CellType.NUMERIC -> {
                val valor = celda.numericCellValue
                if (!valor.isNaN() && !valor.isInfinite() && valor == floor(valor) && abs(valor) < 1.0E15) {
                    valor.toLong().toString()
                } else {
                    NumberToTextConverter.toText(valor)
                }
            }
            CellType.STRING -> celda.stringCellValue
            else -> ""
        }
        return crudo.filter { it.isDigit() }
    }

    private fun leerMonto(fila: Row, indice: Int): Double {
        val celda = celda(fila, indice) ?: return 0.0
        return when (tipoEfectivo(celda)) {
            CellType.NUMERIC -> celda.numericCellValue.takeIf { !it.isNaN() && !it.isInfinite() } ?: 0.0
            CellType.STRING -> parsearNumero(celda.stringCellValue)
            else -> 0.0
        }
    }

    /** Normaliza "11 %", "0.11", "11,0" y celdas con formato de porcentaje a una escala 0..100. */
    private fun leerPorcentaje(fila: Row, indice: Int): Double {
        val celda = celda(fila, indice) ?: return 0.0
        val valor = when (tipoEfectivo(celda)) {
            CellType.NUMERIC -> celda.numericCellValue.takeIf { !it.isNaN() && !it.isInfinite() } ?: 0.0
            CellType.STRING -> parsearNumero(celda.stringCellValue)
            else -> 0.0
        }
        if (valor <= 0.0) return 0.0

        var porcentaje = valor
        if (tieneFormatoPorcentaje(celda)) porcentaje = valor * 100.0
        if (porcentaje in 0.0..1.0) porcentaje = valor * 100.0

        return porcentaje.coerceIn(0.0, 100.0)
    }

    private fun tieneFormatoPorcentaje(celda: Cell): Boolean = try {
        celda.cellStyle?.dataFormatString?.contains('%') == true
    } catch (_: Throwable) {
        false
    }

    /** Acepta "1,234.56", "1.234,56", "1234,56", "(123)" y "1.234" (miles). */
    private fun parsearNumero(texto: String?): Double {
        if (texto.isNullOrBlank()) return 0.0
        var limpio = texto.trim().replace('\u00A0', ' ').replace(" ", "")
        val negativoPorParentesis = limpio.startsWith("(") && limpio.endsWith(")")
        if (negativoPorParentesis) {
            limpio = limpio.substring(1, limpio.length - 1)
        }
        limpio = limpio.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
        if (limpio.isBlank() || limpio == "-") return 0.0

        val decimal = maxOf(limpio.lastIndexOf('.'), limpio.lastIndexOf(','))
        val normalizado = if (decimal < 0) {
            limpio
        } else {
            val entero = limpio.substring(0, decimal).filter { it.isDigit() }
            val fraccion = limpio.substring(decimal + 1).filter { it.isDigit() }
            val separadores = limpio.count { it == '.' || it == ',' }
            when {
                entero.isEmpty() && fraccion.isNotEmpty() -> "0.$fraccion"
                fraccion.isEmpty() -> entero
                separadores == 1 && fraccion.length == 3 -> entero + fraccion
                else -> "$entero.$fraccion"
            }
        }

        val valor = normalizado.toDoubleOrNull() ?: return 0.0
        return if (negativoPorParentesis) -valor else valor
    }

    private fun nombreDelArchivo(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val indice = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (indice >= 0 && cursor.moveToFirst()) cursor.getString(indice) else null
            } ?: uri.lastPathSegment ?: "padron.xlsx"
        } catch (_: Throwable) {
            uri.lastPathSegment ?: "padron.xlsx"
        }
    }

    /**
     * Propone el nombre del periodo a partir del archivo (p. ej.
     * "PADRON DE CONSULTA SETIEMBRE 2026.xls" -> "Setiembre 2026") y, si no encuentra
     * un mes reconocible, cae al mes y año actuales.
     */
    fun sugerirPeriodo(nombreArchivo: String, hoy: LocalDate = LocalDate.now()): String {
        val meses = listOf(
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SETIEMBRE", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
        )
        val texto = nombreArchivo.uppercase(Locale.ROOT)
        val anio = Regex("(20\\d{2})").find(texto)?.groupValues?.get(1)
        val mesIdx = meses.indexOfFirst { texto.contains(it) }
        if (mesIdx >= 0 && anio != null) {
            val nombre = meses[mesIdx].lowercase(Locale.ROOT)
                .replaceFirstChar { it.uppercase() }
                .replace("Septiembre", "Setiembre")
            return "$nombre $anio"
        }
        if (anio != null) {
            return "${hoy.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))} $anio"
                .replaceFirstChar { it.uppercase() }
        }
        val nombreMes = hoy.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
        return "${nombreMes.replaceFirstChar { it.uppercase() }} ${hoy.year}"
    }
}