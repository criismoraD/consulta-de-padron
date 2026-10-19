package com.consultadepadron

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.consultadepadron.data.EstadoTrabajador
import com.consultadepadron.data.Kpis
import com.consultadepadron.data.local.InstitucionDto
import com.consultadepadron.data.local.PeriodoEntity
import com.consultadepadron.data.local.TrabajadorEntity
import com.consultadepadron.data.local.nombreCompleto
import com.consultadepadron.ui.PadronUiState
import com.consultadepadron.ui.PadronViewModel
import com.consultadepadron.ui.ProgresoImportacion
import com.consultadepadron.ui.theme.Ambar
import com.consultadepadron.ui.theme.AmbarFondo
import com.consultadepadron.ui.theme.Azul
import com.consultadepadron.ui.theme.AzulNoche
import com.consultadepadron.ui.theme.AzulPizarra
import com.consultadepadron.ui.theme.Blanco
import com.consultadepadron.ui.theme.BordeSuave
import com.consultadepadron.ui.theme.ConsultaPadronTheme
import com.consultadepadron.ui.theme.DoradoBorde
import com.consultadepadron.ui.theme.DoradoFondo
import com.consultadepadron.ui.theme.Esmeralda
import com.consultadepadron.ui.theme.EsmeraldaFondo
import com.consultadepadron.ui.theme.Hielo
import com.consultadepadron.ui.theme.RojoCoral
import com.consultadepadron.ui.theme.RojoFondo
import com.consultadepadron.ui.theme.Slate300
import com.consultadepadron.ui.theme.Slate400
import com.consultadepadron.ui.theme.Slate500
import com.consultadepadron.ui.theme.Slate700
import com.consultadepadron.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val MIME_EXCEL = arrayOf(
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/octet-stream"
)

class MainActivity : ComponentActivity() {

    private val viewModel: PadronViewModel by viewModels { PadronViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConsultaPadronTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Hielo) {
                    ConsultaPadronPantalla(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsultaPadronPantalla(viewModel: PadronViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

    var archivoPendiente by remember { mutableStateOf<Uri?>(null) }
    var nombreArchivo by remember { mutableStateOf("") }
    var periodoSugerido by remember { mutableStateOf("") }
    var mostrarDialogoPeriodo by remember { mutableStateOf(false) }
    var mostrarInstituciones by remember { mutableStateOf(false) }
    var mostrarGestor by remember { mutableStateOf(false) }
    var periodoAEliminar by remember { mutableStateOf<String?>(null) }
    var buscadorEnfocado by remember { mutableStateOf(false) }

    // Con el teclado abierto la cabecera y los KPIs se compactan para dejar ver resultados.
    val modoBusqueda = buscadorEnfocado || estado.filtro.query.isNotBlank()

    val selectorExcel = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            nombreArchivo = resolverNombreArchivo(contexto, uri)
            periodoSugerido = viewModel.sugerirPeriodo(nombreArchivo)
            archivoPendiente = uri
            mostrarDialogoPeriodo = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.mensajes.collect { snackbarHost.showSnackbar(it) }
    }

    fun lanzarSelector() = selectorExcel.launch(MIME_EXCEL)

    Scaffold(
        containerColor = Hielo,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            CabeceraApp(
                estado = estado,
                compacta = modoBusqueda,
                onPeriodoSeleccionado = viewModel::seleccionarPeriodo,
                onImportar = { lanzarSelector() },
                onGestionar = { mostrarGestor = true }
            )

            if (!estado.hayPadrones) {
                PantallaBienvenida(
                    modifier = Modifier.weight(1f),
                    onSubir = { lanzarSelector() }
                )
            } else {
                Buscador(
                    query = estado.filtro.query,
                    onQuery = viewModel::buscar,
                    onFocusChanged = { buscadorEnfocado = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = if (modoBusqueda) 8.dp else 12.dp,
                            bottom = 8.dp
                        )
                )

                ContenidoPadron(
                    estado = estado,
                    modoBusqueda = modoBusqueda,
                    onEstadoSeleccionado = viewModel::filtrarPorEstado,
                    onInstitucionSeleccionada = viewModel::filtrarPorInstitucion,
                    onAbrirInstituciones = { mostrarInstituciones = true },
                    onCopiar = { valor ->
                        copiarAlPortapapeles(contexto, valor)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // ------------------------------------------------------------- diálogos

    if (mostrarDialogoPeriodo && archivoPendiente != null) {
        DialogoPeriodo(
            archivo = nombreArchivo,
            periodoSugerido = periodoSugerido,
            periodosExistentes = estado.periodos.map { it.nombre },
            onConfirmar = { periodo ->
                val uri = archivoPendiente
                archivoPendiente = null
                mostrarDialogoPeriodo = false
                if (uri != null && periodo.isNotBlank()) viewModel.importar(uri, periodo)
            },
            onDismiss = {
                archivoPendiente = null
                mostrarDialogoPeriodo = false
            }
        )
    }

    estado.importacion?.let { progreso ->
        DialogoImportacion(progreso = progreso, archivo = nombreArchivo)
    }

    if (mostrarInstituciones) {
        ModalInstituciones(
            instituciones = estado.instituciones,
            seleccionada = estado.filtro.institucion,
            onSeleccionar = { viewModel.filtrarPorInstitucion(it) },
            onDismiss = { mostrarInstituciones = false }
        )
    }

    if (mostrarGestor) {
        ModalGestorPadrones(
            periodos = estado.periodos,
            onEliminar = { periodoAEliminar = it },
            onImportar = {
                mostrarGestor = false
                lanzarSelector()
            },
            onDismiss = { mostrarGestor = false }
        )
    }

    periodoAEliminar?.let { periodo ->
        AlertDialog(
            onDismissRequest = { periodoAEliminar = null },
            title = { Text("Eliminar padrón") },
            text = {
                Text(
                    "Se borrarán los ${
                        estado.periodos.firstOrNull { it.nombre == periodo }?.totalRegistros ?: 0
                    } registros de \"$periodo\" del almacenamiento interno. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarPeriodo(periodo)
                        periodoAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoCoral)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { periodoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

// ------------------------------------------------------------------ cabecera

@Composable
private fun CabeceraApp(
    estado: PadronUiState,
    compacta: Boolean,
    onPeriodoSeleccionado: (String) -> Unit,
    onImportar: () -> Unit,
    onGestionar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(AzulNoche, AzulPizarra)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = 20.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = if (compacta) 10.dp else 16.dp
                )
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CONSULTA DE PADRÓN",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onImportar) {
                    Icon(Icons.Rounded.UploadFile, "Importar padrón", tint = Blanco)
                }
                IconButton(onClick = onGestionar) {
                    Icon(Icons.Rounded.Storage, "Gestionar padrones", tint = Blanco)
                }
            }

            SelectorPeriodo(
                estado = estado,
                compacta = compacta,
                onPeriodoSeleccionado = onPeriodoSeleccionado
            )

            AnimatedVisibility(visible = !compacta) {
                Text(
                    text = buildString {
                        append("${formatearEntero(estado.kpis.total)} registros")
                        append("  ·  ${estado.periodos.size} ")
                        append(if (estado.periodos.size == 1) "periodo" else "periodos")
                        append("  ·  base local")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    modifier = Modifier.padding(top = 10.dp, start = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorPeriodo(
    estado: PadronUiState,
    compacta: Boolean,
    onPeriodoSeleccionado: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val foco = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { abierto ->
            expandido = abierto
            if (abierto) foco.clearFocus()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, Slate700),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = if (compacta) 8.dp else 10.dp,
                    bottom = if (compacta) 8.dp else 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        null,
                        tint = Slate300,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (!compacta) {
                        Text(
                            text = "PERIODO ACTIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Text(
                        text = estado.periodoSeleccionado ?: "Sin periodo cargado",
                        style = if (compacta) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        color = Blanco,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Rounded.ExpandMore,
                    "Cambiar periodo",
                    tint = Slate300,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            if (estado.periodos.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Sin periodos cargados", color = Slate500) },
                    onClick = {},
                    enabled = false
                )
            }
            estado.periodos.forEach { periodo ->
                DropdownItemPeriodo(
                    nombre = periodo.nombre,
                    detalle = "${formatearEntero(periodo.totalRegistros)} registros · ${formatearFecha(periodo.fechaCarga)}",
                    seleccionado = periodo.nombre == estado.periodoSeleccionado,
                    onClick = {
                        onPeriodoSeleccionado(periodo.nombre)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DropdownItemPeriodo(
    nombre: String,
    detalle: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (seleccionado) Azul else Slate900
                )
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        },
        onClick = onClick
    )
}

// ------------------------------------------------------------------ buscador

@Composable
private fun Buscador(
    query: String,
    onQuery: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val foco = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier.onFocusChanged { onFocusChanged(it.isFocused) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        placeholder = { Text("DNI, código modular, apellido o nombre") },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Slate500) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onQuery("")
                        foco.clearFocus()
                    }
                ) {
                    Icon(Icons.Rounded.Close, "Limpiar búsqueda", tint = Slate500)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() })
    )
}

// ------------------------------------------------------------------ contenido

@Composable
private fun ContenidoPadron(
    estado: PadronUiState,
    modoBusqueda: Boolean,
    onEstadoSeleccionado: (String?) -> Unit,
    onInstitucionSeleccionada: (String?) -> Unit,
    onAbrirInstituciones: () -> Unit,
    onCopiar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listaState = rememberLazyListState()
    var resumenExpandido by rememberSaveable { mutableStateOf(true) }
    var resumenColapsadoPorBusqueda by remember { mutableStateOf(false) }

    // Al entrar en modo búsqueda el resumen se pliega para ganar altura; al salir vuelve
    // a abrirse solo si fue este cambio automático el que lo cerró.
    LaunchedEffect(modoBusqueda) {
        if (modoBusqueda && resumenExpandido) {
            resumenExpandido = false
            resumenColapsadoPorBusqueda = true
        } else if (!modoBusqueda && resumenColapsadoPorBusqueda) {
            resumenExpandido = true
            resumenColapsadoPorBusqueda = false
        }
    }

    // Mientras se escribe, los resultados suben al primer plano del listado: la cabecera
    // y los filtros quedan por encima, fuera del área que ocupa el teclado.
    LaunchedEffect(estado.filtro.query) {
        if (estado.filtro.query.isNotBlank()) {
            listaState.animateScrollToItem(2)
        }
    }

    LazyColumn(
        state = listaState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "resumen") {
            ModuloResumen(
                estado = estado,
                expandido = resumenExpandido,
                onToggle = {
                    resumenExpandido = !resumenExpandido
                    resumenColapsadoPorBusqueda = false
                }
            )
        }

        item(key = "filtros") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SelectorInstitucion(
                    seleccionada = estado.filtro.institucion,
                    onClick = onAbrirInstituciones,
                    onLimpiar = { onInstitucionSeleccionada(null) }
                )
                Spacer(Modifier.height(10.dp))
                ChipsEstado(
                    estadoSeleccionado = estado.filtro.estado,
                    kpis = estado.kpis,
                    onEstadoSeleccionado = onEstadoSeleccionado
                )
            }
        }

        item(key = "cabecera-resultados") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESULTADOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${formatearEntero(estado.resultadosVisibles)}" +
                        if (estado.limiteAlcanzado) " de ${PadronUiState.LIMITE_BUSQUEDA}+" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate500
                )
            }
        }

        if (estado.resultados.isEmpty()) {
            item(key = "sin-resultados") {
                SinResultados(
                    hayConsulta = estado.filtro.query.isNotBlank() ||
                        estado.filtro.estado != null ||
                        estado.filtro.institucion != null,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(estado.resultados, key = { it.id }) { trabajador ->
                TarjetaTrabajador(
                    trabajador = trabajador,
                    onCopiar = { onCopiar(trabajador.codMod) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------ KPIs

/**
 * Módulo plegable con el resumen del padrón. Se contrae automáticamente cuando el
 * buscador está activo (teclado abierto) y puede reabrirse con un toque.
 */
@Composable
private fun ModuloResumen(
    estado: PadronUiState,
    expandido: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RESUMEN DEL PADRÓN",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    letterSpacing = 1.5.sp
                )
                if (!expandido) {
                    Text(
                        text = "${formatearEntero(estado.kpis.total)} registros" +
                            "  ·  ${estado.kpis.porcentajePermitidos} % aptos",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = if (expandido) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expandido) "Ocultar resumen" else "Mostrar resumen",
                tint = Slate500,
                modifier = Modifier.size(22.dp)
            )
        }

        AnimatedVisibility(visible = expandido) {
            KpiGrid(estado = estado, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun KpiGrid(estado: PadronUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TarjetaKpi(
                titulo = "Total padrón",
                valor = estado.kpis.total,
                porcentaje = null,
                icono = Icons.Rounded.Groups,
                color = Azul,
                fondo = Color(0xFFDBEAFE),
                modifier = Modifier.weight(1f)
            )
            TarjetaKpi(
                titulo = "Aptos",
                valor = estado.kpis.permitidos,
                porcentaje = estado.kpis.porcentajePermitidos,
                icono = Icons.Rounded.VerifiedUser,
                color = Esmeralda,
                fondo = EsmeraldaFondo,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TarjetaKpi(
                titulo = "Por evaluar",
                valor = estado.kpis.evaluar,
                porcentaje = estado.kpis.porcentajeEvaluar,
                icono = Icons.Rounded.Warning,
                color = Ambar,
                fondo = AmbarFondo,
                modifier = Modifier.weight(1f)
            )
            TarjetaKpi(
                titulo = "No aptos",
                valor = estado.kpis.alerta,
                porcentaje = estado.kpis.porcentajeAlerta,
                icono = Icons.Rounded.Error,
                color = RojoCoral,
                fondo = RojoFondo,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TarjetaKpi(
    titulo: String,
    valor: Int,
    porcentaje: Int?,
    icono: ImageVector,
    color: Color,
    fondo: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Blanco),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(fondo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatearEntero(valor),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Slate900
                )
                if (porcentaje != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$porcentaje %",
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ filtros

@Composable
private fun SelectorInstitucion(
    seleccionada: String?,
    onClick: () -> Unit,
    onLimpiar: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Blanco,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Apartment, null, tint = Azul, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = seleccionada ?: "Todas las instituciones",
                style = MaterialTheme.typography.bodyMedium,
                color = if (seleccionada != null) Slate900 else Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (seleccionada != null) {
                IconButton(onClick = onLimpiar, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Close, "Quitar filtro", tint = Slate500, modifier = Modifier.size(16.dp))
                }
            }
            Icon(Icons.Rounded.ExpandMore, null, tint = Slate500, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ChipsEstado(
    estadoSeleccionado: String?,
    kpis: Kpis,
    onEstadoSeleccionado: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipEstado(
            etiqueta = "Todos",
            conteo = kpis.total,
            icono = Icons.Rounded.Groups,
            color = Slate700,
            fondo = Color(0xFFEDF2F7),
            seleccionado = estadoSeleccionado == null,
            onClick = { onEstadoSeleccionado(null) }
        )
        ChipEstado(
            etiqueta = "Permitido",
            conteo = kpis.permitidos,
            icono = Icons.Rounded.CheckCircle,
            color = Esmeralda,
            fondo = EsmeraldaFondo,
            seleccionado = estadoSeleccionado == EstadoTrabajador.PERMITIDO,
            onClick = { onEstadoSeleccionado(EstadoTrabajador.PERMITIDO) }
        )
        ChipEstado(
            etiqueta = "Evaluar",
            conteo = kpis.evaluar,
            icono = Icons.Rounded.Warning,
            color = Ambar,
            fondo = AmbarFondo,
            seleccionado = estadoSeleccionado == EstadoTrabajador.EVALUAR,
            onClick = { onEstadoSeleccionado(EstadoTrabajador.EVALUAR) }
        )
        ChipEstado(
            etiqueta = "Alerta",
            conteo = kpis.alerta,
            icono = Icons.Rounded.Error,
            color = RojoCoral,
            fondo = RojoFondo,
            seleccionado = estadoSeleccionado == EstadoTrabajador.ALERTA,
            onClick = { onEstadoSeleccionado(EstadoTrabajador.ALERTA) }
        )
    }
}

@Composable
private fun ChipEstado(
    etiqueta: String,
    conteo: Int,
    icono: ImageVector,
    color: Color,
    fondo: Color,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(icono, null, modifier = Modifier.size(16.dp)) },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(etiqueta, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatearEntero(conteo),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Blanco,
            labelColor = Slate700,
            iconColor = Slate500,
            selectedContainerColor = fondo,
            selectedLabelColor = color,
            selectedLeadingIconColor = color
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = seleccionado,
            borderColor = BordeSuave,
            selectedBorderColor = color
        )
    )
}

// ------------------------------------------------------------------ tarjeta del trabajador

@Composable
private fun TarjetaTrabajador(
    trabajador: TrabajadorEntity,
    onCopiar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorEstado = colorDeEstado(trabajador.estado)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Blanco),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Franja lateral de 6.dp con el color del estado.
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .background(colorEstado)
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fila superior: nombre + badge de estado
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = trabajador.nombreCompleto.ifBlank { "Sin nombres" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    BadgeEstado(trabajador.estado)
                }

                // Subtítulo: código copiable + institución
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChipCodigo(trabajador.codMod, onCopiar)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.Apartment,
                        null,
                        tint = Slate500,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = trabajador.institucion.ifBlank { "Sin institución" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bloque financiero: 3 tarjetas internas
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CeldaFinanciera(
                        etiqueta = "SUELDO BRUTO",
                        valor = formatearMoneda(trabajador.bruto),
                        modifier = Modifier.weight(1f)
                    )
                    CeldaMontoDisponible(
                        monto = trabajador.monto,
                        modifier = Modifier.weight(1f)
                    )
                    CeldaDescuento(
                        descuento = trabajador.descuento,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeEstado(estado: String) {
    val fondo = fondoDeEstado(estado)
    val color = colorDeEstado(estado)
    val (icono, etiqueta) = when (estado) {
        EstadoTrabajador.PERMITIDO -> Icons.Rounded.CheckCircle to "APTO"
        EstadoTrabajador.EVALUAR -> Icons.Rounded.Warning to "EVALUAR"
        else -> Icons.Rounded.Block to "NO APTO"
    }

    Surface(color = fondo, contentColor = color, shape = RoundedCornerShape(50)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, null, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChipCodigo(codigo: String, onClick: () -> Unit) {
    if (codigo.isBlank()) return
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ContentCopy, null, tint = Azul, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = codigo,
                style = MaterialTheme.typography.labelMedium,
                color = Azul
            )
        }
    }
}

@Composable
private fun CeldaFinanciera(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                fontSize = 9.sp,
                maxLines = 1
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleSmall,
                color = Slate900,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Dato clave del asesor: monto líquido disponible, destacado en dorado. */
@Composable
private fun CeldaMontoDisponible(monto: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DoradoFondo,
        border = BorderStroke(1.dp, DoradoBorde)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "DISPONIBLE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF92700A),
                fontSize = 9.sp,
                maxLines = 1
            )
            Text(
                text = formatearMoneda(monto),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF713F12),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** % de descuento con barra de progreso: verde < 40 %, ámbar 40-50 %, rojo > 50 %. */
@Composable
private fun CeldaDescuento(descuento: Double, modifier: Modifier = Modifier) {
    val fraccion = (descuento / 100.0).coerceIn(0.0, 1.0).toFloat()
    val color = when {
        descuento >= 50.0 -> RojoCoral
        descuento >= 40.0 -> Ambar
        else -> Esmeralda
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "DESCUENTO",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                fontSize = 9.sp,
                maxLines = 1
            )
            Text(
                text = "${descuento.roundToInt()} %",
                style = MaterialTheme.typography.titleSmall,
                color = Slate900,
                fontSize = 13.sp,
                maxLines = 1
            )
            LinearProgressIndicator(
                progress = { fraccion },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = Color(0xFFE2E8F0),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

// ------------------------------------------------------------------ estados vacíos

@Composable
private fun SinResultados(hayConsulta: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Blanco,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.Search,
                null,
                tint = Slate300,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = if (hayConsulta) "Sin coincidencias" else "Sin trabajadores",
                style = MaterialTheme.typography.titleSmall,
                color = Slate700
            )
            Text(
                text = if (hayConsulta) {
                    "Prueba con el DNI completo, otra institución o quita los filtros de estado."
                } else {
                    "Este periodo no tiene registros."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PantallaBienvenida(modifier: Modifier = Modifier, onSubir: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(AzulNoche, AzulPizarra))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.InsertDriveFile,
                null,
                tint = Blanco,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Consulta de Padrón",
            style = MaterialTheme.typography.headlineSmall,
            color = Slate900
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Importa el padrón mensual del Excel y consulta en segundos si un trabajador es apto para el crédito por convenio.",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Blanco,
            border = BorderStroke(1.dp, BordeSuave),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Indicacion("1.", "Elige el archivo .xls o .xlsx del mes.")
                Indicacion("2.", "Confirma el nombre del periodo (ej. Setiembre 2026).")
                Indicacion("3.", "El padrón queda guardado en el teléfono: sin internet.")
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onSubir,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AzulNoche),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Rounded.UploadFile, null)
            Spacer(Modifier.width(10.dp))
            Text("Subir Primer Padrón Excel (.xls)", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Indicacion(numero: String, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = numero,
            style = MaterialTheme.typography.labelMedium,
            color = Azul,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = Slate700
        )
    }
}

// ------------------------------------------------------------------ diálogos y hojas

@Composable
private fun DialogoPeriodo(
    archivo: String,
    periodoSugerido: String,
    periodosExistentes: List<String>,
    onConfirmar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var texto by remember(periodoSugerido) { mutableStateOf(periodoSugerido) }
    val repetido = periodosExistentes.any { it.equals(texto.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.CalendarMonth, null, tint = Azul) },
        title = { Text("Periodo del padrón") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = archivo,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    singleLine = true,
                    label = { Text("Nombre del periodo") },
                    placeholder = { Text("Setiembre 2026") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (repetido) {
                    Text(
                        text = "Este periodo ya existe: se reemplazará el padrón guardado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ambar
                    )
                }
                val reutilizables = periodosExistentes.filterNot { it.equals(texto.trim(), true) }
                if (reutilizables.isNotEmpty()) {
                    Text(
                        text = "PERIODOS GUARDADOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        letterSpacing = 1.sp
                    )
                    reutilizables.take(3).forEach { existente ->
                        Surface(
                            onClick = { texto = existente },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = existente,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(texto.trim()) },
                enabled = texto.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AzulNoche)
            ) { Text("Importar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogoImportacion(
    progreso: ProgresoImportacion,
    archivo: String
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Blanco,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularProgressIndicator(color = Azul, strokeWidth = 3.dp)

                Text(
                    text = "Importando padrón",
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = progreso.periodo,
                        style = MaterialTheme.typography.labelMedium,
                        color = Azul,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = Azul,
                    trackColor = Color(0xFFE2E8F0)
                )

                Text(
                    text = "${formatearEntero(progreso.guardadas)} registros guardados",
                    style = MaterialTheme.typography.titleSmall,
                    color = Esmeralda
                )
                Text(
                    text = "${formatearEntero(progreso.leidas)} filas leídas del archivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )

                HorizontalDivider(color = BordeSuave)

                Text(
                    text = if (archivo.isBlank()) "Procesando hoja BASE…" else archivo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Puedes seguir usando el teléfono: la importación ocurre en segundo plano.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalInstituciones(
    instituciones: List<InstitucionDto>,
    seleccionada: String?,
    onSeleccionar: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val filtradas = remember(instituciones, busqueda) {
        if (busqueda.isBlank()) instituciones
        else instituciones.filter { it.nombre.contains(busqueda.trim(), ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Hielo
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text("Instituciones", style = MaterialTheme.typography.titleLarge, color = Slate900)
            Text(
                text = "${instituciones.size} en ${"este periodo"}",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Buscar institución") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                item(key = "todas") {
                    ListItem(
                        headlineContent = { Text("Todas las instituciones") },
                        trailingContent = {
                            if (seleccionada == null) {
                                Icon(Icons.Rounded.Check, null, tint = Azul)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSeleccionar(null) }
                    )
                }
                items(filtradas, key = { it.nombre }) { institucion ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = institucion.nombre,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "${formatearEntero(institucion.total)} trabajadores",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500
                            )
                        },
                        trailingContent = {
                            if (seleccionada == institucion.nombre) {
                                Icon(Icons.Rounded.Check, null, tint = Azul)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSeleccionar(institucion.nombre) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalGestorPadrones(
    periodos: List<PeriodoEntity>,
    onEliminar: (String) -> Unit,
    onImportar: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Hielo
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text("Padrones guardados", style = MaterialTheme.typography.titleLarge, color = Slate900)
            Text(
                text = "Cada periodo ocupa espacio en el teléfono. Borra los antiguos cuando ya no los necesites.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(periodos, key = { it.id }) { periodo ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Blanco,
                        border = BorderStroke(1.dp, BordeSuave),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, null, tint = Azul)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = periodo.nombre,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Slate900
                                )
                                Text(
                                    text = "${formatearEntero(periodo.totalRegistros)} registros · ${formatearFecha(periodo.fechaCarga)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onEliminar(periodo.nombre) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    "Eliminar ${periodo.nombre}",
                                    tint = RojoCoral
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onImportar,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.UploadFile, null)
                Spacer(Modifier.width(8.dp))
                Text("Importar otro padrón")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ------------------------------------------------------------------ utilidades

private fun copiarAlPortapapeles(contexto: Context, valor: String) {
    val servicio = contexto.getSystemService(Context.CLIPBOARD_SERVICE)
    (servicio as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText("codMod", valor))
}

private fun resolverNombreArchivo(contexto: Context, uri: Uri): String = try {
    contexto.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (indice >= 0 && cursor.moveToFirst()) cursor.getString(indice) else null
    } ?: uri.lastPathSegment ?: "padron.xlsx"
} catch (_: Exception) {
    uri.lastPathSegment ?: "padron.xlsx"
}

/** S/ 1,234.56 sin depender del locale del dispositivo. */
private fun formatearMoneda(valor: Double): String {
    val negativo = valor < 0
    val absoluto = abs(valor)
    var unidades = absoluto.toLong()
    var centimos = ((absoluto - unidades) * 100).roundToInt()
    if (centimos >= 100) {
        unidades += 1
        centimos -= 100
    }
    val miles = unidades.toString().reversed().chunked(3).joinToString(",").reversed()
    return (if (negativo) "-" else "") + "S/ $miles.${centimos.toString().padStart(2, '0')}"
}

private fun formatearEntero(valor: Int): String =
    valor.toString().reversed().chunked(3).joinToString(",").reversed()

private fun formatearFecha(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-PE")).format(Date(millis))

private fun colorDeEstado(estado: String): Color = when (estado) {
    EstadoTrabajador.PERMITIDO -> Esmeralda
    EstadoTrabajador.EVALUAR -> Ambar
    else -> RojoCoral
}

private fun fondoDeEstado(estado: String): Color = when (estado) {
    EstadoTrabajador.PERMITIDO -> EsmeraldaFondo
    EstadoTrabajador.EVALUAR -> AmbarFondo
    else -> RojoFondo
}