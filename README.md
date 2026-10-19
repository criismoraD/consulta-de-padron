# Consulta de Padrón

App Android **100 % offline** para que un asesor comercial importe el padrón mensual de
trabajadores del estado (`.xls` / `.xlsx`), lo convierta en una base de datos interna en el
teléfono y consulte en milisegundos si un trabajador es apto para el crédito por convenio.

---

## 1. Arquitectura

```
MainActivity (UI Compose premium)
      │
PadronViewModel (StateFlow + debounce)
      │
PadronRepository  ──►  PadronDataSource (interfaz cloud-ready)
      │                        │
      │                        └── PadronLocalDataSource  ──►  TrabajadorDao (Room)
      └── importarExcel ──────►  ExcelImporter (Apache POI, solo hoja "BASE")
```

* **Room / SQLite** en almacenamiento interno privado (`padron_interno.db`, journal WAL).
* **Cero permisos**: la app no pide internet ni almacenamiento; el Excel se elige con el
  selector de archivos del sistema (SAF) y se lee por `content://`.
* **Cloud-ready**: `PadronDataSource` es un contrato. Hoy solo existe la fuente local; para
  sincronizar con una nube basta añadir una fuente remota y componerla en `PadronRepository`
  **sin tocar el ViewModel ni la UI**.

## 2. Estructura de archivos

| Archivo | Responsabilidad |
|---|---|
| `app/build.gradle.kts` | Room + KSP, POI, desugaring, `minSdk 26`, `largeHeap` |
| `app/src/main/AndroidManifest.xml` | Sin permisos, `android:largeHeap="true"` |
| `data/local/TrabajadorEntity.kt` | Tabla `trabajador` + sus 8 índices |
| `data/local/PeriodoEntity.kt` | Tabla `periodos` (multi-periodo) |
| `data/local/TrabajadorDao.kt` | KPIs, instituciones, búsqueda con `LIMIT 100`, lotes |
| `data/local/AppDatabase.kt` | Construcción de la base |
| `data/ExcelImporter.kt` | Lector POI de bajo consumo de memoria |
| `data/EstadoTrabajador.kt` | Normalización de la columna AZ a 3 estados |
| `data/PadronRepository.kt` | Fachada + flujo de importación |
| `ui/PadronViewModel.kt` | Estado, filtros, progreso de importación |
| `MainActivity.kt` | Toda la UI: cabecera, KPIs, buscador, tarjetas, diálogos |
| `ui/theme/*` | Paleta ejecutiva fintech |

## 3. Procesamiento del Excel (el punto crítico)

`ExcelImporter` está diseñado alrededor del archivo real de **16 MB / 10 027 filas / 8 hojas /
56 columnas**:

1. Lee **solo la hoja `BASE`**; las demás (`Hoja1`, `Hoja3`…`Hoja6`, `DINAMICA`) ni se abren.
2. Accede **por índice** a las 9 columnas necesarias, sin recorrer la fila completa.
3. Inserta en **lotes de 500** y ejecuta `lote.clear()` en cada.flush: la RAM no crece con el
   tamaño del padrón.
4. Corre en `Dispatchers.IO` con `BufferedInputStream` de 64 KB.
5. **Borra el periodo anterior solo cuando el archivo ya es válido** (hoja `BASE` abierta): un
   Excel corrupto nunca destruye el padrón que ya está en el teléfono.
6. Detecta la fila de encabezado automáticamente (no asume que sea la fila 0).

### Formato real de las columnas (verificado con `ExcelProbeTest`)

| Col | Índice | Contenido | Tipo real en el archivo |
|---|---|---|---|
| D | 3 | `TRABAJA` (institución) | Texto — 418 instituciones distintas |
| E | 4 | `CODMOD` (DNI / código modular) | **Texto** de 8–10 dígitos |
| H/I/J | 7/8/9 | `PATERNO`, `MATERNO`, `NOMBRES` | Texto |
| AT | 45 | `BRUTO` | Numérico (`1418.00`) |
| AV | 47 | `MONTO` | Numérico (`1256.77`) |
| AY | 50 | `% DSCTO` | **Fórmula** `=AU/AT` → fracción `0.0`–`1.0` (promedio 0.275) |
| AZ | 51 | `ESTADO` | **Fórmula** `=IF(AY>0.5,"🚨 ALERTA",IF(AY>=0.4,"⚠ EVALUAR","✔ PERMITIDO"))` |

Distribución real del padrón (primeras 5 000 filas):
`✔ PERMITIDO` 3 161 · `⚠ EVALUAR` 737 · `🚨 ALERTA` 714 · `SIN INFORMACION` 388.

Implicaciones ya contempladas en el código:

* **Celdas de fórmula**: AY y AZ son fórmulas. El importador lee el **valor en caché** del
  libro (`cachedFormulaResultType`), no la fórmula.
* **AY viene como fracción**: `0.44` se normaliza a `44 %`. Los umbrales de color de la UI
  (verde < 40 %, ámbar 40–50 %, rojo > 50 %) son los mismos del Excel.
* **388 filas sin información**: su fórmula da error (`#DIV/0!`, AT vacío). Se importan con
  descuento `0 %` y estado `EVALUAR` — nunca "apto" por defecto.
* **AZ trae emojis** (`✔`, `⚠`, `🚨`) que se limpian al normalizar.
* **CODMOD es texto**, no número: se filtra a dígitos para descartar cualquier separador de
  miles o notación científica (`1.022E9`).

### Revalidar los índices si el MINEDU cambia el archivo

`app/src/test/java/com/consultadepadron/ExcelProbeTest.kt` lee el padrón real del proyecto e
imprime hojas, encabezados y distribución de estados. Se omite solo si el archivo no está en
la raíz del proyecto:

```powershell
.\gradlew.bat :app:testDebugUnitTest
# salida en app/build/test-results/testDebugUnitTest/TEST-*.xml
```

## 4. Consulta y rendimiento

* Todos los KPIs se resuelven con `SELECT COUNT/SUM … CASE` — nunca cargando filas a memoria.
* El buscador es una sola consulta con los tres filtros combinados y `LIMIT 100`, con debounce
  de 250 ms, para que el `LazyColumn` vaya a 60 FPS.
* Índices en `periodo`, `codMod`, `institucion`, `paterno`, `estado` y las compuestas
  `periodo+estado`, `periodo+institucion`, `periodo+paterno`.

## 5. Compilar

```powershell
.\gradlew.bat :app:assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat :app:assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

Requiere JDK 17+ y `compileSdk 37`.

> **Memoria de compilación**: la variante `release` ejecuta R8 sobre Apache POI + xmlbeans en
> modo completo y necesita un daemon de Gradle grande. `gradle.properties` de este proyecto
> fija `org.gradle.jvmargs=-Xmx12288m` y `android.r8.maxWorkers=1`. Con menos RAM el build de
> `release` falla con `java.lang.OutOfMemoryError: Java heap space` durante R8; la variante
> `debug` compila sin problema con 4 GB.

### Notas de la toolchain (ya resueltas en el proyecto)

* **KSP + Kotlin integrado de AGP 9**: el plugin KSP registra sus fuentes con
  `kotlin.sourceSets`, que AGP 9 bloquea por defecto. `gradle.properties` incluye
  `android.disallowKotlinSourceSets=false`, que es la vía soportada hasta que KSP migre a
  `android.sourceSets`.
* **Core library desugaring** activado (`desugar_jdk_libs 2.1.5`): Apache POI usa
  `java.time` / `java.nio.file`, que no existen en API 26.
* **`poi-ooxml` en Android**: la ruta verificada y probada es **HSSF (`.xls`)**, que es el
  formato del padrón real y no depende de StAX. `poi-ooxml` (`.xlsx`) se incluye y el
  importador detecta el formato por los bytes iniciales (`PK` → XSSF, `D0CF11E0` → HSSF), pero
  su soporte en Android depende de las APIs XML disponibles en el dispositivo; si un `.xlsx`
  falla, el importador muestra un mensaje claro y sugiere guardar el archivo como `.xls`.
* La `release` se firma con la clave de depuración para poder instalarla directamente; cambia
  `signingConfig` por el tuyo antes de distribuir.

## 6. Uso en 3 pasos

1. **Subir Padrón Excel** → se elige el archivo y se confirma el periodo (se sugiere a partir
   del nombre, p. ej. *PADRON DE CONSULTA SETIEMBRE 2026.xls* → **Setiembre 2026**).
2. Se importa en segundo plano con contador en vivo; se inserta por lotes de 500.
3. Se consulta por DNI, código modular, apellido o nombre, con filtros de institución y de
   estado. El código modular se copia al portapapeles con un toque.

El gestor de padrones (icono de almacenamiento en la cabecera) permite eliminar periodos
antiguos y liberar espacio interno.