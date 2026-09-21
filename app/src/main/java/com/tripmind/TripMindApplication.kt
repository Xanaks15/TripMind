package com.tripmind

import android.app.Application
import com.tripmind.core.database.*
import com.tripmind.core.repository.*

class TripMindApplication : Application() {
    val container: AppContainer by lazy { AppContainer(TripMindDatabase.create(this)) }
}

/** One database per process. Dependencies can be replaced in tests without a DI framework. */
class AppContainer(database: TripMindDatabase) {
    val offers: OfferRepository = RoomOfferRepository(database.offerDao())
    val trips: TripRepository = RoomTripRepository(database.tripDao())
    val sessions: DriverSessionRepository = RoomDriverSessionRepository(database.driverSessionDao())
    val vehicles: VehicleProfileRepository = RoomVehicleProfileRepository(database.vehicleProfileDao())
}
