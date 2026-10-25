package com.example.core.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: String = "local_player",
    val displayName: String = "Primal Hunter",
    val level: Int = 1,
    val currentXp: Int = 0,
    val requiredXp: Int = 100,
    val fossils: Int = 50,
    val crystals: Int = 15,
    val activeDinoId: String = "raptor",
    val activeGrowthStage: String = "JUVENILE",
    val currentBiome: String = "Verdant Valley",
    val sanctuaryLevel: Int = 1,
    val storyChapter: Int = 1,
    val lastDailyClaimDay: Long = 0L,
    val consecutiveDailyClaims: Int = 0,
    val totalPlayTimeSeconds: Long = 0L,
    val tutorialCompleted: Boolean = false,
    val sfxVolume: Float = 0.8f,
    val musicVolume: Float = 0.6f,
    val screenShakeEnabled: Boolean = true,
    val leftHandedControls: Boolean = false
)

@Entity(tableName = "inventory_items")
data class InventoryEntity(
    @PrimaryKey val itemId: String,
    val quantity: Int
)

@Entity(tableName = "dino_progress")
data class DinoProgressEntity(
    @PrimaryKey val speciesId: String,
    val isUnlocked: Boolean,
    val growthStage: String,
    val level: Int = 1,
    val xp: Int = 0,
    val isFavorite: Boolean = false
)

@Entity(tableName = "sanctuary_structures")
data class SanctuaryEntity(
    @PrimaryKey val id: String,
    val structureType: String,
    val gridX: Int,
    val gridY: Int,
    val level: Int = 1
)

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val currentProgress: Int,
    val isCompleted: Boolean,
    val isClaimed: Boolean
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val currentProgress: Int,
    val isCompleted: Boolean,
    val isClaimed: Boolean
)

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 'local_player'")
    fun getProfileFlow(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 'local_player'")
    suspend fun getProfile(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM inventory_items")
    fun getInventoryFlow(): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setInventoryItem(item: InventoryEntity)

    @Query("DELETE FROM inventory_items WHERE itemId = :itemId")
    suspend fun removeInventoryItem(itemId: String)

    @Query("SELECT * FROM dino_progress")
    fun getDinoProgressFlow(): Flow<List<DinoProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDinoProgress(progress: DinoProgressEntity)

    @Query("SELECT * FROM sanctuary_structures")
    fun getSanctuaryFlow(): Flow<List<SanctuaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSanctuaryStructure(structure: SanctuaryEntity)

    @Query("DELETE FROM sanctuary_structures WHERE id = :id")
    suspend fun deleteSanctuaryStructure(id: String)

    @Query("SELECT * FROM quests")
    fun getQuestsFlow(): Flow<List<QuestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQuest(quest: QuestEntity)

    @Query("SELECT * FROM achievements")
    fun getAchievementsFlow(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAchievement(achievement: AchievementEntity)

    @Query("DELETE FROM player_profile")
    suspend fun clearProfile()

    @Query("DELETE FROM inventory_items")
    suspend fun clearInventory()

    @Query("DELETE FROM dino_progress")
    suspend fun clearDinoProgress()

    @Query("DELETE FROM sanctuary_structures")
    suspend fun clearSanctuary()

    @Query("DELETE FROM quests")
    suspend fun clearQuests()

    @Query("DELETE FROM achievements")
    suspend fun clearAchievements()
}

@Database(
    entities = [
        PlayerProfileEntity::class,
        InventoryEntity::class,
        DinoProgressEntity::class,
        SanctuaryEntity::class,
        QuestEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dino_planet_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
