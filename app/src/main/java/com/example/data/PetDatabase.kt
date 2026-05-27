package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pet_alerts")
data class PetAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val petName: String,
    val petType: String, // "CACHORRO" or "GATO"
    val breed: String,
    val color: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String, // e.g. "Av. Paulista, 1000"
    val ownerName: String,
    val ownerPhone: String,
    val reportedTime: Long,
    val status: String, // "PERDIDO" or "ENCONTRADO"
    val avatarColorSeed: Int = 0, // For generating cute animal shapes/color profiles inCompose
    val reward: String = "" // e.g. "R$ 500" or empty
)

@Entity(tableName = "pet_comments")
data class PetComment(
    @PrimaryKey(autoGenerate = true) val commentId: Int = 0,
    val alertId: Int,
    val author: String,
    val message: String,
    val timestamp: Long,
    val latitude: Double? = null, // Sighting latitude if any
    val longitude: Double? = null, // Sighting longitude if any
    val isSighting: Boolean = false
)

@Entity(tableName = "user_notifications")
data class UserNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val timestamp: Long,
    val alertId: Int? = null,
    val isRead: Boolean = false
)

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_alerts ORDER BY reportedTime DESC")
    fun getAllAlerts(): Flow<List<PetAlert>>

    @Query("SELECT * FROM pet_alerts WHERE id = :id")
    suspend fun getAlertById(id: Int): PetAlert?

    @Query("SELECT * FROM pet_alerts WHERE status = :status ORDER BY reportedTime DESC")
    fun getAlertsByStatus(status: String): Flow<List<PetAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PetAlert): Long

    @Query("UPDATE pet_alerts SET status = :status WHERE id = :id")
    suspend fun updateAlertStatus(id: Int, status: String)

    @Query("DELETE FROM pet_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)

    // Comments
    @Query("SELECT * FROM pet_comments WHERE alertId = :alertId ORDER BY timestamp ASC")
    fun getCommentsForAlert(alertId: Int): Flow<List<PetComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: PetComment): Long

    // Notifications
    @Query("SELECT * FROM user_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<UserNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: UserNotification)

    @Query("UPDATE user_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("UPDATE user_notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()
    
    @Query("DELETE FROM user_notifications")
    suspend fun clearAllNotifications()
}

@Database(
    entities = [PetAlert::class, PetComment::class, UserNotification::class],
    version = 1,
    exportSchema = false
)
abstract class PetDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: PetDatabase? = null

        fun getDatabase(context: android.content.Context): PetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    PetDatabase::class.java,
                    "pet_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
