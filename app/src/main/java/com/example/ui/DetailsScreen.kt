package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PetAlert
import com.example.data.PetComment
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    alert: PetAlert?,
    comments: List<PetComment>,
    userLat: Double,
    userLon: Double,
    onBackClick: () -> Unit,
    onStatusToggle: (Int, String) -> Unit,
    onDeleteClick: (Int) -> Unit,
    onAddComment: (alertId: Int, author: String, msg: String, isSighting: Boolean, sightLat: Double?, sightLon: Double?) -> Unit,
    getDistance: (Double, Double) -> Double,
    modifier: Modifier = Modifier
) {
    if (alert == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val context = LocalContext.current
    val distance = getDistance(alert.latitude, alert.longitude)
    val distanceStr = if (distance < 1.0) "${(distance * 1000).toInt()}m de você" else "${String.format("%.1f", distance)} km de você"

    // Comment inputs
    var authorName by remember { mutableStateOf("") }
    var commentMessage by remember { mutableStateOf("") }
    var selectIsSighting by remember { mutableStateOf(false) }
    var sightLatShift by remember { mutableStateOf(0.000) } // shift from pet original coordinates for sighting spot

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Alerta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Delete alert option
                    IconButton(onClick = { onDeleteClick(alert.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Alerta", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Main Card Summary
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Large Pet mascot avatar - styled with custom 18dp rounded corners and border like the design HTML
                            val col = when (alert.avatarColorSeed) {
                                1 -> Color(0xFFFFE5D9)
                                2 -> Color(0xFFFFF0D4)
                                3 -> Color(0xFFE4F0EC)
                                4 -> Color(0xFFF3E8EE)
                                else -> Color(0xFFECEFF1)
                            }

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(col, RoundedCornerShape(18.dp))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)), RoundedCornerShape(18.dp))
                                    .clip(RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (alert.petType == "CACHORRO") "🐶" else "🐱", fontSize = 36.sp)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = alert.petName,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val isPerdido = alert.status == "PERDIDO"
                                    val bcolor = if (isPerdido) MaterialTheme.colorScheme.error else Color(0xFF03543F)
                                    val bgcol = if (isPerdido) MaterialTheme.colorScheme.errorContainer else Color(0xFFDEF7EC)
                                    val txtcol = if (isPerdido) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF03543F)

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = bgcol),
                                        border = BorderStroke(1.dp, bcolor.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.height(20.dp)
                                    ) {
                                        Text(
                                            text = if (isPerdido) "URGENTE" else "ENCONTRADO",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = txtcol,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${alert.breed} • ${alert.color}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Postado em: ${dateFormat.format(Date(alert.reportedTime))}",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Divider
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Pet Description
                        Text(
                            text = "Descrição:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alert.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Geolocation metadata
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Local: ${alert.address}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Distância das suas coordenadas: $distanceStr",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Reward banner if any
                        if (alert.reward.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Recompensa Oferecida!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = alert.reward,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Contact & Status Action Buttons Row
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Contatos e Ações Rápidas:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Call Action Mobile
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${alert.ownerPhone}"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ligar dono", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Send Mock Whatsapp
                            Button(
                                onClick = {
                                    // Simulated whatsapp intent
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=${alert.ownerPhone.replace(Regex("[^0-9]"), "")}&text=Olá%20pelo%20AlertaPort!%20Tenho%20informações%20sobre%20o%20pet%20${alert.petName}.")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp green
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Toggle resolved button
                        OutlinedButton(
                            onClick = { onStatusToggle(alert.id, alert.status) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("toggle_status_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                if (alert.status == "PERDIDO") Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (alert.status == "PERDIDO") "Marcar como ENCONTRADO!" else "Remarcar como PERDIDO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Local Community Comments Timeline Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mensagens da Comunidade (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Display Comments List
            if (comments.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma pista ou comentário enviado ainda. Seja o primeiro a ajudar!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(comments) { comment ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (comment.isSighting) Color(0xFFFFF7F2) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (comment.isSighting) Color(0xFFFFCCAA) else Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                if (comment.isSighting) Color(0xFFFFEBDD) else MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(if (comment.isSighting) "📍" else "👤", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = comment.author,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (comment.isSighting) Color(0xFFBF5F1B) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Text(
                                    text = dateFormat.format(Date(comment.timestamp)),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = comment.message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Display coordinates if sighting
                            if (comment.isSighting && comment.latitude != null && comment.longitude != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFFFECE0), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFBF5F1B), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Coordenadas relatadas devidamente inseridas no satélite.",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF914410)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Form to Add Pistas / Comments
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enviar Informações ou Comentários:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = authorName,
                            onValueChange = { authorName = it },
                            label = { Text("Seu Nome") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = commentMessage,
                            onValueChange = { commentMessage = it },
                            label = { Text("Mensagem / Pista") },
                            placeholder = { Text("Ex: Vi um cachorro muito parecido brincando com abrigo na Rua Peixoto Gomide...") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth().testTag("comment_message_input")
                        )

                        // Sighting Checkbox with interactive visual offset
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectIsSighting,
                                onCheckedChange = { selectIsSighting = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF6F59)),
                                modifier = Modifier.testTag("sighting_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Isto é um avistamento em tempo real do pet!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selectIsSighting) Color(0xFFE0533C) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Marque para anexar as coordenadas geográficas de satélite e alertar o dono.",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Sighting coordinate coordinate slider adjustment
                        if (selectIsSighting) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0EC)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Ajustar local do avistamento (GPS virtual):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFC7513C)
                                    )
                                    
                                    Slider(
                                        value = sightLatShift.toFloat(),
                                        onValueChange = { sightLatShift = it.toDouble() },
                                        valueRange = -0.005f..0.005f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFF6F59),
                                            activeTrackColor = Color(0xFFFF6F59)
                                        )
                                    )
                                    
                                    val finalSightLat = alert.latitude + sightLatShift
                                    val finalSightLon = alert.longitude + (sightLatShift / 2) // shift combo
                                    Text(
                                        text = "Avistado a apenas ${String.format("%.1f", getDistance(finalSightLat, finalSightLon) * 1000)} metros do sumiço original.",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF913626)
                                    )
                                }
                            }
                        }

                        // Submit comment button
                        Button(
                            onClick = {
                                if (commentMessage.trim().isNotEmpty()) {
                                    val sl = if (selectIsSighting) alert.latitude + sightLatShift else null
                                    val sn = if (selectIsSighting) alert.longitude + (sightLatShift / 2) else null
                                    
                                    onAddComment(
                                        alert.id,
                                        authorName,
                                        commentMessage,
                                        selectIsSighting,
                                        sl,
                                        sn
                                    )
                                    
                                    commentMessage = ""
                                    selectIsSighting = false
                                    authorName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectIsSighting) Color(0xFFFF6F59) else MaterialTheme.colorScheme.primary
                            ),
                            enabled = commentMessage.trim().isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("submit_comment_button")
                        ) {
                            Text(
                                text = if (selectIsSighting) "Relatar Avistamento Urgente 🚨" else "Enviar Mensagem",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
