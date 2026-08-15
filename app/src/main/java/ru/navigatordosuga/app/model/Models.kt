package ru.navigatordosuga.app.model

enum class ActivityMode(val wire: String, val title: String) {
    MUSHROOMS("mushrooms", "Грибы"),
    FISHING("fishing", "Рыбалка"),
    BEAUTIFUL("beautiful", "Места"),
    CINEMA("cinema", "Кино"),
    HISTORY("history", "История"),
    EVENTS("events", "Мероприятия");
    companion object { fun fromWire(v: String?) = entries.firstOrNull { it.wire == v } ?: MUSHROOMS }
}

data class GeoItem(
    val id: String,
    val mode: ActivityMode,
    val name: String,
    val lat: Double?,
    val lon: Double?,
    val region: String = "",
    val category: String = "",
    val subCategory: String = "",
    val score: Double = 0.0,
    val secondaryScore: Double = 0.0,
    val confidence: String = "",
    val summary: String = "",
    val iconKey: String = "",
    val updatedAt: String = "",
    val payloadJson: String = "{}"
)

data class EventItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val category: String,
    val startDateTime: String,
    val endDateTime: String?,
    val status: String,
    val venueName: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val priceType: String,
    val priceMin: Double?,
    val priceMax: Double?,
    val registrationRequired: Boolean,
    val ticketUrl: String?,
    val registrationUrl: String?,
    val ageRating: String?,
    val participants: List<String>,
    val sourceConfidence: String,
    val updatedAt: String,
    val payloadJson: String = "{}"
) {
    val isFree: Boolean get() = priceType in setOf("FREE", "FREE_REGISTRATION", "CONDITIONAL_FREE", "DONATION")
}

data class EventFilter(
    val dateFrom: String,
    val dateTo: String,
    val price: String = "all",
    val category: String = "",
    val query: String = ""
)

data class GeoFilter(
    val minScore: Float = 0f,
    val maxDistanceKm: Float = 250f
)

data class UserLocationState(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
    val heading: Float? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class Profile(
    val id: String,
    val displayName: String,
    val avatarUri: String? = null,
    val interests: Set<String> = emptySet(),
    val transportPreference: String = "car",
    val maxTripDistanceKm: Int = 120,
    val defaultActivity: ActivityMode = ActivityMode.MUSHROOMS,
    val theme: String = "system"
)

data class MapCameraState(
    val lat: Double = 55.751244,
    val lon: Double = 37.618423,
    val zoom: Double = 9.5,
    val bearing: Double = 0.0,
    val tilt: Double = 0.0
)


data class SearchHit(
    val id: String,
    val mode: ActivityMode,
    val title: String,
    val subtitle: String = "",
    val lat: Double? = null,
    val lon: Double? = null,
    val eventDate: String? = null
)

data class StoredItem(
    val dataset: String,
    val itemId: String,
    val title: String,
    val savedAt: Long = 0L,
    val position: Int = 0
)
