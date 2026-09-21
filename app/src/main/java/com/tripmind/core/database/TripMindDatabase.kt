package com.tripmind.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

@Database(
    entities = [OfferEntity::class, TripEntity::class, DriverSessionEntity::class, VehicleProfileEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TripMindDatabase : RoomDatabase() {
    abstract fun offerDao(): OfferDao
    abstract fun tripDao(): TripDao
    abstract fun driverSessionDao(): DriverSessionDao
    abstract fun vehicleProfileDao(): VehicleProfileDao

    companion object {
        // No migrations exist at version 1. Add and test every future version transition here.
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun create(context: Context): TripMindDatabase =
            Room.databaseBuilder(context.applicationContext, TripMindDatabase::class.java, "tripmind.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
