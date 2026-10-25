package com.example.core.repository

import android.content.Context
import com.example.core.audio.GameAudioEngine
import com.example.core.database.*
import com.example.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class GameRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.gameDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _profile = MutableStateFlow(PlayerProfileEntity())
    val profile: StateFlow<PlayerProfileEntity> = _profile.asStateFlow()

    private val _inventory = MutableStateFlow<Map<String, Int>>(emptyMap())
    val inventory: StateFlow<Map<String, Int>> = _inventory.asStateFlow()

    private val _dinoProgress = MutableStateFlow<Map<String, DinoProgressEntity>>(emptyMap())
    val dinoProgress: StateFlow<Map<String, DinoProgressEntity>> = _dinoProgress.asStateFlow()

    private val _sanctuary = MutableStateFlow<List<SanctuaryEntity>>(emptyList())
    val sanctuary: StateFlow<List<SanctuaryEntity>> = _sanctuary.asStateFlow()

    private val _quests = MutableStateFlow<List<Quest>>(emptyList())
    val quests: StateFlow<List<Quest>> = _quests.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _activeNotification = MutableStateFlow<String?>(null)
    val activeNotification: StateFlow<String?> = _activeNotification.asStateFlow()

    init {
        scope.launch {
            loadOrInitGame()
            observeDatabase()
        }
    }

    fun dismissNotification() {
        _activeNotification.value = null
    }

    private suspend fun loadOrInitGame() {
        var existingProfile = dao.getProfile()
        if (existingProfile == null) {
            existingProfile = PlayerProfileEntity()
            dao.saveProfile(existingProfile)

            // Initial starter inventory: 8 berries, 4 wood, 2 stone
            dao.setInventoryItem(InventoryEntity("berries", 8))
            dao.setInventoryItem(InventoryEntity("wood", 6))
            dao.setInventoryItem(InventoryEntity("stone", 4))
            dao.setInventoryItem(InventoryEntity("clean_water", 2))

            // Starter species: Velociraptor unlocked
            PrehistoricDatabase.dinosaurs.forEach { species ->
                dao.saveDinoProgress(
                    DinoProgressEntity(
                        speciesId = species.id,
                        isUnlocked = species.unlockedByDefault,
                        growthStage = if (species.unlockedByDefault) GrowthStage.JUVENILE.name else GrowthStage.EGG.name,
                        level = 1,
                        xp = 0,
                        isFavorite = species.id == "raptor"
                    )
                )
            }

            // Initial sanctuary: 1 basic shelter
            dao.saveSanctuaryStructure(
                SanctuaryEntity(
                    id = UUID.randomUUID().toString(),
                    structureType = StructureType.BASIC_SHELTER.name,
                    gridX = 2,
                    gridY = 2,
                    level = 1
                )
            )

            // Initial Quests
            PrehistoricDatabase.initialQuests.forEach { q ->
                dao.saveQuest(QuestEntity(q.id, q.currentProgress, q.isCompleted, q.isClaimed))
            }

            // Initial Achievements
            PrehistoricDatabase.initialAchievements.forEach { ach ->
                dao.saveAchievement(AchievementEntity(ach.id, ach.currentProgress, ach.isCompleted, ach.isClaimed))
            }
        }

        // Apply audio settings from loaded profile
        GameAudioEngine.sfxVolume = existingProfile.sfxVolume
        GameAudioEngine.musicVolume = existingProfile.musicVolume
    }

    private fun observeDatabase() {
        scope.launch {
            dao.getProfileFlow().collect { p ->
                if (p != null) {
                    _profile.value = p
                }
            }
        }
        scope.launch {
            dao.getInventoryFlow().collect { items ->
                val map = mutableMapOf<String, Int>()
                items.forEach { map[it.itemId] = it.quantity }
                _inventory.value = map
            }
        }
        scope.launch {
            dao.getDinoProgressFlow().collect { progressList ->
                val map = mutableMapOf<String, DinoProgressEntity>()
                progressList.forEach { map[it.speciesId] = it }
                _dinoProgress.value = map
            }
        }
        scope.launch {
            dao.getSanctuaryFlow().collect { structures ->
                _sanctuary.value = structures
            }
        }
        scope.launch {
            dao.getQuestsFlow().collect { questEntities ->
                val entityMap = questEntities.associateBy { it.id }
                _quests.value = PrehistoricDatabase.initialQuests.map { q ->
                    val saved = entityMap[q.id]
                    if (saved != null) {
                        q.copy(
                            currentProgress = saved.currentProgress,
                            isCompleted = saved.isCompleted,
                            isClaimed = saved.isClaimed
                        )
                    } else q
                }
            }
        }
        scope.launch {
            dao.getAchievementsFlow().collect { achEntities ->
                val entityMap = achEntities.associateBy { it.id }
                _achievements.value = PrehistoricDatabase.initialAchievements.map { a ->
                    val saved = entityMap[a.id]
                    if (saved != null) {
                        a.copy(
                            currentProgress = saved.currentProgress,
                            isCompleted = saved.isCompleted,
                            isClaimed = saved.isClaimed
                        )
                    } else a
                }
            }
        }
    }

    fun addItem(itemId: String, count: Int = 1) {
        scope.launch {
            val current = _inventory.value[itemId] ?: 0
            val next = (current + count).coerceAtLeast(0)
            if (next == 0) {
                dao.removeInventoryItem(itemId)
            } else {
                dao.setInventoryItem(InventoryEntity(itemId, next))
            }

            // Update quests related to item collecting
            if (itemId == "berries" && count > 0) {
                incrementQuestProgress("quest_2", count)
                incrementAchievementProgress("ach_berries", count)
            } else if (itemId == "fossil_fragments" && count > 0) {
                incrementQuestProgress("quest_6", count)
                incrementAchievementProgress("ach_fossils", count)
            } else if (itemId.contains("egg") && count > 0) {
                incrementQuestProgress("quest_5", count)
            }
        }
    }

    fun consumeItem(itemId: String): ItemDefinition? {
        val itemDef = PrehistoricDatabase.items.find { it.id == itemId } ?: return null
        val current = _inventory.value[itemId] ?: 0
        if (current <= 0) return null

        scope.launch {
            val next = current - 1
            if (next <= 0) {
                dao.removeInventoryItem(itemId)
            } else {
                dao.setInventoryItem(InventoryEntity(itemId, next))
            }
            if (itemDef.category == ItemCategory.WATER) {
                GameAudioEngine.playDrink()
            } else {
                GameAudioEngine.playCollect()
            }
        }
        return itemDef
    }

    fun craftRecipe(recipe: CraftingRecipe): Boolean {
        val inv = _inventory.value
        // Check if all materials available
        for ((matId, needed) in recipe.requiredMaterials) {
            val available = inv[matId] ?: 0
            if (available < needed) return false
        }

        scope.launch {
            // Deduct ingredients
            for ((matId, needed) in recipe.requiredMaterials) {
                val current = inv[matId] ?: 0
                val next = current - needed
                if (next <= 0) {
                    dao.removeInventoryItem(matId)
                } else {
                    dao.setInventoryItem(InventoryEntity(matId, next))
                }
            }
            // Add output item
            val currentOut = _inventory.value[recipe.outputItemId] ?: 0
            dao.setInventoryItem(InventoryEntity(recipe.outputItemId, currentOut + recipe.outputCount))

            GameAudioEngine.playCollect()
            addXp(30)
            _activeNotification.value = "Crafted ${recipe.name}!"
        }
        return true
    }

    fun addXp(amount: Int) {
        scope.launch {
            val current = _profile.value
            var xp = current.currentXp + amount
            var level = current.level
            var reqXp = current.requiredXp
            var leveledUp = false

            while (xp >= reqXp) {
                xp -= reqXp
                level++
                reqXp = (reqXp * 1.35f).toInt()
                leveledUp = true
            }

            val updated = current.copy(
                level = level,
                currentXp = xp,
                requiredXp = reqXp,
                fossils = if (leveledUp) current.fossils + 50 else current.fossils,
                crystals = if (leveledUp) current.crystals + 10 else current.crystals
            )
            dao.saveProfile(updated)

            if (leveledUp) {
                GameAudioEngine.playLevelUp()
                _activeNotification.value = "Dinosaur Leveled Up! Reached Level $level!"
            }
        }
    }

    fun addCurrencies(fossils: Int, crystals: Int) {
        scope.launch {
            val current = _profile.value
            dao.saveProfile(
                current.copy(
                    fossils = current.fossils + fossils,
                    crystals = current.crystals + crystals
                )
            )
        }
    }

    fun setActiveDinosaur(speciesId: String) {
        scope.launch {
            val progress = _dinoProgress.value[speciesId]
            if (progress?.isUnlocked == true) {
                dao.saveProfile(_profile.value.copy(activeDinoId = speciesId))
                GameAudioEngine.playRoar()
            }
        }
    }

    fun evolveDinosaur(speciesId: String) {
        scope.launch {
            val progress = _dinoProgress.value[speciesId] ?: return@launch
            val currentStage = GrowthStage.valueOf(progress.growthStage)
            val nextStageOrdinal = currentStage.ordinal + 1
            if (nextStageOrdinal < GrowthStage.entries.size) {
                val nextStage = GrowthStage.entries[nextStageOrdinal]
                val cost = nextStage.requiredXp / 5
                val prof = _profile.value
                if (prof.fossils >= cost) {
                    dao.saveProfile(prof.copy(fossils = prof.fossils - cost))
                    dao.saveDinoProgress(progress.copy(growthStage = nextStage.name, level = progress.level + 1))
                    GameAudioEngine.playHatch()
                    _activeNotification.value = "Evolved into ${nextStage.displayName}!"
                }
            }
        }
    }

    fun unlockSpecies(speciesId: String) {
        scope.launch {
            val existing = _dinoProgress.value[speciesId]
            dao.saveDinoProgress(
                existing?.copy(isUnlocked = true) ?: DinoProgressEntity(
                    speciesId = speciesId,
                    isUnlocked = true,
                    growthStage = GrowthStage.JUVENILE.name
                )
            )
            GameAudioEngine.playHatch()
            _activeNotification.value = "Discovered new species!"
            incrementAchievementProgress("ach_dino_master", 1)
        }
    }

    fun placeStructure(type: StructureType, gridX: Int, gridY: Int): Boolean {
        val inv = _inventory.value
        for ((mat, count) in type.baseCost) {
            if ((inv[mat] ?: 0) < count) return false
        }

        scope.launch {
            for ((mat, count) in type.baseCost) {
                val next = (inv[mat] ?: 0) - count
                if (next <= 0) dao.removeInventoryItem(mat)
                else dao.setInventoryItem(InventoryEntity(mat, next))
            }
            dao.saveSanctuaryStructure(
                SanctuaryEntity(
                    id = UUID.randomUUID().toString(),
                    structureType = type.name,
                    gridX = gridX,
                    gridY = gridY,
                    level = 1
                )
            )
            GameAudioEngine.playCollect()
            addXp(60)
            incrementQuestProgress("quest_4", 1)
            incrementAchievementProgress("ach_sanctuary", 1)
            _activeNotification.value = "Built ${type.displayName} in Sanctuary!"
        }
        return true
    }

    fun removeStructure(structureId: String) {
        scope.launch {
            dao.deleteSanctuaryStructure(structureId)
            GameAudioEngine.playButtonClick()
        }
    }

    fun hatchEgg(eggItemId: String): DinosaurSpecies {
        val availableLocked = PrehistoricDatabase.dinosaurs.filter {
            val prog = _dinoProgress.value[it.id]
            prog == null || !prog.isUnlocked
        }
        val targetSpecies = availableLocked.randomOrNull() ?: PrehistoricDatabase.dinosaurs.random()

        scope.launch {
            consumeItem(eggItemId)
            unlockSpecies(targetSpecies.id)
            incrementQuestProgress("quest_8", 1)
            incrementAchievementProgress("ach_eggs", 1)
            addXp(120)
        }
        return targetSpecies
    }

    fun incrementQuestProgress(questId: String, amount: Int) {
        scope.launch {
            val quest = _quests.value.find { it.id == questId } ?: return@launch
            if (quest.isCompleted) return@launch
            val nextProg = quest.currentProgress + amount
            val completed = nextProg >= quest.targetCount
            dao.saveQuest(
                QuestEntity(
                    id = quest.id,
                    currentProgress = nextProg,
                    isCompleted = completed,
                    isClaimed = quest.isClaimed
                )
            )
            if (completed && !quest.isCompleted) {
                GameAudioEngine.playLevelUp()
                _activeNotification.value = "Quest Complete: ${quest.title}!"
            }
        }
    }

    fun claimQuestReward(questId: String) {
        scope.launch {
            val quest = _quests.value.find { it.id == questId } ?: return@launch
            if (!quest.isCompleted || quest.isClaimed) return@launch

            dao.saveQuest(
                QuestEntity(
                    id = quest.id,
                    currentProgress = quest.currentProgress,
                    isCompleted = true,
                    isClaimed = true
                )
            )

            addXp(quest.xpReward)
            addCurrencies(quest.fossilReward, quest.crystalReward)
            quest.itemRewardId?.let { itemId ->
                if (quest.itemRewardCount > 0) {
                    addItem(itemId, quest.itemRewardCount)
                }
            }
            GameAudioEngine.playCollect()
            _activeNotification.value = "Claimed rewards for ${quest.title}!"
        }
    }

    fun incrementAchievementProgress(achId: String, amount: Int) {
        scope.launch {
            val ach = _achievements.value.find { it.id == achId } ?: return@launch
            if (ach.isCompleted) return@launch
            val nextProg = ach.currentProgress + amount
            val completed = nextProg >= ach.targetProgress
            dao.saveAchievement(
                AchievementEntity(
                    id = ach.id,
                    currentProgress = nextProg,
                    isCompleted = completed,
                    isClaimed = ach.isClaimed
                )
            )
            if (completed && !ach.isCompleted) {
                GameAudioEngine.playLevelUp()
                _activeNotification.value = "Achievement Unlocked: ${ach.title}!"
            }
        }
    }

    fun claimAchievementReward(achId: String) {
        scope.launch {
            val ach = _achievements.value.find { it.id == achId } ?: return@launch
            if (!ach.isCompleted || ach.isClaimed) return@launch

            dao.saveAchievement(
                AchievementEntity(
                    id = ach.id,
                    currentProgress = ach.currentProgress,
                    isCompleted = true,
                    isClaimed = true
                )
            )

            addCurrencies(0, ach.crystalReward)
            GameAudioEngine.playCollect()
            _activeNotification.value = "Claimed ${ach.crystalReward} Crystals for ${ach.title}!"
        }
    }

    fun claimDailyReward(): Boolean {
        val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val profile = _profile.value
        if (profile.lastDailyClaimDay == currentDay) return false

        scope.launch {
            val consecutive = if (profile.lastDailyClaimDay == currentDay - 1) {
                profile.consecutiveDailyClaims + 1
            } else {
                1
            }
            // Reward cycle
            val bonusFossils = 30 + consecutive * 10
            val bonusCrystals = if (consecutive % 7 == 0) 25 else 5

            dao.saveProfile(
                profile.copy(
                    lastDailyClaimDay = currentDay,
                    consecutiveDailyClaims = consecutive,
                    fossils = profile.fossils + bonusFossils,
                    crystals = profile.crystals + bonusCrystals
                )
            )
            // Grant random materials
            addItem("berries", 5)
            addItem("wood", 3)
            GameAudioEngine.playLevelUp()
            _activeNotification.value = "Claimed Day $consecutive Daily Survival Pack!"
        }
        return true
    }

    fun updateSettings(
        sfx: Float,
        music: Float,
        screenShake: Boolean,
        leftHanded: Boolean
    ) {
        scope.launch {
            GameAudioEngine.sfxVolume = sfx
            GameAudioEngine.musicVolume = music
            val current = _profile.value
            dao.saveProfile(
                current.copy(
                    sfxVolume = sfx,
                    musicVolume = music,
                    screenShakeEnabled = screenShake,
                    leftHandedControls = leftHanded
                )
            )
        }
    }

    fun resetGameProgress() {
        scope.launch {
            dao.clearProfile()
            dao.clearInventory()
            dao.clearDinoProgress()
            dao.clearSanctuary()
            dao.clearQuests()
            dao.clearAchievements()
            loadOrInitGame()
            _activeNotification.value = "Progress reset to Day 1."
        }
    }
}
