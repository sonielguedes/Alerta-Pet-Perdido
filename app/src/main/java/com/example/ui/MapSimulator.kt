package com.example.ui

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PetAlert
import kotlin.math.absoluteValue

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
    val textMeasurer = rememberTextMeasurer()

    // Bounding box representing the core Paulista area we seed
    val minLat = -23.5720
    val maxLat = -23.5510
    val minLon = -46.6700
    val maxLon = -46.6380

    // Pulse animation for user location dot
    val infiniteTransition = rememberInfiniteTransition()
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Preset positions for easy test-user jumping
    val locationPresets = listOf(
        Triple(-23.5615, -46.6560, "MASP - Av. Paulista 1500"),
        Triple(-23.5580, -46.6620, "Consolação - Rua Augusta 1200"),
        Triple(-23.5630, -46.6540, "Jardins - Al. Santos 900"),
        Triple(-23.5650, -46.6510, "Paraíso - Pça Oswaldo Cruz")
    )

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

            // Simulated Map Box Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)), shape = RoundedCornerShape(24.dp))
                    .pointerInput(alerts) {
                        detectTapGestures { offset ->
                            // Translate click offset back to latitude/longitude to change location
                            val pctX = offset.x / size.width
                            val pctY = offset.y / size.height
                            
                            val clickedLon = minLon + pctX * (maxLon - minLon)
                            val clickedLat = minLat + (1.0 - pctY) * (maxLat - minLat)
                            
                            // Check if clicked close to any alert pin (approx 15dp tolerance)
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
                // Background Drawing Canvas
                val streetColor = Color(0xFFFCFBF9)
                val riverColor = Color(0xFFBACEDB)
                val textStyle = TextStyle(
                    color = Color(0x66444444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw river on the top corner (Tietê Mockup)
                    drawRect(
                        color = riverColor.copy(alpha = 0.5f),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h * 0.08f)
                    )

                    // Helper mapping to screen pixels
                    fun getX(lon: Double) = (((lon - minLon) / (maxLon - minLon)) * w).toFloat()
                    fun getY(lat: Double) = ((1.0 - ((lat - minLat) / (maxLat - minLat))) * h).toFloat()

                    // 2. Draw Simulated Grid Streets (Lanes)
                    // Av. Paulista (Horizontal)
                    drawLine(
                        color = streetColor,
                        start = Offset(0f, getY(-23.5615)),
                        end = Offset(w, getY(-23.5615)),
                        strokeWidth = 24f
                    )
                    // Alameda Santos (Horizontal)
                    drawLine(
                        color = streetColor,
                        start = Offset(0f, getY(-23.5645)),
                        end = Offset(w, getY(-23.5645)),
                        strokeWidth = 16f
                    )
                    // Alameda Lorena (Horizontal)
                    drawLine(
                        color = streetColor,
                        start = Offset(0f, getY(-23.5685)),
                        end = Offset(w, getY(-23.5685)),
                        strokeWidth = 14f
                    )

                    // Rua Augusta (Vertical)
                    drawLine(
                        color = streetColor,
                        start = Offset(getX(-46.6620), 0f),
                        end = Offset(getX(-46.6620), h),
                        strokeWidth = 16f
                    )
                    // Al. Casa Branca (Vertical)
                    drawLine(
                        color = streetColor,
                        start = Offset(getX(-46.6590), 0f),
                        end = Offset(getX(-46.6590), h),
                        strokeWidth = 14f
                    )
                    // Av. Brig. Luis Antonio (Vertical Angle)
                    drawLine(
                        color = streetColor,
                        start = Offset(getX(-46.6515), 0f),
                        end = Offset(getX(-46.6510), h),
                        strokeWidth = 20f
                    )

                    // Street Labels
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "AVENIDA PAULISTA",
                        topLeft = Offset(w * 0.1f, getY(-23.5615) - 15f),
                        style = textStyle.copy(fontWeight = FontWeight.Bold)
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "ALAMEDA SANTOS",
                        topLeft = Offset(w * 0.4f, getY(-23.5645) - 13f),
                        style = textStyle
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "RUA AUGUSTA",
                        topLeft = Offset(getX(-46.6620) + 10f, h * 0.6f),
                        style = textStyle
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "PÇA OSWALDO CRUZ",
                        topLeft = Offset(getX(-46.6510) - 110f, getY(-23.5650) - 12f),
                        style = textStyle.copy(color = Color(0xFF6B824A))
                    )

                    // 4. Draw Park Green Area (Parque Trianon)
                    drawRect(
                        color = Color(0xFFC0D8B4),
                        topLeft = Offset(getX(-46.6590), getY(-23.5615) + 12f),
                        size = Size(getX(-46.6565) - getX(-46.6590), getY(-23.5645) - getY(-23.5615) - 24f)
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "PQ. TRIANON",
                        topLeft = Offset(getX(-46.6590) + 5f, getY(-23.5615) + 20f),
                        style = textStyle.copy(color = Color(0xFF3B5E2C), fontSize = 8.sp)
                    )

                    // 5. Draw Pet Pins
                    alerts.forEach { alert ->
                        val alertX = getX(alert.longitude)
                        val alertY = getY(alert.latitude)

                        // Choose color depending on Type (Dog: Coral, Cat: Teal)
                        val pinColor = if (alert.petType == "CACHORRO") Color(0xFFFF6F59) else Color(0xFF139A8C)
                        val strokeColor = if (alert.status == "PERDIDO") Color(0xFFB3261E) else Color(0xFF388E3C)

                        // Pin Droplet
                        drawCircle(
                            color = strokeColor,
                            center = Offset(alertX, alertY),
                            radius = 16f
                        )
                        drawCircle(
                            color = pinColor,
                            center = Offset(alertX, alertY),
                            radius = 12f
                        )
                    }

                    // 6. Draw User Location Pin Pulsing Ring
                    val uX = getX(userLon)
                    val uY = getY(userLat)

                    // Pulsing blue ring
                    drawCircle(
                        color = Color(0xFF2196F3).copy(alpha = pulseAlpha),
                        center = Offset(uX, uY),
                        radius = pulseRadius
                    )

                    // Solid blue circle with white frame
                    drawCircle(
                        color = Color.White,
                        center = Offset(uX, uY),
                        radius = 10f
                    )
                    drawCircle(
                        color = Color(0xFF2196F3),
                        center = Offset(uX, uY),
                        radius = 7f
                    )
                }

                // Instructions in-map
                Text(
                    text = "*Toque em qualquer rua para mover-se ou toque no Pin de um pet",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
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
