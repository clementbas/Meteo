package data.api

import data.model.CitySuggestion
import data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "fr",
        @Query("appid") apiKey: String
    ): WeatherResponse

    @GET("data/2.5/weather")
    suspend fun getCurrentWeatherByCity(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "fr"
    ): WeatherResponse

    @GET("geo/1.0/direct")
    suspend fun getCitySuggestions(
        @Query("q") cityName: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): List<CitySuggestion>
}
