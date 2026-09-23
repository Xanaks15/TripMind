package com.tripmind.uber.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import com.tripmind.TripMindApplication
import com.tripmind.analyzer.Recommendation
import com.tripmind.analyzer.TripAnalysis
import com.tripmind.analyzer.TripAnalyzer
import com.tripmind.core.model.OfferStatus
import com.tripmind.core.model.Trip
import com.tripmind.settings.DefaultVehicleProfile
import com.tripmind.uber.detector.OfferDeduplicator
import com.tripmind.uber.detector.TripStateSignal
import com.tripmind.uber.detector.UberTripStateTracker
import com.tripmind.uber.detector.identityKey
import com.tripmind.uber.parser.UberIncomingOfferParser
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UberAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = UberIncomingOfferParser()
    private val deduplicator = OfferDeduplicator()
    private val tracker = UberTripStateTracker()
    private val analyzer = TripAnalyzer()
    private val overlay by lazy { RecommendationOverlay(this) }
    private val container by lazy { (application as TripMindApplication).container }
    private val preferences by lazy { getSharedPreferences(PREFERENCES, MODE_PRIVATE) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName !in UBER_PACKAGES) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val root = rootInActiveWindow ?: return
        val rawText = collectText(root)
        if (rawText.isBlank()) return
        scope.launch { process(rawText) }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        overlay.hide()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun process(rawText: String) {
        val activeOfferId = preferences.getString(ACTIVE_OFFER_ID, null)
        if (activeOfferId != null) {
            when (tracker.detect(rawText)) {
                TripStateSignal.Accepted -> {
                    markAccepted(activeOfferId)
                    return
                }
                TripStateSignal.Completed -> {
                    markCompleted(activeOfferId)
                    return
                }
                null -> {
                    val current = container.offers.get(activeOfferId)
                    if (current?.status == OfferStatus.ACCEPTED) return
                }
            }
        }

        val offer = parser.parse(rawText) ?: return
        if (deduplicator.isDuplicate(offer) || isPersistedDuplicate(offer)) return

        activeOfferId?.let { previousId ->
            container.offers.get(previousId)?.takeIf { it.status == OfferStatus.ANALYZED }?.let {
                container.offers.update(it.copy(status = OfferStatus.EXPIRED))
            }
        }
        container.offers.create(offer)
        val vehicle = container.vehicles.get(DefaultVehicleProfile.ID) ?: DefaultVehicleProfile.create()
        val analysis = analyzer.analyze(offer, vehicle)
        container.offers.update(offer.copy(status = OfferStatus.ANALYZED))
        preferences.edit().putString(ACTIVE_OFFER_ID, offer.id).apply()
        overlay.show(analysis)
    }

    private suspend fun isPersistedDuplicate(offer: com.tripmind.core.model.Offer): Boolean =
        container.offers.observePage(limit = 10).first().any { previous ->
            previous.identityKey() == offer.identityKey() &&
                !offer.detectedAt.isBefore(previous.detectedAt) &&
                Duration.between(previous.detectedAt, offer.detectedAt) <= Duration.ofSeconds(30)
        }

    private suspend fun markAccepted(offerId: String) {
        val offer = container.offers.get(offerId) ?: return
        if (offer.status != OfferStatus.ACCEPTED) container.offers.update(offer.copy(status = OfferStatus.ACCEPTED))
        if (container.trips.getByOfferId(offerId) == null) {
            container.trips.create(
                Trip(
                    offerId = offerId,
                    acceptedAt = Instant.now(),
                    estimatedDurationSeconds = offer.estimatedDurationSeconds,
                    estimatedDistanceKm = offer.estimatedDistanceKm,
                    pickupName = offer.pickupName,
                    pickupAddress = offer.pickupAddress,
                    dropoffAddress = offer.dropoffAddress,
                ),
            )
        }
    }

    private suspend fun markCompleted(offerId: String) {
        markAccepted(offerId)
        val trip = container.trips.getByOfferId(offerId) ?: return
        val completedAt = Instant.now()
        val duration = trip.acceptedAt?.let { Duration.between(it, completedAt).seconds.coerceAtLeast(0) }
        container.trips.update(trip.copy(completedAt = completedAt, actualDurationSeconds = duration))
        preferences.edit().remove(ACTIVE_OFFER_ID).apply()
        overlay.hide()
    }

    private fun collectText(root: AccessibilityNodeInfo): String {
        val values = linkedSetOf<String>()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > MAX_TREE_DEPTH || values.size >= MAX_TEXT_NODES) return
            node.text?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let(values::add)
            node.contentDescription?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let(values::add)
            repeat(node.childCount) { index -> node.getChild(index)?.let { visit(it, depth + 1) } }
        }
        visit(root, 0)
        return values.joinToString("\n").take(MAX_RAW_TEXT_LENGTH)
    }

    companion object {
        const val PREFERENCES = "uber_capture_state"
        const val ACTIVE_OFFER_ID = "active_offer_id"
        private const val MAX_TREE_DEPTH = 40
        private const val MAX_TEXT_NODES = 500
        private const val MAX_RAW_TEXT_LENGTH = 20_000
        private val UBER_PACKAGES = setOf("com.ubercab.driver")
    }
}

private class RecommendationOverlay(private val service: AccessibilityService) {
    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = service.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
    private var view: TextView? = null
    private val hideAction = Runnable { hide() }

    fun show(analysis: TripAnalysis) = handler.post {
        hideNow()
        val (title, color) = when (analysis.recommendation) {
            Recommendation.GREEN -> "🟢 CONVIENE" to Color.rgb(24, 120, 68)
            Recommendation.YELLOW -> "🟡 REVISAR" to Color.rgb(176, 122, 0)
            Recommendation.RED -> "🔴 NO CONVIENE" to Color.rgb(170, 42, 42)
        }
        val hourly = analysis.netPerHourMinor?.toPesos()?.let { "$it/h" } ?: "$/h pendiente"
        val perKm = analysis.netPerKmMinor?.toPesos()?.let { "$it/km" } ?: "$/km pendiente"
        val textView = TextView(service).apply {
            text = "$title\n$hourly · $perKm"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(28, 20, 28, 20)
            elevation = 12f
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = 24f
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 120
        }
        runCatching { windowManager.addView(textView, params) }.onSuccess { view = textView }
        handler.removeCallbacks(hideAction)
        handler.postDelayed(hideAction, 15_000)
    }

    fun hide() = handler.post { hideNow() }

    private fun hideNow() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        handler.removeCallbacks(hideAction)
    }

    private fun BigDecimal.toPesos(): String = "$" + movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString()
}
