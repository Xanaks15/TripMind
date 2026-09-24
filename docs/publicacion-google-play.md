# Preparación y publicación en Google Play

> Alternativa gratuita para pruebas: una cuenta personal de distribución limitada
> en Android Developer Console permite registrar `com.tripmind` y compartir el APK
> con hasta 20 dispositivos autorizados. En ese caso GitHub Releases puede seguir
> alojando el APK. La clave permanente y su huella SHA-256 deben registrarse antes
> de distribuirlo. Esta modalidad no publica la aplicación en Google Play.

## Estado del paquete

- Aplicación: TripMind
- Identificador: `com.tripmind`
- Versión: `0.2.0-alpha01` (`versionCode` 2)
- Canal inicial recomendado: prueba interna
- No publicar en producción antes de validar el analizador con Uber Driver real.

## Crear la cuenta

Crear una cuenta personal en <https://play.google.com/console/>. Google solicita
una cuota única de registro, identidad legal, dirección, correo y teléfono
verificables, un perfil de pagos y la verificación de acceso a un dispositivo
Android real. No compartir con terceros los códigos de verificación.

Las cuentas personales nuevas pueden usar la prueba interna sin completar la
prueba exigida para producción. Para solicitar acceso a producción deberán mantener
al menos 12 probadores inscritos en una prueba cerrada durante 14 días continuos.

## Antes de subir el primer paquete

1. Crear la aplicación con el nombre TripMind y el paquete `com.tripmind`.
2. Aceptar Play App Signing.
3. Crear y guardar fuera del repositorio una clave de subida.
4. Configurar Gradle para firmar el paquete con secretos locales o del sistema CI.
5. Generar un Android App Bundle release con `:app:bundleRelease`.
6. No subir a Git la clave, contraseñas, archivos `keystore.properties` ni JSON de cuentas.

La misma clave de subida debe conservarse para las siguientes versiones. Cada
actualización debe aumentar `versionCode`.

## Declaración de Accesibilidad

TripMind no debe marcarse como herramienta para personas con discapacidad. En la
declaración de `AccessibilityService`, explicar que el uso corresponde a la
funcionalidad principal de análisis de ofertas. Adjuntar un video que muestre:

1. La apertura de TripMind.
2. La divulgación completa dentro de la aplicación.
3. La opción **No ahora**.
4. La opción **Acepto y abrir ajustes**.
5. La activación manual del servicio.
6. Una oferta de prueba y la recomendación, sin aceptar ni rechazar automáticamente.

## Datos y ficha de la tienda

- Publicar `PRIVACY.md` en una URL HTTPS pública y usarla como política de privacidad.
- Declarar que la app procesa actividad de otras aplicaciones y datos generados
  por el usuario sólo en el dispositivo.
- Confirmar en el formulario de seguridad de datos el comportamiento exacto de la
  versión publicada; no asumir que "local" y "recopilado" son sinónimos.
- Indicar claramente que TripMind no está afiliada ni respaldada por Uber.
- Añadir correo de soporte activo, icono, capturas, descripción corta y completa.

## Prueba interna

1. Abrir **Pruebas y lanzamiento → Pruebas → Prueba interna**.
2. Crear una lista de probadores por correo.
3. Subir el `.aab` firmado y completar las advertencias de la versión.
4. Publicar el canal interno y compartir el enlace de inscripción.
5. Instalar exclusivamente desde el enlace de Google Play y validar Accesibilidad,
   detección, recomendaciones, historial y consumo de batería.
