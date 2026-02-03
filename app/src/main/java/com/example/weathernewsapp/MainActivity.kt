package com.example.weathernewsapp

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var newsContainer: LinearLayout
    
    private var currentLat: Double = 21.15
    private var currentLon: Double = 81.25
    private var cityName: String = "Detecting..."
    private var stateName: String = "Chhattisgarh"
    private var nearbyArea: String? = null
    private var district: String? = null

    private lateinit var txtLocation: TextView
    private lateinit var txtWeather: TextView
    private lateinit var txtJoke: TextView
    private lateinit var txtFactDisplay: TextView

    private val NEWS_API_KEY = "8b2712a5b06147ae9f867e2ab0466886"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Personal Branding Splash Overlay
        val splashOverlay = findViewById<FrameLayout>(R.id.splashOverlay)
        splashOverlay.visibility = View.VISIBLE
        val composeView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setContent {
                RoyalMaharajSplash(onAnimationFinished = {
                    runOnUiThread {
                        splashOverlay.animate()
                            .alpha(0f)
                            .setDuration(800)
                            .withEndAction { 
                                splashOverlay.visibility = View.GONE 
                            }
                    }
                })
            }
        }
        splashOverlay.addView(composeView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        newsContainer = findViewById(R.id.newsContainer)
        txtLocation = findViewById(R.id.txtLocation)
        txtWeather = findViewById(R.id.txtWeather)
        txtJoke = findViewById(R.id.txtJoke)
        txtFactDisplay = findViewById(R.id.txtFactDisplay)

        val btnWeather = findViewById<Button>(R.id.btnWeather)
        val btnNews = findViewById<Button>(R.id.btnNews)
        val btnJoke = findViewById<Button>(R.id.btnJoke)
        val btnJokeHindi = findViewById<Button>(R.id.btnJokeHindi)
        val btnFact = findViewById<Button>(R.id.btnFact)
        val btnRefreshIcon = findViewById<ImageButton>(R.id.btnRefreshIcon)
        val fabRefreshBottom = findViewById<FloatingActionButton>(R.id.fabRefreshBottom)
        val btnMore = findViewById<ImageButton>(R.id.btnMore)

        checkLocationPermission()

        btnMore.setOnClickListener {
            showMoreOptions()
        }

        swipeRefresh.setOnRefreshListener { refreshAll() }
        btnRefreshIcon.setOnClickListener { swipeRefresh.isRefreshing = true; refreshAll() }
        fabRefreshBottom.setOnClickListener { swipeRefresh.isRefreshing = true; refreshAll() }

        btnWeather.setOnClickListener { updateWeather() }
        btnNews.setOnClickListener { 
            getLastLocation { updateNews() }
        }
        
        btnFact.setOnClickListener {
            fetchData("https://uselessfacts.jsph.pl/random.json?language=en") { result ->
                runOnUiThread {
                    try {
                        txtFactDisplay.text = JSONObject(result).getString("text")
                    } catch (e: Exception) { txtFactDisplay.text = "❌ Fact Error" }
                }
            }
        }

        btnJoke.setOnClickListener {
            fetchData("https://official-joke-api.appspot.com/random_joke") { result ->
                runOnUiThread { txtJoke.text = parseJoke(result) }
            }
        }

        btnJokeHindi.setOnClickListener {
            val hindiJokes = listOf(
                "संता: ओए, ये क्या कर रहा है?\nबंता: यार, मैं तो बस ये देख रहा था कि अगर मैं अपनी आँखें बंद करूँ तो क्या मुझे कुछ दिखेगा!",
                "टीचर: संता, बताओ अकबर ने कब तक राज किया?\nसंता: सर, पेज नंबर 15 से पेज नंबर 22 तक!",
                "टीचर: न्यूटन का नियम बताओ?\nछात्र: सर, न्यूटन अपनी बीवी से पिटकर बाहर बैठा था, तभी उसके सिर पर सेब गिरा और उसने नियम बना दिया!"
            )
            txtJoke.text = "🤣 ${hindiJokes.random()}"
        }
    }

    @Composable
    fun RoyalMaharajSplash(onAnimationFinished: () -> Unit) {
        val transition = rememberInfiniteTransition(label = "LogoPulse")
        val scale by transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Scale"
        )

        LaunchedEffect(Unit) {
            delay(2500) 
            onAnimationFinished()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6200EE),
                            Color(0xFF3700B3)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scale)
                        .clip(CircleShape),
                    color = Color.White.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "RM",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "ROYAL MAHARAJ",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 6.sp
                )
                
                Text(
                    text = "CREATIVE STUDIOS",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 3.sp
                )
            }
        }
    }

    private fun showMoreOptions() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_more, null)
        bottomSheetDialog.setContentView(view)
        
        view.findViewById<TextView>(R.id.btnAbout).setOnClickListener {
            Toast.makeText(this, "Weather News App v1.3", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }
        
        view.findViewById<TextView>(R.id.btnPrivacy).setOnClickListener {
            Toast.makeText(this, "Privacy Policy", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun refreshAll() {
        getLastLocation {
            updateWeather()
            updateNews()
            swipeRefresh.isRefreshing = false
        }
    }

    private fun updateWeather() {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$currentLat&longitude=$currentLon&current_weather=true&hourly=pm2_5"
        fetchData(url) { result ->
            runOnUiThread { txtWeather.text = parseWeatherWithAQI(result) }
        }
    }

    private fun updateNews() {
        val queryParts = mutableListOf<String>()
        nearbyArea?.let { queryParts.add(it) }
        queryParts.add(cityName)
        queryParts.add(stateName)
        
        val localQuery = queryParts.distinct().joinToString(" OR ")
        val url = "https://newsapi.org/v2/everything?q=($localQuery) AND India&language=en&sortBy=publishedAt&pageSize=5&apiKey=$NEWS_API_KEY"
        
        fetchData(url) { result ->
            runOnUiThread { 
                if (!displayNewsCards(result)) {
                    fetchTopIndianNews()
                }
            }
        }
    }

    private fun fetchTopIndianNews() {
        val url = "https://newsapi.org/v2/top-headlines?country=in&pageSize=5&apiKey=$NEWS_API_KEY"
        fetchData(url) { result ->
            runOnUiThread { displayNewsCards(result, isFallback = true) }
        }
    }

    private fun displayNewsCards(jsonString: String, isFallback: Boolean = false): Boolean {
        try {
            val json = JSONObject(jsonString)
            val articles = json.getJSONArray("articles")
            
            if (articles.length() == 0) return false

            newsContainer.removeAllViews()
            
            val headerText = if (isFallback) "🇮🇳 Top Indian News" else "📍 Nearby $cityName News"
            val sectionHeader = TextView(this).apply {
                text = headerText
                textSize = 14f
                setPadding(16, 0, 0, 16)
                setTextColor(android.graphics.Color.DKGRAY)
            }
            newsContainer.addView(sectionHeader)

            for (i in 0 until minOf(5, articles.length())) {
                val art = articles.getJSONObject(i)
                val title = art.getString("title")
                val source = art.getJSONObject("source").getString("name")

                val card = CardView(this).apply {
                    radius = 32f
                    useCompatPadding = true
                    elevation = 4f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,0,0,16) }
                }

                val layout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                }

                val titleTv = TextView(this).apply {
                    text = title
                    textSize = 16f
                    setTextColor(android.graphics.Color.BLACK)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                val sourceTv = TextView(this).apply {
                    text = "📰 $source"
                    textSize = 12f
                    setTextColor(android.graphics.Color.GRAY)
                    setPadding(0, 8, 0, 16)
                }

                val shareBtn = Button(this).apply {
                    text = "Share"
                    textSize = 12f
                    setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_share, 0, 0, 0)
                    background = null
                    setOnClickListener {
                        val itShare = Intent(Intent.ACTION_SEND).apply { 
                            type = "text/plain"; 
                            putExtra(Intent.EXTRA_TEXT, "$title\n\nSource: $source\nShared via Weather News App") 
                        }
                        startActivity(Intent.createChooser(itShare, "Share news via"))
                    }
                }

                layout.addView(titleTv)
                layout.addView(sourceTv)
                layout.addView(shareBtn)
                card.addView(layout)
                newsContainer.addView(card)
            }
            return true
        } catch (e: Exception) { 
            return false
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else { getLastLocation() }
    }

    private fun getLastLocation(onComplete: (() -> Unit)? = null) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLon = location.longitude
                    updateLocationName(currentLat, currentLon)
                }
                onComplete?.invoke()
            }.addOnFailureListener {
                onComplete?.invoke()
            }
        } catch (e: SecurityException) { onComplete?.invoke() }
    }

    private fun updateLocationName(lat: Double, lon: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                cityName = addr.locality ?: "Nearby"
                stateName = addr.adminArea ?: "India"
                nearbyArea = addr.subLocality
                district = addr.subAdminArea
                
                txtLocation.text = if (nearbyArea != null) "📍 $nearbyArea, $cityName" else "📍 $cityName, $stateName"
            }
        } catch (e: Exception) { txtLocation.text = "📍 $lat, $lon" }
    }

    private fun fetchData(url: String, callback: (String) -> Unit) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { }
            override fun onResponse(call: Call, response: Response) {
                response.use { if (response.isSuccessful) callback(response.body?.string() ?: "") }
            }
        })
    }

    private fun parseWeatherWithAQI(jsonString: String): String {
        return try {
            val json = JSONObject(jsonString)
            val current = json.getJSONObject("current_weather")
            val temp = current.getDouble("temperature")
            val code = current.getInt("weathercode")
            val pm25 = json.optJSONObject("hourly")?.getJSONArray("pm2_5")?.optDouble(0) ?: 0.0
            
            val icon = when (code) {
                0 -> "☀️ Sunny"
                1, 2, 3 -> "🌤️ Cloudy"
                45, 48 -> "🌫️ Foggy"
                51, 53, 55, 61, 63, 65 -> "🌧️ Rainy"
                else -> "☁️ Cloudy"
            }
            val aqi = if (pm25 < 12) "Fresh 🟢" else if (pm25 < 35) "Moderate 🟡" else "Polluted 🔴"
            "$icon in $cityName\n\n🌡️ Temp: $temp°C\n🍃 Air Quality: $aqi"
        } catch (e: Exception) { "❌ Weather error" }
    }

    private fun parseJoke(jsonString: String): String {
        return try {
            val json = JSONObject(jsonString)
            "${json.getString("setup")}\n\n— ${json.getString("punchline")}"
        } catch (e: Exception) { "❌ Joke error" }
    }
}
