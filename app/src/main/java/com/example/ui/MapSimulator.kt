package com.example.ui

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.PetAlert
import kotlin.math.absoluteValue

class WebAppInterface(
    private val onMapClick: (Double, Double) -> Unit,
    private val onMarkerClick: (Int) -> Unit,
    private val handler: android.os.Handler
) {
    @JavascriptInterface
    fun onMapClick(lat: Double, lon: Double) {
        handler.post { onMapClick(lat, lon) }
    }

    @JavascriptInterface
    fun onMarkerClick(id: Int) {
        handler.post { onMarkerClick(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSimulator(
    userLat: Double,
    userLon: Double,
    userAddressName: String,
    alerts: List<PetAlert>,
    onSelectAlert: (Int) -> Unit,
    onChangeUserLocation: (Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPinForPreview by remember { mutableStateOf<PetAlert?>(null) }
    var isWebViewSupported by remember { mutableStateOf(false) } // Default to safe Canvas map to prevent any WebKit thread crashes on emulators

    LaunchedEffect(Unit) {
        // Kept safe without automatic thread resource allocation
    }

    // Bounding box representing the core Paulista area we seed for Canvas fallback
    val minLat = -23.5720
    val maxLat = -23.5510
    val minLon = -46.6700
    val maxLon = -46.6380

    // Preset positions for easy test-user jumping
    val locationPresets = listOf(
        Triple(-23.5615, -46.6560, "MASP - Av. Paulista 1500"),
        Triple(-23.5580, -46.6620, "Consolação - Rua Augusta 1200"),
        Triple(-23.5630, -46.6540, "Jardins - Al. Santos 900"),
        Triple(-23.5650, -46.6510, "Paraíso - Pça Oswaldo Cruz")
    )

    // Dynamic Leaflet HTML setup
    val leafletHtml = remember(userLat, userLon) {
        """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" />
                <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>
                <style>
                    body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #F4F0EB; }
                    .leaflet-control-attribution { display: none !important; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map', { 
                        zoomControl: false,
                        attributionControl: false
                    }).setView([$userLat, $userLon], 15);
                    
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19
                    }).addTo(map);

                    // Custom pulsing animation for user marker
                    var style = document.createElement('style');
                    style.innerHTML = '@keyframes pulse { 0% { box-shadow: 0 0 0 0px rgba(33,150,243,0.7); } 70% { box-shadow: 0 0 0 14px rgba(33,150,243,0); } 100% { box-shadow: 0 0 0 0px rgba(33,150,243,0); } }';
                    document.getElementsByTagName('head')[0].appendChild(style);

                    var userIcon = L.divIcon({
                        html: '<div style="background-color: #2196F3; width: 16px; height: 16px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 10px rgba(33,150,243,0.8); animation: pulse 1.50s infinite;"></div>',
                        className: '',
                        iconSize: [22, 22],
                        iconAnchor: [11, 11]
                    });

                    var userMarker = L.marker([$userLat, $userLon], { icon: userIcon }).addTo(map);
                    var alertMarkers = {};

                    function updateUserMarker(lat, lon) {
                        userMarker.setLatLng([lat, lon]);
                        map.panTo([lat, lon]);
                    }

                    function clearAlertMarkers() {
                        for (var id in alertMarkers) {
                            map.removeLayer(alertMarkers[id]);
                        }
                        alertMarkers = {};
                    }

                    function addAlertMarker(id, lat, lon, name, type, status) {
                        var color = (type === 'CACHORRO') ? '#FF6F59' : '#139A8C';
                        var strokeColor = (status === 'PERDIDO') ? '#B3261E' : '#388E3C';
                        var petEmoji = (type === 'CACHORRO') ? '🐶' : '🐱';
                        
                        var customIcon = L.divIcon({
                            html: '<div style="background-color: ' + color + '; width: 34px; height: 34px; border-radius: 50%; border: 2.5px solid ' + strokeColor + '; box-shadow: 0 2px 6px rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; font-size: 18px;">' + petEmoji + '</div>',
                            className: '',
                            iconSize: [34, 34],
                            iconAnchor: [17, 17]
                        });

                        var marker = L.marker([lat, lon], { icon: customIcon }).addTo(map);
                        marker.on('click', function() {
                            AndroidBridge.onMarkerClick(id);
                        });
                        alertMarkers[id] = marker;
                    }

                    map.on('click', function(e) {
                        AndroidBridge.onMapClick(e.latlng.lat, e.latlng.lng);
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Simulador de Mapa do Bairro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Localização: $userAddressName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Sua geolocalização",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation preset chips for easy simulation
            Text(
                text = "Simular Mudança de Local (Clique para Teletransportar):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                locationPresets.forEach { (lat, lon, label) ->
                    val isCurrent = (userLat - lat).absoluteValue < 0.0001 && (userLon - lon).absoluteValue < 0.0001
                    val chipName = label.substringBefore(" -")
                    
                    FilterChip(
                        selected = isCurrent,
                        onClick = { onChangeUserLocation(lat, lon, label) },
                        label = { Text(chipName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Map Engine Switch UI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tipo de Mapa:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !isWebViewSupported,
                        onClick = { isWebViewSupported = false },
                        label = { Text("Simulador ⚡", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = null,
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = isWebViewSupported,
                        onClick = {
                            try {
                                android.webkit.CookieManager.getInstance()
                                isWebViewSupported = true
                            } catch (e: Throwable) {
                                Log.e("MapSimulator", "WebView or WebKit not available on device", e)
                            }
                        },
                        label = { Text("Mapa Real 🌐", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive OpenStreetMap Leaflet WebView Map (or robust Canvas fallback)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)), shape = RoundedCornerShape(24.dp))
            ) {
                if (isWebViewSupported) {
                    var webViewRef by remember { mutableStateOf<WebView?>(null) }
                    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

                    AndroidView(
                        factory = { context ->
                            try {
                                WebView(context).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            try {
                                                alerts.forEach { alert ->
                                                    val escapedName = alert.petName.replace("'", "\\'")
                                                    evaluateJavascript(
                                                        "addAlertMarker(${alert.id}, ${alert.latitude}, ${alert.longitude}, '$escapedName', '${alert.petType}', '${alert.status}');",
                                                        null
                                                    )
                                                }
                                            } catch (e: Throwable) {
                                                Log.e("MapSimulator", "onPageFinished inject initial markers failed", e)
                                            }
                                        }
                                    }
                                    addJavascriptInterface(
                                        WebAppInterface(
                                            onMapClick = { lat, lon ->
                                                selectedPinForPreview = null
                                                onChangeUserLocation(
                                                    lat,
                                                    lon,
                                                    "Ponto Selecionado (${String.format("%.4f", lat)}, ${String.format("%.4f", lon)})"
                                                )
                                            },
                                            onMarkerClick = { id ->
                                                val clickedAlert = alerts.firstOrNull { it.id == id }
                                                if (clickedAlert != null) {
                                                    selectedPinForPreview = clickedAlert
                                                }
                                            },
                                            handler = mainHandler
                                        ),
                                        "AndroidBridge"
                                    )
                                    loadDataWithBaseURL("https://leaflet-map", leafletHtml, "text/html", "UTF-8", null)
                                    webViewRef = this
                                }
                            } catch (e: Throwable) {
                                // Fallback if system failed to initialize WebView (headless or virtual CI devices)
                                Log.e("MapSimulator", "Failed to create/load WebView, switching to Canvas fallback", e)
                                isWebViewSupported = false
                                android.view.View(context)
                            }
                        },
                        update = {
                            // Ensure it keeps references or values updated if needed
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Reactive updates on user position changing
                    LaunchedEffect(userLat, userLon) {
                        try {
                            webViewRef?.evaluateJavascript("updateUserMarker($userLat, $userLon);", null)
                        } catch (e: Throwable) {
                            Log.e("MapSimulator", "updateUserMarker evaluate failed", e)
                        }
                    }

                    // Reactive updates on alerts code changing
                    LaunchedEffect(alerts) {
                        try {
                            webViewRef?.let { webView ->
                                webView.evaluateJavascript("clearAlertMarkers();", null)
                                alerts.forEach { alert ->
                                    val escapedName = alert.petName.replace("'", "\\'")
                                    webView.evaluateJavascript(
                                        "addAlertMarker(${alert.id}, ${alert.latitude}, ${alert.longitude}, '$escapedName', '${alert.petType}', '${alert.status}');",
                                        null
                                    )
                                }
                            }
                        } catch (e: Throwable) {
                            Log.e("MapSimulator", "clear/addAlertMarkers evaluate failed", e)
                        }
                    }
                } else {
                    // Native Canvas-based Fallback Map
                    val streetColor = Color(0xFFFCFBF9)
                    val riverColor = Color(0xFFBACEDB)
                    val textStyle = TextStyle(
                        color = Color(0x66444444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    val textMeasurer = rememberTextMeasurer()

                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseRadius by infiniteTransition.animateFloat(
                        initialValue = 12f,
                        targetValue = 35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "radius"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF4F0EB))
                            .pointerInput(alerts) {
                                detectTapGestures { offset ->
                                    val pctX = offset.x / size.width
                                    val pctY = offset.y / size.height
                                    
                                    val clickedLon = minLon + pctX * (maxLon - minLon)
                                    val clickedLat = minLat + (1.0 - pctY) * (maxLat - minLat)
                                    
                                    val clickedAlert = alerts.firstOrNull { alert ->
                                        val pX = ((alert.longitude - minLon) / (maxLon - minLon)) * size.width
                                        val pY = (1.0 - ((alert.latitude - minLat) / (maxLat - minLat))) * size.height
                                        val dx = pX - offset.x
                                        val dy = pY - offset.y
                                        (dx * dx + dy * dy) < 400f // 20px radius
                                    }

                                    if (clickedAlert != null) {
                                        selectedPinForPreview = clickedAlert
                                    } else {
                                        selectedPinForPreview = null
                                        onChangeUserLocation(
                                            clickedLat,
                                            clickedLon,
                                            "Ponto Selecionado (${String.format("%.4f", clickedLat)}, ${String.format("%.4f", clickedLon)})"
                                        )
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw Tietê River mockup at top
                            drawRect(
                                color = riverColor.copy(alpha = 0.5f),
                                topLeft = Offset(0f, 0f),
                                size = Size(w, h * 0.08f)
                            )

                            fun getX(lon: Double) = (((lon - minLon) / (maxLon - minLon)) * w).toFloat()
                            fun getY(lat: Double) = ((1.0 - ((lat - minLat) / (maxLat - minLat))) * h).toFloat()

                            // Draw streets grid
                            drawLine(color = streetColor, start = Offset(0f, getY(-23.5615)), end = Offset(w, getY(-23.5615)), strokeWidth = 24f)
                            drawLine(color = streetColor, start = Offset(0f, getY(-23.5645)), end = Offset(w, getY(-23.5645)), strokeWidth = 16f)
                            drawLine(color = streetColor, start = Offset(0f, getY(-23.5685)), end = Offset(w, getY(-23.5685)), strokeWidth = 14f)

                            drawLine(color = streetColor, start = Offset(getX(-46.6620), 0f), end = Offset(getX(-46.6620), h), strokeWidth = 16f)
                            drawLine(color = streetColor, start = Offset(getX(-46.6590), 0f), end = Offset(getX(-46.6590), h), strokeWidth = 14f)
                            drawLine(color = streetColor, start = Offset(getX(-46.6515), 0f), end = Offset(getX(-46.6510), h), strokeWidth = 20f)

                            // Labels
                            drawText(textMeasurer = textMeasurer, text = "AVENIDA PAULISTA", topLeft = Offset(w * 0.1f, getY(-23.5615) - 15f), style = textStyle.copy(fontWeight = FontWeight.Bold))
                            drawText(textMeasurer = textMeasurer, text = "ALAMEDA SANTOS", topLeft = Offset(w * 0.4f, getY(-23.5645) - 13f), style = textStyle)
                            drawText(textMeasurer = textMeasurer, text = "RUA AUGUSTA", topLeft = Offset(getX(-46.6620) + 10f, h * 0.6f), style = textStyle)
                            drawText(textMeasurer = textMeasurer, text = "PÇA OSWALDO CRUZ", topLeft = Offset(getX(-46.6510) - 110f, getY(-23.5650) - 12f), style = textStyle.copy(color = Color(0xFF6B824A)))

                            // Draw Trianon Park
                            drawRect(
                                color = Color(0xFFC0D8B4),
                                topLeft = Offset(getX(-46.6590), getY(-23.5615) + 12f),
                                size = Size(getX(-46.6565) - getX(-46.6590), getY(-23.5645) - getY(-23.5615) - 24f)
                            )
                            drawText(textMeasurer = textMeasurer, text = "PQ. TRIANON", topLeft = Offset(getX(-46.6590) + 5f, getY(-23.5615) + 20f), style = textStyle.copy(color = Color(0xFF3B5E2C), fontSize = 8.sp))

                            // Draw pins
                            alerts.forEach { alert ->
                                val alertX = getX(alert.longitude)
                                val alertY = getY(alert.latitude)
                                val pinColor = if (alert.petType == "CACHORRO") Color(0xFFFF6F59) else Color(0xFF139A8C)
                                val strokeColor = if (alert.status == "PERDIDO") Color(0xFFB3261E) else Color(0xFF388E3C)

                                drawCircle(color = strokeColor, center = Offset(alertX, alertY), radius = 16f)
                                drawCircle(color = pinColor, center = Offset(alertX, alertY), radius = 12f)
                            }

                            // Draw user location
                            val uX = getX(userLon)
                            val uY = getY(userLat)
                            drawCircle(color = Color(0xFF2196F3).copy(alpha = pulseAlpha), center = Offset(uX, uY), radius = pulseRadius)
                            drawCircle(color = Color.White, center = Offset(uX, uY), radius = 10f)
                            drawCircle(color = Color(0xFF2196F3), center = Offset(uX, uY), radius = 7f)
                        }
                    }
                }

                // Instructions in-map
                Text(
                    text = "*Navegue livremente. Toque para se teletransportar ou no Emoji do pet para ver detalhes",
                    fontSize = 9.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            // Pin Floating Preview Card
            selectedPinForPreview?.let { alert ->
                Spacer(modifier = Modifier.height(10.dp))
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pet Icon Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (alert.petType == "CACHORRO") Color(0xFFFFE3E0) else Color(0xFFE0F2F1),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (alert.petType == "CACHORRO") "🐶" else "🐱", fontSize = 20.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = alert.petName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Status Badge
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (alert.status == "PERDIDO") Color(0xFFFDE8E8) else Color(0xFFDEF7EC)
                                        ),
                                        modifier = Modifier.height(18.dp)
                                    ) {
                                        Text(
                                            text = alert.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (alert.status == "PERDIDO") Color(0xFF9B1C1C) else Color(0xFF03543F),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${alert.breed} • ${alert.breed}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = alert.address,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Ver Mais action button
                        Button(
                            onClick = { onSelectAlert(alert.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Ver Detalhes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
