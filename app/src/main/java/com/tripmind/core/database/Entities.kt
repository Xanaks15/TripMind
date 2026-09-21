package com.tripmind.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tripmind.core.model.DriverSession
import com.tripmind.core.model.Offer
import com.tripmind.core.model.Trip
import com.tripmind.core.model.VehicleProfile

@Entity(tableName = "driver_sessions", primaryKeys = ["id"], indices = [Index("connectedAt")])
data class DriverSessionEntity(@Embedded val value: DriverSession)

@Entity(
    tableName = "offers",
    primaryKeys = ["id"],
    foreignKeys = [ForeignKey(
        entity = DriverSessionEntity::class,
        parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("sessionId"), Index("detectedAt"), Index("status")],
)
data class OfferEntity(@Embedded val value: Offer)

@Entity(
    tableName = "trips",
    primaryKeys = ["id"],
    foreignKeys = [ForeignKey(
        entity = OfferEntity::class,
        parentColumns = ["id"], childColumns = ["offerId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index(value = ["offerId"], unique = true), Index("completedAt")],
)
data class TripEntity(@Embedded val value: Trip)

@Entity(tableName = "vehicle_profiles", primaryKeys = ["id"], indices = [Index("name")])
data class VehicleProfileEntity(@Embedded val value: VehicleProfile)
