package com.tripmind

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tripmind.analyzer.cost.TripCostCalculator
import com.tripmind.analyzer.Recommendation
import com.tripmind.analyzer.TripAnalysis
import com.tripmind.analyzer.TripAnalyzer
import com.tripmind.core.model.Offer
import com.tripmind.core.model.Trip
import com.tripmind.core.model.VehicleProfile
import com.tripmind.core.repository.TripRepository
import com.tripmind.core.repository.VehicleProfileRepository
import com.tripmind.history.importer.TripCandidate
import com.tripmind.history.importer.UberScreenshotOcr
import com.tripmind.history.parser.UberEarningsHistoryParser
import com.tripmind.history.repository.OfferTripMatcher
import com.tripmind.settings.DefaultVehicleProfile
import com.tripmind.uber.accessibility.UberAccessibilityService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TripMindApplication).container
        setContent { MaterialTheme { TripMindScreen(container) } }
    }
}

private enum class Section { LIVE, IMPORT, HISTORY, COSTS }

@Composable
private fun TripMindScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val candidates = remember { mutableStateListOf<TripCandidate>() }
    var section by remember { mutableStateOf(Section.LIVE) }
    var processing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        processing = true
        message = null
        scope.launch {
            val parser = UberEarningsHistoryParser()
            val ocr = UberScreenshotOcr()
            var failures = 0
            try {
                uris.forEach { uri ->
                    runCatching { parser.parse(ocr.recognize(context, uri), uri.toString()) }
                        .onSuccess(candidates::add)
                        .onFailure { failures++ }
                }
            } finally {
                ocr.close()
                processing = false
                message = if (failures == 0) {
                    "${uris.size} captura(s) procesada(s). Revisa los datos antes de guardar."
                } else {
                    "Se procesaron ${uris.size - failures}; $failures no pudieron leerse."
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("TripMind", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionButton("En vivo", section == Section.LIVE) { section = Section.LIVE }
                SectionButton("Importar", section == Section.IMPORT) { section = Section.IMPORT }
                SectionButton("Historial", section == Section.HISTORY) { section = Section.HISTORY }
                SectionButton("Costos", section == Section.COSTS) { section = Section.COSTS }
            }
            when (section) {
                Section.LIVE -> LiveSection(container)
                Section.IMPORT -> ImportSection(
                        candidates = candidates,
                        processing = processing,
                        message = message,
                        onPick = { picker.launch("image/*") },
                        onDiscard = { candidates.remove(it) },
                        onSave = { candidate ->
                            scope.launch {
                                runCatching {
                                    val trips = container.trips.observePage(limit = 100).first()
                                    val linked = OfferTripMatcher.find(candidate, trips)
                                    if (linked == null) container.trips.create(candidate.toTrip())
                                    else container.trips.update(OfferTripMatcher.merge(linked, candidate))
                                }
                                    .onSuccess {
                                        candidates.remove(candidate)
                                        message = "Viaje guardado y asociado cuando se encontró una oferta compatible."
                                    }
                                    .onFailure { message = "No se pudo guardar: ${it.message ?: "error desconocido"}" }
                            }
                        },
                    )
                Section.HISTORY -> HistorySection(container.trips)
                Section.COSTS -> CostSection(container.vehicles)
            }
        }
    }
}

@Composable
private fun SectionButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) }
    else OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun ImportSection(
    candidates: List<TripCandidate>,
    processing: Boolean,
    message: String?,
    onPick: () -> Unit,
    onDiscard: (TripCandidate) -> Unit,
    onSave: (TripCandidate) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Importa una o varias capturas del historial de Uber. El reconocimiento ocurre en el dispositivo.")
        Button(onClick = onPick, enabled = !processing) { Text("Seleccionar capturas") }
        if (processing) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator()
            Text("Leyendo capturas…")
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        candidates.forEach { candidate ->
            CandidateEditor(candidate, onDiscard = { onDiscard(candidate) }, onSave = onSave)
        }
    }
}

@Composable
private fun CandidateEditor(
    candidate: TripCandidate,
    onDiscard: () -> Unit,
    onSave: (TripCandidate) -> Unit,
) {
    var earnings by remember(candidate.id) { mutableStateOf(candidate.earningsMinor.toMoneyInput()) }
    var tip by remember(candidate.id) { mutableStateOf(candidate.tipMinor.toMoneyInput()) }
    var cash by remember(candidate.id) { mutableStateOf(candidate.cashCollectedMinor.toMoneyInput()) }
    var duration by remember(candidate.id) { mutableStateOf(candidate.actualDurationSeconds?.toString().orEmpty()) }
    var distance by remember(candidate.id) { mutableStateOf(candidate.actualDistanceKm?.toPlainString().orEmpty()) }
    var pickup by remember(candidate.id) { mutableStateOf(candidate.pickupName.orEmpty()) }
    var destination by remember(candidate.id) { mutableStateOf(candidate.dropoffAddress.orEmpty()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Revisar viaje", style = MaterialTheme.typography.titleLarge)
            Text("Confianza ${(candidate.confidence * 100).toInt()}% · ${candidate.tripType ?: "Tipo desconocido"}")
            candidate.warnings.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
            EditField("Ganancia (MXN)", earnings) { earnings = it }
            EditField("Propina incluida en la ganancia (MXN)", tip) { tip = it }
            EditField("Efectivo recibido, separado (MXN)", cash) { cash = it }
            EditField("Duración (segundos)", duration) { duration = it }
            EditField("Distancia (km)", distance) { distance = it }
            EditField("Comercio", pickup) { pickup = it }
            EditField("Destino", destination) { destination = it }
            candidate.completedAt?.let {
                Text("Fecha: ${DATE_TIME.format(it.atZone(ZoneId.systemDefault()))}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = earnings.toMinorUnitsOrNull() != null,
                    onClick = {
                        onSave(candidate.copy(
                            earningsMinor = earnings.toMinorUnitsOrNull(),
                            tipMinor = tip.toMinorUnitsOrNull(),
                            cashCollectedMinor = cash.toMinorUnitsOrNull(),
                            actualDurationSeconds = duration.toLongOrNull(),
                            actualDistanceKm = distance.decimalOrNull(),
                            pickupName = pickup.nullIfBlank(),
                            dropoffAddress = destination.nullIfBlank(),
                        ))
                    },
                ) { Text("Guardar") }
                OutlinedButton(onClick = onDiscard) { Text("Descartar") }
            }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun LiveSection(container: AppContainer) {
    val context = LocalContext.current
    var settingsRefresh by remember { mutableStateOf(0) }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        settingsRefresh++
    }
    val offers by container.offers.observePage(limit = 25).collectAsState(initial = emptyList())
    val profiles by container.vehicles.observePage(limit = 1).collectAsState(initial = emptyList())
    val vehicle = profiles.firstOrNull() ?: DefaultVehicleProfile.create()
    val enabled = remember(settingsRefresh) { isAccessibilityServiceEnabled(context) }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Analizador en vivo", style = MaterialTheme.typography.headlineSmall)
        Text(
            if (enabled) "Accesibilidad: activa" else "Accesibilidad: desactivada",
            color = if (enabled) Color(0xFF187844) else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
        )
        Text("TripMind solo lee el texto visible de Uber Driver. No pulsa Aceptar, Rechazar ni controla la aplicación.")
        Button(onClick = {
            settingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) { Text("Abrir ajustes de accesibilidad") }
        Text("Configura tus costos y objetivos en la pestaña Costos antes de una jornada real.")
        Text("Ofertas detectadas", style = MaterialTheme.typography.titleLarge)
        if (offers.isEmpty()) Text("Aún no se han detectado ofertas. Abre Uber Driver después de activar el servicio.")
        offers.forEach { offer -> OfferAnalysisCard(offer, TripAnalyzer().analyze(offer, vehicle)) }
    }
}

@Composable
private fun OfferAnalysisCard(offer: Offer, analysis: TripAnalysis) {
    val (label, color) = when (analysis.recommendation) {
        Recommendation.GREEN -> "🟢 CONVIENE" to Color(0xFF187844)
        Recommendation.YELLOW -> "🟡 REVISAR" to Color(0xFF9A6A00)
        Recommendation.RED -> "🔴 NO CONVIENE" to Color(0xFFAA2A2A)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("Oferta: ${BigDecimal.valueOf(offer.offeredAmountMinor).toMxn()}")
            Text("Bruto/h: ${analysis.grossPerHourMinor?.toMxn() ?: "pendiente"}")
            Text("Bruto/km: ${analysis.grossPerKmMinor?.toMxn() ?: "pendiente"}")
            Text("Neto: ${analysis.netEarningsMinor?.toMxn() ?: "pendiente"}")
            Text("Neto/h: ${analysis.netPerHourMinor?.toMxn() ?: "pendiente"}")
            Text("Neto/km: ${analysis.netPerKmMinor?.toMxn() ?: "pendiente"}")
            Text("Estado: ${offer.status}")
            Text(DATE_TIME.format(offer.detectedAt.atZone(ZoneId.systemDefault())))
            analysis.reasons.forEach { Text("• $it") }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val expected = ComponentName(context, UberAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

@Composable
private fun CostSection(repository: VehicleProfileRepository) {
    val scope = rememberCoroutineScope()
    val profiles by repository.observePage(limit = 1).collectAsState(initial = emptyList())
    val savedProfile = profiles.firstOrNull()
    var gross by remember { mutableStateOf("90.00") }
    var distance by remember { mutableStateOf("9.0") }
    var fuelPrice by remember { mutableStateOf("24.00") }
    var efficiency by remember { mutableStateOf("12.0") }
    var maintenance by remember { mutableStateOf("0.50") }
    var depreciation by remember { mutableStateOf("0.30") }
    var targetHourly by remember { mutableStateOf("120.00") }
    var targetPerKm by remember { mutableStateOf("8.00") }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedProfile?.id) {
        savedProfile?.let {
            fuelPrice = it.fuelPriceMinorPerLiter.movePointLeft(2).toPlainString()
            efficiency = it.fuelEfficiencyKmPerLiter.toPlainString()
            maintenance = it.maintenanceMinorPerKm.movePointLeft(2).toPlainString()
            depreciation = it.depreciationMinorPerKm.movePointLeft(2).toPlainString()
            targetHourly = BigDecimal.valueOf(it.targetNetHourlyMinor).movePointLeft(2).toPlainString()
            targetPerKm = it.targetNetPerKmMinor.movePointLeft(2).toPlainString()
        }
    }

    val calculation = runCatching {
        val profile = VehicleProfile(
            id = DefaultVehicleProfile.ID,
            name = "Mi vehículo",
            fuelPriceMinorPerLiter = fuelPrice.requiredDecimal().movePointRight(2),
            fuelEfficiencyKmPerLiter = efficiency.requiredDecimal(),
            maintenanceMinorPerKm = maintenance.requiredDecimal().movePointRight(2),
            depreciationMinorPerKm = depreciation.requiredDecimal().movePointRight(2),
            targetNetHourlyMinor = targetHourly.requiredDecimal().movePointRight(2).longValueExact(),
            targetNetPerKmMinor = targetPerKm.requiredDecimal().movePointRight(2),
        )
        val grossMinor = gross.requiredDecimal().movePointRight(2)
        require(grossMinor.signum() >= 0) { "Gross earnings cannot be negative" }
        TripCostCalculator().calculate(distance.requiredDecimal(), profile) to grossMinor
    }.getOrNull()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Calculadora de costos", style = MaterialTheme.typography.headlineSmall)
        Text("Guarda estos valores: serán los que use el análisis automático de ofertas.")
        EditField("Pago de la oferta (MXN)", gross) { gross = it }
        EditField("Distancia del viaje (km)", distance) { distance = it }
        EditField("Precio de gasolina (MXN/L)", fuelPrice) { fuelPrice = it }
        EditField("Rendimiento del vehículo (km/L)", efficiency) { efficiency = it }
        EditField("Mantenimiento (MXN/km)", maintenance) { maintenance = it }
        EditField("Depreciación (MXN/km)", depreciation) { depreciation = it }
        EditField("Objetivo neto (MXN/h)", targetHourly) { targetHourly = it }
        EditField("Objetivo neto (MXN/km)", targetPerKm) { targetPerKm = it }

        if (calculation == null) {
            Text("Revisa los valores: no pueden ser negativos y el rendimiento debe ser mayor que cero.", color = MaterialTheme.colorScheme.error)
        } else {
            val (costs, grossMinor) = calculation
            val profile = VehicleProfile(
                id = DefaultVehicleProfile.ID,
                name = "Mi vehículo",
                fuelPriceMinorPerLiter = fuelPrice.requiredDecimal().movePointRight(2),
                fuelEfficiencyKmPerLiter = efficiency.requiredDecimal(),
                maintenanceMinorPerKm = maintenance.requiredDecimal().movePointRight(2),
                depreciationMinorPerKm = depreciation.requiredDecimal().movePointRight(2),
                targetNetHourlyMinor = targetHourly.requiredDecimal().movePointRight(2).longValueExact(),
                targetNetPerKmMinor = targetPerKm.requiredDecimal().movePointRight(2),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resultado", style = MaterialTheme.typography.titleLarge)
                    CostRow("Combustible", costs.fuelCostMinor)
                    CostRow("Mantenimiento", costs.maintenanceCostMinor)
                    CostRow("Depreciación", costs.depreciationCostMinor)
                    CostRow("Costo total", costs.totalCostMinor, bold = true)
                    CostRow("Neto estimado", costs.netMinor(grossMinor), bold = true)
                }
            }
            Button(onClick = {
                scope.launch {
                    runCatching {
                        if (repository.get(profile.id) == null) repository.create(profile) else repository.update(profile)
                    }.onSuccess { saveMessage = "Perfil guardado. El análisis en vivo ya usa estos objetivos." }
                        .onFailure { saveMessage = "No se pudo guardar el perfil." }
                }
            }) { Text("Guardar perfil del vehículo") }
            saveMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun CostRow(label: String, amountMinor: BigDecimal, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(amountMinor.toMxn(), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun HistorySection(repository: TripRepository) {
    val trips by repository.observePage(limit = 100).collectAsState(initial = emptyList())
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Historial local", style = MaterialTheme.typography.headlineSmall)
        if (trips.isEmpty()) Text("Todavía no hay viajes guardados.")
        trips.forEach { HistoryCard(it) }
    }
}

@Composable
private fun HistoryCard(trip: Trip) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(trip.earningsMinor?.let { "$${it / 100}.${(it % 100).toString().padStart(2, '0')} MXN" } ?: "Ganancia pendiente", fontWeight = FontWeight.Bold)
            trip.completedAt?.let { Text(DATE_TIME.format(it.atZone(ZoneId.systemDefault()))) }
            Text(listOfNotNull(
                trip.actualDurationSeconds?.let { "${it / 60} min ${it % 60} s" },
                trip.actualDistanceKm?.let { "$it km" },
            ).joinToString(" · "))
            trip.pickupName?.let { Text("Comercio: $it") }
            trip.dropoffAddress?.let { Text("Destino: $it") }
            trip.tipMinor?.let { Text("Propina incluida: ${it.toMoneyInput()} MXN") }
            trip.cashCollectedMinor?.let { Text("Efectivo recibido: ${it.toMoneyInput()} MXN") }
        }
    }
}

private fun Long?.toMoneyInput(): String = this?.let { BigDecimal(it).movePointLeft(2).toPlainString() }.orEmpty()
private fun String.toMinorUnitsOrNull(): Long? = runCatching {
    trim().takeIf(String::isNotEmpty)?.replace(',', '.')?.let(::BigDecimal)
        ?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()
}.getOrNull()
private fun String.decimalOrNull(): BigDecimal? = runCatching {
    trim().takeIf(String::isNotEmpty)?.replace(',', '.')?.let(::BigDecimal)
}.getOrNull()
private fun String.nullIfBlank(): String? = trim().ifEmpty { null }
private fun String.requiredDecimal(): BigDecimal = BigDecimal(trim().replace(',', '.'))
private fun BigDecimal.toMxn(): String = "$" + movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString() + " MXN"
private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
