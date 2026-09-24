# Política de privacidad de TripMind

Vigente desde el 23 de septiembre de 2026.

TripMind es una aplicación de análisis para conductores. Funciona sin crear una
cuenta y no incorpora publicidad, analítica ni un servidor propio.

## Datos que utiliza

Con autorización expresa del usuario, el servicio de Accesibilidad lee
exclusivamente el contenido visible de la aplicación Uber Driver
(`com.ubercab.driver`). Esto puede incluir importes, tiempos, distancias, puntos
de recogida, destinos y estados del viaje. TripMind utiliza esa información para
calcular indicadores económicos, mostrar recomendaciones y relacionar una oferta
con un viaje terminado.

El usuario también puede seleccionar capturas de su historial de Uber. El texto
se reconoce en el dispositivo y se presenta para revisión antes de guardarlo.
TripMind utiliza además los costos del vehículo introducidos por el usuario.

## Almacenamiento, transferencia y conservación

Las ofertas, viajes, costos y diagnósticos se guardan únicamente en el espacio
privado de la aplicación en el dispositivo. TripMind no vende, comparte ni envía
estos datos a un servidor del desarrollador. Las imágenes seleccionadas se leen
para el reconocimiento local y no se copian a la base de datos de TripMind.

Los datos permanecen en el dispositivo hasta que el usuario los elimina o
desinstala la aplicación. Las copias de seguridad automáticas están desactivadas.

## Accesibilidad y control del usuario

TripMind no es una herramienta de accesibilidad para personas con discapacidad.
Utiliza `AccessibilityService` únicamente para la función de análisis de Uber
descrita anteriormente. No pulsa Aceptar o Rechazar, no realiza gestos y no toma
decisiones en nombre del usuario.

El acceso es opcional. Si el usuario no da su consentimiento, puede seguir usando
la calculadora de costos, la importación manual y el historial. El servicio puede
revocarse en cualquier momento desde los ajustes de Accesibilidad de Android.

## Seguridad y eliminación

TripMind limita el servicio de Accesibilidad al paquete de Uber Driver. Los datos
se almacenan usando el aislamiento estándar de aplicaciones de Android. El usuario
puede eliminar todos los datos desde la información de la aplicación o al
desinstalarla.

## Cambios y contacto

Las actualizaciones de esta política se publicarán en este archivo con una nueva
fecha de vigencia. Para preguntas o solicitudes, abre un issue en:

<https://github.com/Xanaks15/TripMind/issues>
