package data.model

data class CitySuggestion(
    val name: String,
    val country: String,
    val state: String? = null,
    val lat: Double,
    val lon: Double
)
