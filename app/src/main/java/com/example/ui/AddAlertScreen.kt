package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlertScreen(
    currentLat: Double,
    currentLon: Double,
    onBackClick: () -> Unit,
    onSubmitAlert: (
        name: String,
        type: String,
        breed: String,
        color: String,
        description: String,
        latitude: Double,
        longitude: Double,
        address: String,
        ownerName: String,
        ownerPhone: String,
        reward: String,
        avatarSeed: Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var petName by remember { mutableStateOf("") }
    var petType by remember { mutableStateOf("CACHORRO") } // CACHORRO or GATO
    var breed by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var reward by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    // Default coordinates to slightly offset from current user GPS position to simulate realistic distance
    var latOffset by remember { mutableStateOf(0.001) } // will add to current userLat
    var lonOffset by remember { mutableStateOf(-0.001) } // will add to current userLon

    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    
    val avatarSeed = remember { Random.nextInt(1, 5) }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar Pet Perdido", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Tip Bento Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Seu alerta será propagado instantaneamente para todos os vizinhos conectados num raio de 5km via notificação push.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Pet Type Toggle (Dog vs Cat)
            Text(
                text = "Tipo do Animal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (petType == "CACHORRO") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (petType == "CACHORRO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { petType = "CACHORRO" }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text("🐶", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Cachorro", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (petType == "GATO") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (petType == "GATO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { petType = "GATO" }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text("🐱", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Gato", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Pet Information Fields
            Text(
                text = "Informações Básicas do Pet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            
            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Nome do Pet (Ex: Pipoca)") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pet_name_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Raça (Ex: Poodle)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Cor predominante") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = reward,
                onValueChange = { reward = it },
                label = { Text("Recompensa (Opcional - Ex: R$ 300)") },
                leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição / Sinais Particulares") },
                placeholder = { Text("Ex: Mancando da pata esquerda, muito dócil, usava coleira vermelha...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Geolocation and Address
            Text(
                text = "Geolocalização / Local do Sumiço",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Endereço / Ponto de Referência") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                placeholder = { Text("Ex: Av. Paulista, 1200 - Perto do MASP") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic interactive geolocation adjuster for coordinates simulation
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Ajustar Distância no Sinal GPS do Alerta:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Este alerta será cadastrado próximo às suas coordenadas atuais. Ajuste os controles abaixo para simular distâncias:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Afastar Latitude: ", fontSize = 11.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = latOffset.toFloat(),
                            onValueChange = { latOffset = it.toDouble() },
                            valueRange = -0.01f..0.01f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Afastar Longitude: ", fontSize = 11.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = lonOffset.toFloat(),
                            onValueChange = { lonOffset = it.toDouble() },
                            valueRange = -0.01f..0.01f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val finalLat = currentLat + latOffset
                    val finalLon = currentLon + lonOffset
                    Text(
                        text = "Coordenadas Finais: ${String.format("%.4f", finalLat)}, ${String.format("%.4f", finalLon)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Owner Contato Information
            Text(
                text = "Informações de Contato do Dono",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("Seu Nome") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ownerPhone,
                onValueChange = { ownerPhone = it },
                label = { Text("Telefone / WhatsApp (Celular)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                placeholder = { Text("Ex: (11) 99999-9999") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Error display
            if (showError) {
                Text(
                    text = "* Por favor, preencha o Nome do Pet, Endereço de sumiço e Telefone de contato.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (petName.trim().isEmpty() || address.trim().isEmpty() || ownerPhone.trim().isEmpty()) {
                        showError = true
                    } else {
                        showError = false
                        onSubmitAlert(
                            petName,
                            petType,
                            breed,
                            color,
                            description,
                            currentLat + latOffset,
                            currentLon + lonOffset,
                            address,
                            ownerName,
                            ownerPhone,
                            reward,
                            avatarSeed
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_pet_alert_button"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Publicar e Alertar Comunidade", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
