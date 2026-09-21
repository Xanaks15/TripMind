# Esquemas Room

Room exporta automáticamente el JSON de cada versión en este directorio al compilar.
Incluir esos JSON en Git después de la primera compilación; no escribirlos a mano.

Para cambiar una tabla: incrementar la versión, añadir una Migration a
TripMindDatabase.MIGRATIONS y probar la actualización conservando los datos con
MigrationTestHelper. No utilizar fallbackToDestructiveMigration.

La versión 1 no necesita una migración desde una versión anterior.
