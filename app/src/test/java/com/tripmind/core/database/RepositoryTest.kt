package com.tripmind.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tripmind.AppContainer
import com.tripmind.core.model.*
import com.tripmind.core.repository.Repository
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RepositoryTest {
    private lateinit var database: TripMindDatabase
    private lateinit var repositories: AppContainer
    private val instant = Instant.parse("2026-09-15T23:29:00.123456789Z")

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), TripMindDatabase::class.java,
        ).allowMainThreadQueries().build()
        repositories = AppContainer(database)
    }

    @After fun close() = database.close()

    private fun offer(id: String = "offer") = Offer(
        id = id, detectedAt = instant, offeredAmountMinor = 5450,
        estimatedDistanceKm = BigDecimal("10.04"), estimatedDurationSeconds = 1751,
    )

    private suspend fun <T> checkCrud(repo: Repository<T>, id: String, initial: T, changed: T) {
        assertNull(repo.get(id))
        assertFalse(repo.update(initial))
        repo.create(initial)
        assertEquals(initial, repo.get(id))
        assertEquals(listOf(initial), repo.observePage().first())
        assertTrue(repo.update(changed))
        assertEquals(changed, repo.get(id))
        assertEquals(listOf(changed), repo.observePage().first())
        assertTrue(repo.delete(id))
        assertNull(repo.get(id))
        assertTrue(repo.observePage().first().isEmpty())
        assertFalse(repo.delete(id))
    }

    @Test fun offerCrudPreservesAmountsPrecisionAndStatus() = runTest {
        val initial = offer()
        checkCrud(repositories.offers, initial.id, initial, initial.copy(status = OfferStatus.REJECTED))
    }

    @Test fun importedTripCrudKeepsCashSeparateFromEarnings() = runTest {
        val initial = Trip(id = "trip", completedAt = instant, earningsMinor = 5478,
            cashCollectedMinor = 16900, actualDistanceKm = BigDecimal("8.66"), actualDurationSeconds = 2502)
        checkCrud(repositories.trips, initial.id, initial, initial.copy(earningsMinor = 6000, tipMinor = 522))
    }

    @Test fun sessionCrudPreservesSeconds() = runTest {
        val initial = DriverSession(id = "session", connectedAt = instant)
        checkCrud(repositories.sessions, initial.id, initial, initial.copy(
            disconnectedAt = instant.plusSeconds(3600), onlineSeconds = 3600,
            activeSeconds = 2502, idleSeconds = 1098, offersReceived = 9, offersAccepted = 3,
        ))
    }

    @Test fun vehicleCrudPreservesFractionalCentavoRates() = runTest {
        val initial = VehicleProfile(id = "vehicle", name = "Auto", fuelPriceMinorPerLiter = BigDecimal("2400"),
            fuelEfficiencyKmPerLiter = BigDecimal("12.5"), maintenanceMinorPerKm = BigDecimal("30.125"),
            depreciationMinorPerKm = BigDecimal("20.50"), targetNetHourlyMinor = 12000,
            targetNetPerKmMinor = BigDecimal("800"))
        checkCrud(repositories.vehicles, initial.id, initial, initial.copy(name = "Auto actualizado"))
    }

    @Test fun deletingOfferPreservesCompletedTripAndItsEarnings() = runTest {
        repositories.offers.create(offer())
        val trip = Trip(id = "trip", offerId = "offer", earningsMinor = 5450, tipMinor = 1474)
        repositories.trips.create(trip)
        repositories.offers.delete("offer")
        assertEquals(trip.copy(offerId = null), repositories.trips.get("trip"))
    }

    @Test fun deletingSessionPreservesOffers() = runTest {
        repositories.sessions.create(DriverSession(id = "session", connectedAt = instant))
        repositories.offers.create(offer().copy(sessionId = "session"))
        repositories.sessions.delete("session")
        assertEquals(offer(), repositories.offers.get("offer"))
    }

    @Test fun updatingOfferDoesNotDetachTrip() = runTest {
        repositories.offers.create(offer())
        repositories.trips.create(Trip(id = "trip", offerId = "offer"))
        repositories.offers.update(offer().copy(status = OfferStatus.ACCEPTED))
        assertEquals("offer", repositories.trips.get("trip")?.offerId)
        assertEquals("trip", repositories.trips.getByOfferId("offer")?.id)
    }

    @Test fun duplicateInsertFailsWithoutOverwritingOriginal() = runTest {
        repositories.offers.create(offer())
        expectConstraintFailure { repositories.offers.create(offer().copy(offeredAmountMinor = 9999)) }
        assertEquals(offer(), repositories.offers.get("offer"))
    }

    @Test fun tripRequiresExistingOfferAndOnlyOneTripPerOffer() = runTest {
        expectConstraintFailure { repositories.trips.create(Trip(id = "missing", offerId = "missing")) }
        repositories.offers.create(offer())
        repositories.trips.create(Trip(id = "first", offerId = "offer"))
        expectConstraintFailure { repositories.trips.create(Trip(id = "second", offerId = "offer")) }
        assertEquals(1, repositories.trips.observePage().first().size)
    }

    @Test fun timestampsSortChronologicallyIncludingFractionalSeconds() = runTest {
        val whole = offer("whole").copy(detectedAt = Instant.parse("2026-09-15T23:29:00Z"))
        val fractional = offer("fractional").copy(detectedAt = whole.detectedAt.plusNanos(1))
        repositories.offers.create(whole)
        repositories.offers.create(fractional)
        assertEquals(listOf(fractional, whole), repositories.offers.observePage().first())
    }

    @Test fun historyPagesAreBoundedAndDoNotOverlap() = runTest {
        repeat(5) { index ->
            repositories.offers.create(offer("offer-$index").copy(detectedAt = instant.plusSeconds(index.toLong())))
        }
        val first = repositories.offers.observePage(limit = 2).first()
        val second = repositories.offers.observePage(limit = 2, offset = 2).first()
        val last = repositories.offers.observePage(limit = 2, offset = 4).first()
        assertEquals(listOf("offer-4", "offer-3"), first.map { it.id })
        assertEquals(listOf("offer-2", "offer-1"), second.map { it.id })
        assertEquals(listOf("offer-0"), last.map { it.id })
        assertThrows(IllegalArgumentException::class.java) { repositories.offers.observePage(limit = 501) }
        assertThrows(IllegalArgumentException::class.java) { repositories.offers.observePage(offset = -1) }
    }

    @Test fun dataSurvivesDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        try {
            Room.databaseBuilder(context, TripMindDatabase::class.java, name).build().let { disk ->
                try { RoomOfferRepository(disk.offerDao()).create(offer()) } finally { disk.close() }
            }
            Room.databaseBuilder(context, TripMindDatabase::class.java, name).build().let { disk ->
                try { assertEquals(offer(), RoomOfferRepository(disk.offerDao()).get("offer")) }
                finally { disk.close() }
            }
        } finally { context.deleteDatabase(name) }
    }

    private suspend fun expectConstraintFailure(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected SQLite constraint violation")
        } catch (expected: android.database.sqlite.SQLiteConstraintException) {
            // The original rows must survive; callers assert that separately.
        }
    }
}
