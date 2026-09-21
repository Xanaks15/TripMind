# Requisito de rendimiento

Dispositivo objetivo confirmado: Android con **4 GB de RAM y 64 GB de
almacenamiento total**, compartidos con el sistema, Uber Driver y otras apps.
Estos valores no representan memoria ni espacio disponibles para TripMind.

## Decisiones del bloque 00–03

- Historial paginado: 50 registros por defecto, máximo 500 por consulta.
- Índices para fechas, estados y relaciones. No cargar todo el historial en memoria.
- Base Room única por proceso, consultas suspend y observación Flow por página.
- Sin servicios permanentes, tareas periódicas o SDK de analítica en este bloque.
- Release con R8 y reducción de recursos activados.
- Sin imágenes ni capturas en la base. La política de capturas se implementará
  junto con el importador; no se guarda material ficticio ni de demostración.

## Requisitos para los siguientes bloques

- OCR secuencial con concurrencia máxima de una imagen, tamaño de decodificación
  acotado y liberación de recursos después de cada captura. Procesamiento fuera
  del hilo principal y cancelable. Evitar conservar bitmaps durante la revisión.
- No duplicar las capturas originales: usar sus URI mientras sea necesario y
  borrar únicamente temporales propios. La app no borrará originales del usuario.
- Limitar tamaño de rawText y diagnósticos; definir retención antes de activar
  captura continua. No eliminar viajes u ofertas silenciosamente para ahorrar espacio.
- Accesibilidad filtrada por paquete/evento y deduplicación antes de persistir.
- Consultar agregados o páginas en análisis e interfaz; evitar lecturas completas.
- Medir listas largas; introducir paginación por cursor cuando el coste de OFFSET
  lo justifique. Las escrituras simultáneas pueden desplazar páginas con OFFSET.

## Validación antes de cerrar la Fase 1

Medir una compilación release en teléfono real de 4 GB, con Uber Driver abierto:
memoria PSS y picos de OCR, arranque, fluidez de listas, batería, tamaño instalado,
crecimiento de la base y latencia detección → recomendación (objetivo de producto:
1–2 segundos; registrar mediana y percentil 95).

Probar historial de al menos 10 000 ofertas, una importación de 100 capturas,
rotación, proceso recreado y almacenamiento disponible reducido. Verificar que
no haya ANR, cierres por memoria ni pérdida de registros.

Los umbrales de memoria y espacio se fijarán con estas mediciones. No se declara
compatibilidad de rendimiento verificada únicamente por estas decisiones de diseño.
