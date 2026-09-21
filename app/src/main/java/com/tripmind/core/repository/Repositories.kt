package com.tripmind.core.repository

import com.tripmind.core.model.*
import kotlinx.coroutines.flow.Flow

/** Insert rejects duplicate IDs; update/delete return false when the ID does not exist. */
interface Repository<T> {
    suspend fun create(value: T)
    suspend fun get(id: String): T?
    /** Bounded history queries: callers load only the page currently needed. */
    fun observePage(limit: Int = 50, offset: Int = 0): Flow<List<T>>
    suspend fun update(value: T): Boolean
    suspend fun delete(id: String): Boolean
}

interface OfferRepository : Repository<Offer>
interface TripRepository : Repository<Trip>
interface DriverSessionRepository : Repository<DriverSession>
interface VehicleProfileRepository : Repository<VehicleProfile>
