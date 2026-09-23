# Prueba de campo — Fase 1

Esta prueba valida lo que no puede comprobarse con datos simulados: qué texto
expone la versión instalada de Uber Driver mediante accesibilidad.

## Preparación

1. Instalar la APK de campo.
2. Abrir TripMind y guardar el perfil real en **Costos**.
3. Abrir **En vivo** y habilitar **TripMind — Analizador Uber** en accesibilidad.
4. Confirmar que TripMind muestre `Accesibilidad: activa` al volver.

TripMind no pulsa botones ni acepta o rechaza solicitudes. El conductor mantiene
siempre el control de Uber Driver.

## Comprobaciones

- Al aparecer una oferta, el overlay debe mostrarse una sola vez en pocos segundos.
- El importe, tiempo y distancia deben coincidir con Uber.
- La recomendación debe aparecer como verde, amarilla o roja.
- La pestaña **En vivo** debe conservar la oferta aunque expire.
- Al aceptar manualmente, la oferta debe cambiar a `ACCEPTED` y crear un viaje.
- Al completar el servicio, el viaje debe recibir una hora de finalización.
- Al importar después la captura del historial, sus ganancias y propina deben
  completar ese viaje, no crear un duplicado.

## Datos que deben anotarse si falla

- Versión de Uber Driver y versión de Android.
- Tipo de solicitud: viaje, entrega o compra.
- Campos visibles que no fueron reconocidos.
- Estado registrado por TripMind.
- Si el overlay no apareció, si apareció tarde o si se repitió.

No usar estas primeras pruebas para tomar decisiones económicas mientras no se
haya confirmado que importe, tiempo y distancia coinciden con la pantalla real.
