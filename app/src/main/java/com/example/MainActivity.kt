package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val viewModel: PetAlertViewModel = viewModel()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedAlertId by viewModel.selectedAlertId.collectAsState()
    
    val userLat by viewModel.userLatitude.collectAsState()
    val userLon by viewModel.userLongitude.collectAsState()
    val userAddressName by viewModel.userAddressName.collectAsState()

    val alerts by viewModel.filteredAlerts.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()

    val selectedAlert by viewModel.selectedAlert.collectAsState()
    val selectedComments by viewModel.selectedAlertComments.collectAsState()

    // Filters UI States
    val filterType by viewModel.filterType.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val maxDistanceKm by viewModel.maxDistanceKm.collectAsState()

    val scope = rememberCoroutineScope()

    // Push notification banner simulation
    var showPushBanner by remember { mutableStateOf(false) }
    var pushBannerTitle by remember { mutableStateOf("") }
    var pushBannerBody by remember { mutableStateOf("") }
    var lastNotificationId by remember { mutableStateOf(0) }

    // Listen to incoming real-time notifications to slide in an simulated OS Push Notification drawer banner!
    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            val mostRecent = notifications.first()
            if (mostRecent.id > lastNotificationId && !mostRecent.isRead) {
                lastNotificationId = mostRecent.id
                pushBannerTitle = mostRecent.title
                pushBannerBody = mostRecent.body
                
                // Trigger visual slider dropdown
                showPushBanner = true
                delay(6000) // view for 6 seconds
                showPushBanner = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🐾 ", fontSize = 24.sp)
                        Column {
                            Text(
                                "Alerta Pet Perdido",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Comunidade Vigilante • Geofenced",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Quick stats indicators
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                            .clickable { viewModel.navigateTo(PetScreen.NOTIFICATIONS) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alertas",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            if (unreadNotificationsCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$unreadNotificationsCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Pets, contentDescription = "Pets") },
                    label = { Text("Feed", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == PetScreen.FEED || currentScreen == PetScreen.DETAILS,
                    onClick = { viewModel.navigateTo(PetScreen.FEED) },
                    modifier = Modifier.testTag("nav_item_feed")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
                    label = { Text("Mapa", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == PetScreen.MAP,
                    onClick = { viewModel.navigateTo(PetScreen.MAP) },
                    modifier = Modifier.testTag("nav_item_map")
                )
                NavigationBarItem(
                    icon = { 
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge { Text("$unreadNotificationsCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificações")
                        }
                    },
                    label = { Text("Alertas", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == PetScreen.NOTIFICATIONS,
                    onClick = { viewModel.navigateTo(PetScreen.NOTIFICATIONS) },
                    modifier = Modifier.testTag("nav_item_notifications")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "Ajuda") },
                    label = { Text("Ajuda", fontWeight = FontWeight.Bold) },
                    selected = currentScreen == PetScreen.ABOUT,
                    onClick = { viewModel.navigateTo(PetScreen.ABOUT) },
                    modifier = Modifier.testTag("nav_item_about")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switcher
            when (currentScreen) {
                PetScreen.FEED -> {
                    FeedScreen(
                        filteredAlerts = alerts,
                        filterType = filterType ?: "TODOS",
                        filterStatus = filterStatus ?: "TODOS",
                        maxDistance = maxDistanceKm,
                        onSelectType = { viewModel.setFilterType(it) },
                        onSelectStatus = { viewModel.setFilterStatus(it) },
                        onDistanceChange = { viewModel.setMaxDistance(it) },
                        onAddAlertClick = { viewModel.navigateTo(PetScreen.ADD_ALERT) },
                        onViewMapClick = { viewModel.navigateTo(PetScreen.MAP) },
                        onSelectAlert = { id -> viewModel.navigateTo(PetScreen.DETAILS, id) },
                        getDistance = { lat, lon -> viewModel.getDistanceFromUser(lat, lon) }
                    )
                }
                
                PetScreen.MAP -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MapSimulator(
                            userLat = userLat,
                            userLon = userLon,
                            userAddressName = userAddressName,
                            alerts = alerts,
                            onSelectAlert = { id -> viewModel.navigateTo(PetScreen.DETAILS, id) },
                            onChangeUserLocation = { lat, lon, label ->
                                viewModel.updateUserLocation(lat, lon, label)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Toque no mapa ou escolha um bairro para se teleportar. O app atualizará a distância dos pets e registrará notificações inteligentes!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                PetScreen.ADD_ALERT -> {
                    AddAlertScreen(
                        currentLat = userLat,
                        currentLon = userLon,
                        onBackClick = { viewModel.navigateTo(PetScreen.FEED) },
                        onSubmitAlert = { name, type, breed, color, desc, lat, lon, addr, owner, phone, reward, seed ->
                            viewModel.createPetAlert(name, type, breed, color, desc, lat, lon, addr, owner, phone, reward, seed)
                        }
                    )
                }

                PetScreen.DETAILS -> {
                    DetailsScreen(
                        alert = selectedAlert,
                        comments = selectedComments,
                        userLat = userLat,
                        userLon = userLon,
                        onBackClick = { viewModel.navigateTo(PetScreen.FEED) },
                        onStatusToggle = { id, currentStatus -> viewModel.toggleAlertStatus(id, currentStatus) },
                        onDeleteClick = { id -> viewModel.deleteAlert(id) },
                        onAddComment = { id, author, msg, isSighting, sl, sn ->
                            viewModel.addComment(id, author, msg, isSighting, sl, sn)
                        },
                        getDistance = { lat, lon -> viewModel.getDistanceFromUser(lat, lon) }
                    )
                }

                PetScreen.NOTIFICATIONS -> {
                    NotificationScreen(
                        notifications = notifications,
                        onNotificationClick = { alertId ->
                            viewModel.navigateTo(PetScreen.DETAILS, alertId)
                        },
                        onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
                        onClearAll = { viewModel.clearNotifications() }
                    )
                }

                PetScreen.ABOUT -> {
                    AboutScreen(
                        onResetPresets = {
                            viewModel.updateUserLocation(-23.5615, -46.6560, "MASP - Av. Paulista 1500")
                        }
                    )
                }
            }

            // Real-time Push Notification Sliding Drop-down Banner Overlay
            AnimatedVisibility(
                visible = showPushBanner,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.clickable {
                        viewModel.navigateTo(PetScreen.NOTIFICATIONS)
                        showPushBanner = false
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pushBannerTitle,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pushBannerBody,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = { showPushBanner = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar banner", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    onResetPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Alerta Pet: Tecnologia Comunitária de Proximidade",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "O Alerta Pet foi construído para ajudar na busca emergencial de cães e gatos utilizando geolocalização exata de satélite e notificações em tempo real.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Text(
            text = "Como Funciona o Aplicativo?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Guide timeline
        GuideStep(
            step = "1",
            title = "Cadastro de Alertas",
            desc = "Ao registrar um animal perdido no botão \"Reportar Pet\", insira as características (raça, cor) e o local de sumiço. O app calcula as coordenadas GPS e cadastra no banco de dados local Room (SQLite)."
        )

        GuideStep(
            step = "2",
            title = "Cálculo de Proximidade Geofenced",
            desc = "Utilizando a fórmula matemática de Haversine diretamente no código, calculamos a distância entre cada animal perdido e as suas coordenadas geográficas atuais.",
            tinted = true
        )

        GuideStep(
            step = "3",
            title = "Notificações Push Simuladas",
            desc = "Sempre que um novo pet é cadastrado numa distância de até 5.0 km de você, a comunidade local dispara um gatilho de push inteligente! Um banner desce da tela em tempo real e o alerta é gravado na Central de Notificações."
        )

        GuideStep(
            step = "4",
            title = "Linha do Tempo de Sinais e Avistamentos",
            desc = "Vizinhos que virem o pet podem registrar comentários e pistas no chat de detalhes. Ao marcar a opção \"Eu vi este pet\", o vizinho insere seu próprio Pin de localização geográfica no satélite, desenhando um histórico claro de avistamentos para o dono organizar as buscas."
        )

        Text(
            text = "Dicas de Teste Rápido:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BulletPoint("Acesse a aba 'Mapa' para ver pins em tempo real em São Paulo / Avenida Paulista.")
                BulletPoint("Toque nos bairros 'Consolação', 'Jardins' ou 'Paraíso' para teletransportar sua localização.")
                BulletPoint("Experimente voltar na aba 'Feed' e regular a barra de 'Raio de busca': os pets sumirão ou aparecerão conforme a sua distância simulada atual.")
                BulletPoint("Vá até o Feed, clique em 'Reportar Pet', e registre o nome 'Bidu' ou similar perto da sua rua: acompanhe a notificação push simulada descendo da tela instantaneamente!")
            }
        }

        Button(
            onClick = onResetPresets,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Resetar Local de Teste para MASP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun GuideStep(
    step: String,
    title: String,
    desc: String,
    tinted: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (tinted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(step, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray, lineHeight = 16.sp)
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text, fontSize = 11.sp, color = Color.DarkGray, lineHeight = 14.sp)
    }
}
