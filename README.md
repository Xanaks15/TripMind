# TripMind

Aplicación Android para analizar ofertas y viajes de Uber Driver. Kotlin, Jetpack
Compose y persistencia local con Room. Especificación: [objetivo del producto](docs/objetivo-del-producto.txt).

Dispositivo objetivo: **4 GB de RAM y 64 GB de almacenamiento**. Consultas
paginadas, índices y optimización release desde el primer bloque. Véase el
[plan de rendimiento y validación](docs/rendimiento.md).

## Bloques implementados: tickets 00–09

Se incluyen los modelos Offer, Trip, DriverSession y VehicleProfile, sus tablas
Room y repositorios con crear, consultar, observar, actualizar y eliminar.

La app permite seleccionar varias capturas del historial, reconocer texto
localmente con ML Kit, convertirlo en candidatos de viaje, revisar y corregir los
campos antes de guardarlos, y consultar los viajes en el historial local. El OCR
no necesita enviar las imágenes a un servidor.

El motor de costos calcula combustible, mantenimiento, depreciación, costo total
y neto estimado con precisión decimal. La pestaña Costos permite probarlo con
valores editables. La captura en vivo, el análisis completo y el overlay
corresponden a bloques posteriores.

## Compilación y pruebas

Requisitos: JDK 17 y Android SDK 35. El wrapper descarga Gradle 8.11.1 y verifica
su SHA-256. Configurar `JAVA_HOME` y `ANDROID_HOME` o `sdk.dir` en
`local.properties` (archivo local, excluido de Git).

En Windows:

```powershell
.\scripts\verify.ps1 -Release
```

El script usa las herramientas de `.tools` cuando existen. También configura
una carpeta local de sockets para evitar el fallo `Unable to establish loopback
connection` de Java en algunos terminales empaquetados de Windows. No cambia la
configuración global del equipo. La propiedad utilizada está documentada en
[Java Networking](https://docs.oracle.com/en/java/javase/17/core/java-networking.html).
El script evita mantener un daemon y desactiva la observación de archivos para
evitar bloqueos de JAR y detección incompleta de cambios en este entorno Windows.

En Linux/macOS o con un entorno Android ya configurado:

```sh
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
```

Las pruebas de persistencia usan Room real con Robolectric, sin requerir emulador.
Cubren CRUD de las cuatro entidades, restricciones referenciales, actualizaciones,
precisión, orden temporal y conservación de datos al reabrir la base.

**Bloque 00–03 validado:** APK debug y release generadas, 17 pruebas aprobadas
(5 de modelos/conversiones y 12 de persistencia) y Android Lint sin errores.
El esquema Room v1 está incluido en Git. Las advertencias de versiones más
recientes de dependencias quedan como mantenimiento posterior.

**Bloque 04–08 validado:** APK debug y release generadas, 20 pruebas aprobadas
(incluidas tres del parser) y Android Lint sin errores. La app todavía no analiza
Uber en vivo. El OCR y el rendimiento siguen pendientes de validación en un
dispositivo real de 4 GB; una compilación correcta no sustituye esa prueba.

**Ticket 09 validado:** motor de costos y calculadora visual, con 23 pruebas
aprobadas en total y Android Lint sin errores.

GitHub Actions ejecuta las mismas comprobaciones en cada push a `develop`/`main`
y en cada pull request, conserva los informes y publica la APK de depuración
como artefacto de la ejecución.

## Organización

- `core/model`: modelos sin dependencia de Android o Room.
- `core/repository`: contratos de acceso a datos con suspend y Flow.
- `core/database`: entidades, DAOs, conversiones e implementaciones Room.
- `AppContainer`: inyección manual y una base compartida por proceso.

Las siguientes funcionalidades se añadirán en `uber/parser`, `uber/accessibility`,
`history/importer`, `analyzer`, `overlay` y, en Fase 2, `dashboard`, sin crear
implementaciones ficticias en este bloque.

## Convenciones de datos

- Importes en unidades menores enteras (`Long`, centavos para MXN), con moneda.
- Tasas por kilómetro y distancias en `BigDecimal`, almacenadas como texto exacto.
  Las agregaciones decimales se harán en Kotlin; no usar SUM sobre esos textos.
- Duraciones en segundos; tiempos en `Instant`, persistidos como UTC de longitud
  fija con nueve decimales para mantener precisión y orden cronológico.
- Datos desconocidos como null; cero representa un valor conocido.
- `earningsMinor` representa el total final, que incluye `tipMinor` cuando se
  conoce su desglose. Nunca sumar otra vez la propina ni el efectivo recibido.
  El futuro parser debe normalizar cada pantalla de Uber a esta convención.
- Un viaje histórico puede no tener oferta. Una oferta tiene como máximo un viaje.
  Eliminar una oferta conserva el viaje y deja su relación en null.
- Insertar un ID existente falla; actualizar no reemplaza filas ni elimina relaciones.
- No se habilitan permisos de captura ni copias de seguridad automáticas.

## Versionado de la base

Room exporta esquemas en `app/schemas`. No se habilita borrado destructivo al
migrar. Los cambios futuros requieren migración y prueba de conservación de datos.

Compatibilidad de herramientas: [AGP 8.9](https://developer.android.com/build/releases/past-releases/agp-8-9-0-release-notes)
y [Room](https://developer.android.com/jetpack/androidx/releases/room).
