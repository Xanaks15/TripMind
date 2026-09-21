package com.tripmind.core.database

import com.tripmind.core.model.*
import com.tripmind.core.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomOfferRepository(private val dao: OfferDao) : OfferRepository {
    override suspend fun create(value: Offer) = dao.insert(OfferEntity(value))
    override suspend fun get(id: String): Offer? = dao.get(id)?.value
    override fun observePage(limit: Int, offset: Int): Flow<List<Offer>> {
        require(limit in 1..500 && offset >= 0)
        return dao.observePage(limit, offset).map { rows -> rows.map { it.value } }
    }
    override suspend fun update(value: Offer): Boolean = dao.update(OfferEntity(value)) == 1
    override suspend fun delete(id: String): Boolean = dao.delete(id) == 1
}

class RoomTripRepository(private val dao: TripDao) : TripRepository {
    override suspend fun create(value: Trip) = dao.insert(TripEntity(value))
    override suspend fun get(id: String): Trip? = dao.get(id)?.value
    override fun observePage(limit: Int, offset: Int): Flow<List<Trip>> {
        require(limit in 1..500 && offset >= 0)
        return dao.observePage(limit, offset).map { rows -> rows.map { it.value } }
    }
    override suspend fun update(value: Trip): Boolean = dao.update(TripEntity(value)) == 1
    override suspend fun delete(id: String): Boolean = dao.delete(id) == 1
}

class RoomDriverSessionRepository(private val dao: DriverSessionDao) : DriverSessionRepository {
    override suspend fun create(value: DriverSession) = dao.insert(DriverSessionEntity(value))
    override suspend fun get(id: String): DriverSession? = dao.get(id)?.value
    override fun observePage(limit: Int, offset: Int): Flow<List<DriverSession>> {
        require(limit in 1..500 && offset >= 0)
        return dao.observePage(limit, offset).map { rows -> rows.map { it.value } }
    }
    override suspend fun update(value: DriverSession): Boolean = dao.update(DriverSessionEntity(value)) == 1
    override suspend fun delete(id: String): Boolean = dao.delete(id) == 1
}

class RoomVehicleProfileRepository(private val dao: VehicleProfileDao) : VehicleProfileRepository {
    override suspend fun create(value: VehicleProfile) = dao.insert(VehicleProfileEntity(value))
    override suspend fun get(id: String): VehicleProfile? = dao.get(id)?.value
    override fun observePage(limit: Int, offset: Int): Flow<List<VehicleProfile>> {
        require(limit in 1..500 && offset >= 0)
        return dao.observePage(limit, offset).map { rows -> rows.map { it.value } }
    }
    override suspend fun update(value: VehicleProfile): Boolean = dao.update(VehicleProfileEntity(value)) == 1
    override suspend fun delete(id: String): Boolean = dao.delete(id) == 1
}
