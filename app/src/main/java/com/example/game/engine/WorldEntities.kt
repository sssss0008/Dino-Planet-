package com.example.game.engine

import kotlin.math.*

enum class EntityType {
    PREY_GALLIMIMUS,
    NEUTRAL_TRICERATOPS,
    HOSTILE_ALLOSAURUS
}

data class Wildlife(
    val id: String,
    val type: EntityType,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var health: Float = 100f,
    var maxHealth: Float = 100f,
    var state: String = "IDLE", // IDLE, ROAM, CHASE, FLEE, ATTACK
    var stateTimer: Float = 2f,
    var facingRight: Boolean = true,
    val speed: Float = 3.5f,
    val detectionRadius: Float = 220f,
    val attackRadius: Float = 55f,
    var attackCooldown: Float = 0f
)

enum class ResourceNodeType(
    val displayName: String,
    val itemId: String,
    val harvestCount: Int,
    val respawnSeconds: Float,
    val actionText: String
) {
    BERRY_BUSH("Berry Bush", "berries", 3, 20f, "Pick Berries"),
    SPRING_POOL("Crystal Spring", "clean_water", 1, 10f, "Drink & Fill Water"),
    PETRIFIED_LOG("Ancient Log", "wood", 3, 25f, "Gather Wood"),
    BASALT_ROCK("Basalt Deposit", "stone", 2, 25f, "Quarry Stone"),
    FOSSIL_MOUND("Fossil Mound", "fossil_fragments", 2, 30f, "Excavate Fossil"),
    HIDDEN_NEST("Wild Nest", "common_egg", 1, 60f, "Rescue Egg"),
    SHELTER_SITE("Leaf Shelter", "rest", 0, 0f, "Rest in Shelter")
}

data class ResourceNode(
    val id: String,
    val type: ResourceNodeType,
    val x: Float,
    val y: Float,
    val radius: Float = 48f,
    var isDepleted: Boolean = false,
    var respawnTimer: Float = 0f
)

data class GameParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var life: Float,
    var maxLife: Float,
    var colorHex: Long,
    var alpha: Float = 1f
)

data class ScreenShake(
    var intensity: Float = 0f,
    var duration: Float = 0f
) {
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    fun update(dt: Float) {
        if (duration > 0f) {
            duration -= dt
            offsetX = ((Math.random() * 2.0 - 1.0) * intensity).toFloat()
            offsetY = ((Math.random() * 2.0 - 1.0) * intensity).toFloat()
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }
}
