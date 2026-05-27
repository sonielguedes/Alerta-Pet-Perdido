package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PetAlert
import com.example.data.PetComment
import com.example.data.UserNotification
import com.example.repository.PetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class PetScreen {
    FEED,
    MAP,
    DETAILS,
    ADD_ALERT,
    NOTIFICATIONS,
    ABOUT
}

class PetAlertViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PetRepository(application)

    // Current screen navigation state
    private val _currentScreen = MutableStateFlow(PetScreen.FEED)
    val currentScreen: StateFlow<PetScreen> = _currentScreen.asStateFlow()

    private val _selectedAlertId = MutableStateFlow<Int?>(null)
    val selectedAlertId: StateFlow<Int?> = _selectedAlertId.asStateFlow()

    // Mock geolocation coordinates of the user (Defaults to MASP Paulista -23.5615, -46.6560)
    private val _userLatitude = MutableStateFlow(-23.5615)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow(-46.6560)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    private val _userAddressName = MutableStateFlow("Av. Paulista, 1500 (Configuração Padrão)")
    val userAddressName: StateFlow<String> = _userAddressName.asStateFlow()

    // Filters
    private val _filterType = MutableStateFlow<String?>("TODOS") // TODOS, CACHORRO, GATO
    val filterType: StateFlow<String?> = _filterType.asStateFlow()

    private val _filterStatus = MutableStateFlow<String?>("TODOS") // TODOS, PERDIDO, ENCONTRADO
    val filterStatus: StateFlow<String?> = _filterStatus.asStateFlow()

    private val _maxDistanceKm = MutableStateFlow(5.0) // Radius slider (1km to 15km)
    val maxDistanceKm: StateFlow<Double> = _maxDistanceKm.asStateFlow()

    // Combined Alerts List based on filter constraints and user location
    val alerts: StateFlow<List<PetAlert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAlerts: StateFlow<List<PetAlert>> = combine(
        repository.allAlerts,
        _userLatitude,
        _userLongitude,
        _filterType,
        _filterStatus,
        _maxDistanceKm
    ) { args: Array<Any?> ->
        val allAlerts = args[0] as List<PetAlert>
        val userLat = args[1] as Double
        val userLon = args[2] as Double
        val type = args[3] as String?
        val status = args[4] as String?
        val maxDist = args[5] as Double

        allAlerts.filter { alert ->
            val matchesType = type == "TODOS" || alert.petType == type
            val matchesStatus = status == "TODOS" || alert.status == status
            
            val distance = repository.calculateDistance(userLat, userLon, alert.latitude, alert.longitude)
            val matchesDistance = distance <= maxDist

            matchesType && matchesStatus && matchesDistance
        }.sortedBy { alert ->
            repository.calculateDistance(userLat, userLon, alert.latitude, alert.longitude)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications Feed
    val notifications: StateFlow<List<UserNotification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.allNotifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Current selected pet detail with its comments
    val selectedAlert: StateFlow<PetAlert?> = _selectedAlertId
        .flatMapLatest { id ->
            if (id != null) {
                flow<PetAlert?> { emit(repository.getAlertById(id)) }
            } else {
                flowOf<PetAlert?>(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedAlertComments: StateFlow<List<PetComment>> = _selectedAlertId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getCommentsForAlert(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedDatabase()
        }
    }

    // Navigation and screen actions
    fun navigateTo(screen: PetScreen, alertId: Int? = null) {
        _selectedAlertId.value = alertId
        _currentScreen.value = screen
    }

    // Set User GPS Location Simulated or Real
    fun updateUserLocation(lat: Double, lon: Double, addressName: String) {
        _userLatitude.value = lat
        _userLongitude.value = lon
        _userAddressName.value = addressName

        // Trigger a friendly popup simulated push notification when teleporting to alert user
        viewModelScope.launch {
            repository.insertNotification(
                UserNotification(
                    title = "Geolocalização Atualizada!",
                    body = "Sua coordenada simulada é agora: $addressName ($lat, $lon). Buscando animais nessa vizinhança.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Set filter configuration
    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun setMaxDistance(distance: Double) {
        _maxDistanceKm.value = distance
    }

    // Insert new pet alert from user input
    fun createPetAlert(
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
    ) {
        viewModelScope.launch {
            val alert = PetAlert(
                petName = name.ifEmpty { "Sem nome" },
                petType = type,
                breed = breed.ifEmpty { "Mestiço / SRD" },
                color = color.ifEmpty { "Não informada" },
                description = description.ifEmpty { "Sem descrição." },
                latitude = latitude,
                longitude = longitude,
                address = address,
                ownerName = ownerName.ifEmpty { "Morador Local" },
                ownerPhone = ownerPhone.ifEmpty { "Disponível no app" },
                reportedTime = System.currentTimeMillis(),
                status = "PERDIDO",
                reward = reward,
                avatarColorSeed = avatarSeed
            )
            repository.insertAlert(alert, _userLatitude.value, _userLongitude.value)
            _currentScreen.value = PetScreen.FEED
        }
    }

    // Add comment to an alert
    fun addComment(alertId: Int, author: String, message: String, isSighting: Boolean = false, sightingLat: Double? = null, sightingLon: Double? = null) {
        viewModelScope.launch {
            val comment = PetComment(
                alertId = alertId,
                author = author.ifEmpty { "Vizinho Útil" },
                message = message,
                timestamp = System.currentTimeMillis(),
                latitude = sightingLat,
                longitude = sightingLon,
                isSighting = isSighting
            )
            repository.insertComment(comment)
        }
    }

    // Update alert status (solved or lost)
    fun toggleAlertStatus(alertId: Int, currentStatus: String) {
        viewModelScope.launch {
            val nextStatus = if (currentStatus == "PERDIDO") "ENCONTRADO" else "PERDIDO"
            repository.updateAlertStatus(alertId, nextStatus)
        }
    }

    // Delete alert list item (owner removes or spam cleanup)
    fun deleteAlert(alertId: Int) {
        viewModelScope.launch {
            repository.deleteAlert(alertId)
            if (_selectedAlertId.value == alertId) {
                _selectedAlertId.value = null
                _currentScreen.value = PetScreen.FEED
            }
        }
    }

    // Helper to calculate distance from current simulated user position
    fun getDistanceFromUser(alertLat: Double, alertLon: Double): Double {
        return repository.calculateDistance(_userLatitude.value, _userLongitude.value, alertLat, alertLon)
    }

    // Clear notifications log
    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }
}
