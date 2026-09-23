package com.tripmind.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {
    @Insert suspend fun insert(entity: OfferEntity)
    @Update suspend fun update(entity: OfferEntity): Int
    @Query("SELECT * FROM offers WHERE id = :id") suspend fun get(id: String): OfferEntity?
    @Query("SELECT * FROM offers ORDER BY detectedAt DESC, id ASC LIMIT :limit OFFSET :offset")
    fun observePage(limit: Int, offset: Int): Flow<List<OfferEntity>>
    @Query("DELETE FROM offers WHERE id = :id") suspend fun delete(id: String): Int
}

@Dao
interface TripDao {
    @Insert suspend fun insert(entity: TripEntity)
    @Update suspend fun update(entity: TripEntity): Int
    @Query("SELECT * FROM trips WHERE id = :id") suspend fun get(id: String): TripEntity?
    @Query("SELECT * FROM trips WHERE offerId = :offerId LIMIT 1") suspend fun getByOfferId(offerId: String): TripEntity?
    @Query("SELECT * FROM trips ORDER BY completedAt DESC, id ASC LIMIT :limit OFFSET :offset")
    fun observePage(limit: Int, offset: Int): Flow<List<TripEntity>>
    @Query("DELETE FROM trips WHERE id = :id") suspend fun delete(id: String): Int
}

@Dao
interface DriverSessionDao {
    @Insert suspend fun insert(entity: DriverSessionEntity)
    @Update suspend fun update(entity: DriverSessionEntity): Int
    @Query("SELECT * FROM driver_sessions WHERE id = :id") suspend fun get(id: String): DriverSessionEntity?
    @Query("SELECT * FROM driver_sessions ORDER BY connectedAt DESC, id ASC LIMIT :limit OFFSET :offset")
    fun observePage(limit: Int, offset: Int): Flow<List<DriverSessionEntity>>
    @Query("DELETE FROM driver_sessions WHERE id = :id") suspend fun delete(id: String): Int
}

@Dao
interface VehicleProfileDao {
    @Insert suspend fun insert(entity: VehicleProfileEntity)
    @Update suspend fun update(entity: VehicleProfileEntity): Int
    @Query("SELECT * FROM vehicle_profiles WHERE id = :id") suspend fun get(id: String): VehicleProfileEntity?
    @Query("SELECT * FROM vehicle_profiles ORDER BY name DESC, id ASC LIMIT :limit OFFSET :offset")
    fun observePage(limit: Int, offset: Int): Flow<List<VehicleProfileEntity>>
    @Query("DELETE FROM vehicle_profiles WHERE id = :id") suspend fun delete(id: String): Int
}
