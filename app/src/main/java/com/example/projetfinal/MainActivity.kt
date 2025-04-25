package com.example.projetfinal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import coil.compose.rememberAsyncImagePainter
import com.example.projetfinal.ui.theme.ProjetFinalTheme
import com.google.android.gms.location.LocationServices
import data.api.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.Locale
import data.CityList

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProjetFinalTheme {
                val context = LocalContext.current
                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                val coroutineScope = rememberCoroutineScope()

                var location by remember { mutableStateOf<Location?>(null) }
                val fusedLocationClient = remember {
                    LocationServices.getFusedLocationProviderClient(context)
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            location = loc
                        }
                    } else {
                        Toast.makeText(context, "Permission refusée", Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            location = loc
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }

                val apiKey = "8ae75617928daadf28762df0af085cae"
                var weatherDescription by remember { mutableStateOf<String?>(null) }
                var temperature by remember { mutableStateOf<Float?>(null) }
                var cityName by remember { mutableStateOf<String?>(null) }
                var iconCode by remember { mutableStateOf<String?>(null) }
                var cityInput by remember { mutableStateOf("") }
                var citySuggestions by remember { mutableStateOf(listOf<String>()) }

                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val lastSearchedCity = remember {
                    context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
                        .getString("last_city", null)
                }

                LaunchedEffect(location) {
                    if (location != null && cityInput.isBlank()) {
                        try {
                            val weather = RetrofitInstance.api.getCurrentWeather(
                                lat = location!!.latitude,
                                lon = location!!.longitude,
                                apiKey = apiKey
                            )
                            temperature = weather.main.temp
                            cityName = weather.name
                            weatherDescription = weather.weather[0].description
                            iconCode = weather.weather[0].icon
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Erreur météo depuis la localisation",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Dernière ville consultée :",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Text(
                                text = lastSearchedCity ?: "Aucune",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    },
                    drawerState = drawerState
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Ma Météo") },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            drawerState.open()
                                        }
                                    }) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    ) { padding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF87CEFA),
                                            Color(0xFFE0F7FA)
                                        )
                                    )
                                )
                                .padding(padding)
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                Spacer(modifier = Modifier.height(32.dp))

                                OutlinedTextField(
                                    value = cityInput,
                                    onValueChange = { input ->
                                        cityInput = input
                                        citySuggestions = if (input.isNotBlank()) {
                                            CityList.cities.filter {
                                                it.lowercase(Locale.ROOT)
                                                    .contains(input.lowercase(Locale.ROOT))
                                            }
                                        } else {
                                            emptyList()
                                        }
                                    },
                                    label = { Text("Rechercher une ville") },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            if (cityInput.isNotBlank()) {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    try {
                                                        val weather = RetrofitInstance.api.getCurrentWeatherByCity(
                                                            cityName = cityInput,
                                                            apiKey = apiKey
                                                        )
                                                        temperature = weather.main.temp
                                                        cityName = weather.name
                                                        weatherDescription = weather.weather[0].description
                                                        iconCode = weather.weather[0].icon
                                                        citySuggestions = emptyList()
                                                        saveLastSearchedCity(context, weather.name)

                                                        // Fermer le clavier et le focus après recherche
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()

                                                    } catch (e: Exception) {
                                                        Log.e("API", "Erreur API : $e")
                                                    }
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Search, contentDescription = "Rechercher")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (citySuggestions.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        citySuggestions.forEach { suggestion ->
                                            Text(
                                                text = suggestion,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        cityInput = suggestion
                                                        citySuggestions = emptyList()
                                                    }
                                                    .padding(8.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Greeting(
                                    name = location?.let {
                                        "Lat: ${it.latitude}, Lon: ${it.longitude}"
                                    } ?: "Position inconnue",
                                    city = cityName,
                                    temp = temperature?.toDouble(),
                                    description = weatherDescription,
                                    iconCode = iconCode
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveLastSearchedCity(context: Context, cityName: String) {
        val sharedPref = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("last_city", cityName).apply()
    }
}

@Composable
fun Greeting(
    name: String,
    city: String?,
    temp: Double?,
    description: String?,
    iconCode: String?,
    modifier: Modifier = Modifier
) {
    val tempFormatted = temp?.let { String.format("%.1f", it) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = city ?: "Chargement...", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        if (iconCode != null) {
            val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@4x.png"
            Image(
                painter = rememberAsyncImagePainter(iconUrl),
                contentDescription = description,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = tempFormatted?.let { "$it°C" } ?: "– °C",
            style = MaterialTheme.typography.displayMedium
        )

        Text(
            text = description?.replaceFirstChar { it.uppercase() } ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "📍 $name",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
