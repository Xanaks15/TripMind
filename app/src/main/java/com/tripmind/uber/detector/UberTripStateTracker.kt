package com.tripmind.uber.detector

sealed interface TripStateSignal {
    data object Accepted : TripStateSignal
    data object Completed : TripStateSignal
}

class UberTripStateTracker {
    fun detect(rawText: String): TripStateSignal? = when {
        COMPLETED.containsMatchIn(rawText) -> TripStateSignal.Completed
        ACCEPTED.containsMatchIn(rawText) -> TripStateSignal.Accepted
        else -> null
    }

    companion object {
        private val ACCEPTED = Regex(
            "\\b(recoger (?:pedido|pasajero)|dir[ií]gete (?:al|a la) (?:punto|recogida)|iniciar (?:viaje|entrega)|navegar al destino)\\b",
            RegexOption.IGNORE_CASE,
        )
        private val COMPLETED = Regex(
            "\\b(viaje completado|entrega completada|pedido entregado|has completado (?:el viaje|la entrega))\\b",
            RegexOption.IGNORE_CASE,
        )
    }
}
