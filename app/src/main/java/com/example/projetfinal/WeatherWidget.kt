package com.example.projetfinal

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)

            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)

            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude

                        fetchWeatherAndUpdateWidget(context, latitude, longitude, views, appWidgetManager, appWidgetId)
                    } else {
                        views.setTextViewText(R.id.widget_city_name, "Position inconnue")
                        views.setTextViewText(R.id.widget_temperature, "--°C")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: SecurityException) {
                views.setTextViewText(R.id.widget_city_name, "Permission refusée")
                views.setTextViewText(R.id.widget_temperature, "--°C")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun fetchWeatherAndUpdateWidget(
            context: Context,
            latitude: Double,
            longitude: Double,
            views: RemoteViews,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val apiKey = "8ae75617928daadf28762df0af085cae"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = data.api.RetrofitInstance.api.getCurrentWeather(
                        lat = latitude,
                        lon = longitude,
                        apiKey = apiKey
                    )

                    val city = response.name
                    val temp = response.main.temp.toInt().toString() + "°C"

                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.widget_city_name, city)
                        views.setTextViewText(R.id.widget_temperature, temp)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.widget_city_name, "Erreur API")
                        views.setTextViewText(R.id.widget_temperature, "--°C")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
    }
}
