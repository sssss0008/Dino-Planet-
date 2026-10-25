package com.example.core.model

enum class DietType {
    CARNIVORE, HERBIVORE, OMNIVORE
}

enum class Rarity {
    COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
}

enum class GrowthStage(
    val displayName: String,
    val scaleMultiplier: Float,
    val healthMultiplier: Float,
    val damageMultiplier: Float,
    val staminaMultiplier: Float,
    val requiredXp: Int
) {
    EGG("Egg", 0.4f, 0.2f, 0.0f, 0.2f, 0),
    HATCHLING("Hatchling", 0.6f, 0.5f, 0.5f, 0.7f, 100),
    JUVENILE("Juvenile", 0.8f, 0.75f, 0.75f, 0.9f, 300),
    ADOLESCENT("Adolescent", 1.0f, 1.0f, 1.0f, 1.0f, 700),
    ADULT("Adult", 1.25f, 1.35f, 1.35f, 1.2f, 1500),
    ELDER("Elder Legendary", 1.5f, 1.75f, 1.75f, 1.5f, 3000)
}

data class DinosaurSpecies(
    val id: String,
    val name: String,
    val scientificName: String,
    val diet: DietType,
    val rarity: Rarity,
    val habitat: String,
    val description: String,
    val favoriteFood: String,
    val baseHealth: Float,
    val baseSpeed: Float,
    val baseDamage: Float,
    val baseDefense: Float,
    val baseStamina: Float,
    val specialAbility: String,
    val abilityDescription: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val unlockedByDefault: Boolean = false
)

enum class ItemCategory {
    FOOD, WATER, CRAFTING, FOSSIL, EGG, MEDICINE, ARTIFACT
}

data class ItemDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: ItemCategory,
    val rarity: Rarity,
    val stackLimit: Int = 99,
    val healAmount: Float = 0f,
    val hungerAmount: Float = 0f,
    val hydrationAmount: Float = 0f,
    val staminaAmount: Float = 0f,
    val sellValue: Int = 5
)

data class InventoryItem(
    val itemId: String,
    val quantity: Int
)

data class CraftingRecipe(
    val id: String,
    val name: String,
    val description: String,
    val outputItemId: String,
    val outputCount: Int = 1,
    val requiredMaterials: Map<String, Int>,
    val unlockedAtLevel: Int = 1,
    val category: String = "Survival"
)

enum class StructureType(
    val displayName: String,
    val description: String,
    val iconName: String,
    val maxLevel: Int = 3,
    val baseCost: Map<String, Int>
) {
    BASIC_SHELTER(
        "Leaf Shelter",
        "Safe haven to rest, recover health, and save your adventure.",
        "shelter",
        3,
        mapOf("wood" to 15, "fiber" to 10, "stone" to 5)
    ),
    INCUBATION_NEST(
        "Incubation Nest",
        "Warms rescued prehistoric eggs until they crack and hatch.",
        "nest",
        3,
        mapOf("fiber" to 20, "feathers" to 8, "wood" to 10)
    ),
    CAMPFIRE(
        "Stone Hearth",
        "Provides warmth at night, repels predators, and cooks herbs.",
        "fire",
        2,
        mapOf("wood" to 20, "stone" to 15)
    ),
    STORAGE_BASKET(
        "Supply Cache",
        "Stores extra resources and prehistoric artifacts safely.",
        "storage",
        3,
        mapOf("wood" to 25, "fiber" to 15)
    ),
    WATER_BASIN(
        "Rain Basin",
        "Collects clean drinking water during storms and morning mist.",
        "water",
        2,
        mapOf("stone" to 30, "ancient_amber" to 2)
    ),
    BONE_BARRICADE(
        "Bone Barricade",
        "Fortifies sanctuary perimeter against roaming carnivores.",
        "fence",
        2,
        mapOf("bones" to 20, "wood" to 15)
    ),
    FOSSIL_ALTAR(
        "Ancient Totem",
        "Channeled fossil relic that boosts player experience gain by 15%.",
        "altar",
        3,
        mapOf("fossil_fragments" to 30, "crystals" to 5, "stone" to 25)
    )
}

data class SanctuaryBuilding(
    val id: String,
    val type: StructureType,
    val gridX: Int,
    val gridY: Int,
    val level: Int = 1
)

enum class QuestCategory {
    STORY, SURVIVAL, EXPLORATION, RESCUE, CRAFTING, SANCTUARY, DAILY
}

data class Quest(
    val id: String,
    val chapter: Int,
    val title: String,
    val description: String,
    val category: QuestCategory,
    val targetCount: Int,
    val currentProgress: Int = 0,
    val xpReward: Int,
    val fossilReward: Int,
    val crystalReward: Int = 0,
    val itemRewardId: String? = null,
    val itemRewardCount: Int = 0,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val tier: Rarity,
    val currentProgress: Int = 0,
    val targetProgress: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val crystalReward: Int = 10
)

data class PlayerVitals(
    val health: Float = 100f,
    val maxHealth: Float = 100f,
    val hunger: Float = 100f,
    val maxHunger: Float = 100f,
    val hydration: Float = 100f,
    val maxHydration: Float = 100f,
    val stamina: Float = 100f,
    val maxStamina: Float = 100f,
    val isInjured: Boolean = false
)

enum class WeatherType(val displayName: String, val hazardWarning: String?) {
    CLEAR("Clear Sky", null),
    CLOUDY("Overcast", null),
    RAIN("Rainfall", "Water replenished"),
    THUNDERSTORM("Thunderstorm", "High winds & low visibility"),
    HEATWAVE("Solar Heatwave", "Hydration drains 50% faster!"),
    VOLCANIC_ASH("Volcanic Ash Fall", "Danger: Volcanic rumble approaching!")
}

enum class DayPhase(val displayName: String, val ambientBrightness: Float) {
    DAWN("Dawn", 0.65f),
    MORNING("Morning", 0.9f),
    NOON("Noon", 1.0f),
    SUNSET("Sunset", 0.7f),
    NIGHT("Night", 0.35f)
}
