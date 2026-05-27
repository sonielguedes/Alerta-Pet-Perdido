package com.example.repository

import android.content.Context
import android.util.Log
import com.example.data.PetAlert
import com.example.data.PetComment
import com.example.data.PetDao
import com.example.data.PetDatabase
import com.example.data.UserNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.*

class PetRepository(private val context: Context) {
    private val petDao: PetDao = PetDatabase.getDatabase(context).petDao()

    val allAlerts: Flow<List<PetAlert>> = petDao.getAllAlerts()
    val allNotifications: Flow<List<UserNotification>> = petDao.getAllNotifications()

    fun getCommentsForAlert(alertId: Int): Flow<List<PetComment>> = petDao.getCommentsForAlert(alertId)

    suspend fun getAlertById(id: Int): PetAlert? = petDao.getAlertById(id)

    suspend fun insertAlert(alert: PetAlert, userLat: Double, userLon: Double): Long {
        val id = petDao.insertAlert(alert)
        val savedAlert = alert.copy(id = id.toInt())
        
        // Trigger simulated push notification if pet is registered nearby
        checkAndTriggerPushNotification(savedAlert, userLat, userLon)
        return id
    }

    suspend fun updateAlertStatus(id: Int, status: String) {
        petDao.updateAlertStatus(id, status)
        
        // Notify community of status update (e.g. "Pipoca foi encontrado!")
        val alert = petDao.getAlertById(id)
        if (alert != null) {
            val statusText = if (status == "ENCONTRADO") "foi ENCONTRADO!" else "está PERDIDO novamente."
            insertNotification(
                UserNotification(
                    title = "Pet Atualizado: ${alert.petName}",
                    body = "O pet ${alert.petName} (${alert.breed}) ${statusText} Comunidade em festa!",
                    timestamp = System.currentTimeMillis(),
                    alertId = alert.id
                )
            )
        }
    }

    suspend fun deleteAlert(id: Int) {
        petDao.deleteAlertById(id)
    }

    suspend fun insertComment(comment: PetComment) {
        petDao.insertComment(comment)
        
        // If it is a sighting report, notify the owner
        if (comment.isSighting) {
            val alert = petDao.getAlertById(comment.alertId)
            if (alert != null) {
                insertNotification(
                    UserNotification(
                        title = "Novo avistamento de ${alert.petName}!",
                        body = "${comment.author} relatou ver o pet: \"${comment.message}\"",
                        timestamp = System.currentTimeMillis(),
                        alertId = alert.id
                    )
                )
            }
        }
    }

    suspend fun insertNotification(notification: UserNotification) {
        petDao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: Int) {
        petDao.markNotificationAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        petDao.markAllNotificationsAsRead()
    }

    suspend fun clearAllNotifications() {
        petDao.clearAllNotifications()
    }

    // Coordinates distance calculation using Haversine Formula
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private suspend fun checkAndTriggerPushNotification(alert: PetAlert, userLat: Double, userLon: Double) {
        val distance = calculateDistance(userLat, userLon, alert.latitude, alert.longitude)
        // If within 5.0 km, consider it in the user's community local area
        if (distance <= 5.0) {
            val typeStr = if (alert.petType == "CACHORRO") "Cachorro" else "Gato"
            val statusStr = if (alert.status == "PERDIDO") "perdido" else "encontrado"
            val body = "${typeStr} ${alert.petName} (${alert.breed}) foi reportado como ${statusStr} a apenas ${String.format("%.1f", distance)} km de sua localização atual (${alert.address})."
            
            insertNotification(
                UserNotification(
                    title = "ALERTA: Pet ${alert.status} nas Proximidades!",
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    alertId = alert.id
                )
            )
        }
    }

    suspend fun seedDatabase() {
        val existing = petDao.getAllAlerts().first()
        if (existing.isNotEmpty()) return

        Log.d("PetRepository", "Seeding initial local pet data...")
        
        // Baseline location is Av. Paulista - lat: -23.5615, lon: -46.6560
        val baseTime = System.currentTimeMillis()
        
        val seedAlerts = listOf(
            PetAlert(
                petName = "Pipoca",
                petType = "CACHORRO",
                breed = "Poodle",
                color = "Branco",
                description = "Sumiu perto do MASP. É dócil mas está assustada. Coleira vermelha.",
                latitude = -23.5600,
                longitude = -46.6570,
                address = "Av. Paulista, 1500 - Cerqueira César",
                ownerName = "Bruno Silva",
                ownerPhone = "11 98888-7777",
                reportedTime = baseTime - 4 * 3600000, // 4 hours ago
                status = "PERDIDO",
                avatarColorSeed = 1,
                reward = "R$ 400"
            ),
            PetAlert(
                petName = "Garfield",
                petType = "GATO",
                breed = "Persa",
                color = "Laranja Listrado",
                description = "Fugiu do apartamento no 4º andar. É bem gordinho e assustado.",
                latitude = -23.5630,
                longitude = -46.6540,
                address = "Alameda Santos, 900 - Jardins",
                ownerName = "Ana Costa",
                ownerPhone = "11 97777-6666",
                reportedTime = baseTime - 12 * 3600000, // 12 hours ago
                status = "PERDIDO",
                avatarColorSeed = 2,
                reward = ""
            ),
            PetAlert(
                petName = "Baleia",
                petType = "CACHORRO",
                breed = "Vira-Lata (SRD)",
                color = "Preto e Caramelo",
                description = "Muito brincalhão. Tem manchas beges nas patas e responde pelo nome.",
                latitude = -23.5580,
                longitude = -46.6620,
                address = "Rua Augusta, 1200 - Consolação",
                ownerName = "Jefferson Lima",
                ownerPhone = "11 95555-4444",
                reportedTime = baseTime - 24 * 3600000, // 1 day ago
                status = "PERDIDO",
                avatarColorSeed = 3,
                reward = "Gratidão e recompensa"
            ),
            PetAlert(
                petName = "Bolinha",
                petType = "GATO",
                breed = "Vira-Lata (SRD)",
                color = "Preto/Branco (Tuxedo)",
                description = "Gatinha encontrada miando perto dos arbustos do metrô, muito sociável e bem cuidada. Abastecendo com comidinha temporariamente.",
                latitude = -23.5650,
                longitude = -46.6510,
                address = "Praça Oswaldo Cruz - Paraíso",
                ownerName = "Renata Melo",
                ownerPhone = "11 96666-5555",
                reportedTime = baseTime - 36 * 3600000, // 1.5 days ago
                status = "ENCONTRADO",
                avatarColorSeed = 4,
                reward = ""
            )
        )

        for (alert in seedAlerts) {
            val alertId = petDao.insertAlert(alert)
            
            // Seed sample comments
            if (alert.petName == "Pipoca") {
                petDao.insertComment(
                    PetComment(
                        alertId = alertId.toInt(),
                        author = "Carlos Pereira",
                        message = "Acho que vi um cachorrinho igual descendo a Alameda Casa Branca!",
                        timestamp = baseTime - 2 * 3600000,
                        latitude = -23.5620,
                        longitude = -46.6590,
                        isSighting = true
                    )
                )
                petDao.insertComment(
                    PetComment(
                        alertId = alertId.toInt(),
                        author = "Maria Helena",
                        message = "Espero que encontre logo! Já compartilhei no grupo do condomínio.",
                        timestamp = baseTime - 3600000,
                        latitude = null,
                        longitude = null,
                        isSighting = false
                    )
                )
            } else if (alert.petName == "Bolinha") {
                petDao.insertComment(
                    PetComment(
                        alertId = alertId.toInt(),
                        author = "Guilherme Santos",
                        message = "Ela sumiu da Rua Treze de Maio ontem de manhã. Parece muito com a bolinha da vizinha! Mandei foto para ela.",
                        timestamp = baseTime - 18 * 3600000,
                        latitude = null,
                        longitude = null,
                        isSighting = false
                    )
                )
            }
        }

        // Seed some initial welcome local push notification log
        petDao.insertNotification(
            UserNotification(
                title = "Bem-vindo ao Alerta Pet!",
                body = "Sua geolocalização está ativa para Avenida Paulista, São Paulo. Você receberá alertas de pets perdidos num raio de até 5km.",
                timestamp = baseTime - 48 * 3600000,
                isRead = true
            )
        )
    }
}
