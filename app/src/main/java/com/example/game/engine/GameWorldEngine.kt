package com.example.game.engine

import com.example.core.audio.GameAudioEngine
import com.example.core.model.*
import kotlin.math.*

class GameWorldEngine(
    val worldWidth: Float = 2400f,
    val worldHeight: Float = 1600f
) {
    // Player dinosaur physics & state
    var playerX: Float = 500f
    var playerY: Float = 500f
    var playerZ: Float = 0f
    var playerVZ: Float = 0f
    var playerFacingRight: Boolean = true
    var isMoving: Boolean = false
    var isSprinting: Boolean = false
    var isAttacking: Boolean = false
    var attackTimer: Float = 0f
    var abilityCooldown: Float = 0f
    var invulnerableTimer: Float = 0f
    var tailAngle: Float = 0f
    var walkAnimTime: Float = 0f

    // Vitals
    val vitals = PlayerVitals()
    var currentHealth: Float = 100f
    var currentHunger: Float = 100f
    var currentHydration: Float = 100f
    var currentStamina: Float = 100f

    // Camera follow with target & shake
    var cameraX: Float = 500f
    var cameraY: Float = 500f
    val screenShake = ScreenShake()

    // Environment
    var dayTimeSeconds: Float = 60f // 240 seconds = full day cycle
    var currentWeather: WeatherType = WeatherType.CLEAR
    var weatherTimer: Float = 45f
    var disasterActive: Boolean = false
    var disasterTimer: Float = 0f
    var distanceTraveled: Float = 0f

    // Entities
    val wildlifeList = mutableListOf<Wildlife>()
    val resourceNodes = mutableListOf<ResourceNode>()
    val particles = mutableListOf<GameParticle>()

    // Interactive node near player
    var nearbyInteractable: ResourceNode? = null

    init {
        initWorldNodes()
        initWildlife()
    }

    private fun initWorldNodes() {
        // Berry bushes
        val berryPositions = listOf(
            Pair(350f, 420f), Pair(620f, 380f), Pair(480f, 720f),
            Pair(900f, 550f), Pair(1200f, 400f), Pair(1600f, 700f),
            Pair(750f, 1100f), Pair(1400f, 1200f)
        )
        berryPositions.forEachIndexed { i, (x, y) ->
            resourceNodes.add(ResourceNode("berry_$i", ResourceNodeType.BERRY_BUSH, x, y))
        }

        // River & Spring pools
        resourceNodes.add(ResourceNode("spring_1", ResourceNodeType.SPRING_POOL, 800f, 850f, 70f))
        resourceNodes.add(ResourceNode("spring_2", ResourceNodeType.SPRING_POOL, 1500f, 950f, 70f))

        // Petrified trees & Basalt rocks
        val logPositions = listOf(Pair(400f, 600f), Pair(1100f, 680f), Pair(1350f, 350f), Pair(1750f, 1100f))
        logPositions.forEachIndexed { i, (x, y) ->
            resourceNodes.add(ResourceNode("log_$i", ResourceNodeType.PETRIFIED_LOG, x, y))
        }

        val rockPositions = listOf(Pair(600f, 250f), Pair(1400f, 220f), Pair(1900f, 600f), Pair(1050f, 1300f))
        rockPositions.forEachIndexed { i, (x, y) ->
            resourceNodes.add(ResourceNode("rock_$i", ResourceNodeType.BASALT_ROCK, x, y))
        }

        // Fossil mounds
        resourceNodes.add(ResourceNode("fossil_1", ResourceNodeType.FOSSIL_MOUND, 720f, 300f))
        resourceNodes.add(ResourceNode("fossil_2", ResourceNodeType.FOSSIL_MOUND, 1650f, 450f))
        resourceNodes.add(ResourceNode("fossil_3", ResourceNodeType.FOSSIL_MOUND, 1300f, 1350f))

        // Hidden raptor egg nest
        resourceNodes.add(ResourceNode("nest_1", ResourceNodeType.HIDDEN_NEST, 1850f, 350f))

        // Basic starting Leaf Shelter
        resourceNodes.add(ResourceNode("shelter_start", ResourceNodeType.SHELTER_SITE, 450f, 480f, 60f))
    }

    private fun initWildlife() {
        wildlifeList.add(Wildlife("gallimimus_1", EntityType.PREY_GALLIMIMUS, 750f, 600f, speed = 4.2f))
        wildlifeList.add(Wildlife("gallimimus_2", EntityType.PREY_GALLIMIMUS, 950f, 700f, speed = 4.2f))
        wildlifeList.add(Wildlife("gallimimus_3", EntityType.PREY_GALLIMIMUS, 1400f, 650f, speed = 4.2f))

        wildlifeList.add(Wildlife("trike_1", EntityType.NEUTRAL_TRICERATOPS, 1200f, 850f, health = 220f, maxHealth = 220f, speed = 2.5f))
        wildlifeList.add(Wildlife("trike_2", EntityType.NEUTRAL_TRICERATOPS, 1600f, 1050f, health = 220f, maxHealth = 220f, speed = 2.5f))

        wildlifeList.add(Wildlife("allo_1", EntityType.HOSTILE_ALLOSAURUS, 1700f, 500f, health = 180f, maxHealth = 180f, speed = 4.0f, detectionRadius = 260f))
    }

    fun update(
        dt: Float,
        inputX: Float,
        inputY: Float,
        sprintRequested: Boolean,
        jumpRequested: Boolean,
        attackRequested: Boolean,
        abilityRequested: Boolean,
        activeSpecies: DinosaurSpecies,
        activeStage: GrowthStage
    ) {
        val delta = dt.coerceIn(0.001f, 0.05f)

        // Day/Night Cycle (240 seconds full loop)
        dayTimeSeconds = (dayTimeSeconds + delta) % 240f

        // Weather cycle
        weatherTimer -= delta
        if (weatherTimer <= 0f) {
            weatherTimer = (35..75).random().toFloat()
            currentWeather = if (Math.random() < 0.25) {
                WeatherType.VOLCANIC_ASH
            } else if (Math.random() < 0.5) {
                WeatherType.RAIN
            } else {
                WeatherType.CLEAR
            }
            if (currentWeather == WeatherType.VOLCANIC_ASH) {
                GameAudioEngine.playVolcanoWarning()
                triggerScreenShake(8f, 1.5f)
            }
        }

        // Periodic Volcanic rumble event
        if (currentWeather == WeatherType.VOLCANIC_ASH) {
            disasterTimer += delta
            if (disasterTimer >= 20f) {
                disasterActive = true
                triggerScreenShake(12f, 2.0f)
                GameAudioEngine.playVolcanoWarning()
                if (disasterTimer >= 28f) {
                    disasterActive = false
                    disasterTimer = 0f
                }
            }
        } else {
            disasterActive = false
            disasterTimer = 0f
        }

        // Screen shake decay
        screenShake.update(delta)

        // Timers
        if (invulnerableTimer > 0f) invulnerableTimer -= delta
        if (abilityCooldown > 0f) abilityCooldown -= delta
        if (attackTimer > 0f) {
            attackTimer -= delta
            if (attackTimer <= 0f) isAttacking = false
        }

        // Movement Speed calculation based on stats & growth
        val baseSpeed = activeSpecies.baseSpeed * activeStage.scaleMultiplier * 40f
        val canSprint = sprintRequested && currentStamina > 5f && (inputX != 0f || inputY != 0f)
        isSprinting = canSprint
        val currentSpeed = if (canSprint) baseSpeed * 1.6f else baseSpeed

        // Player movement
        val inputMag = sqrt(inputX * inputX + inputY * inputY)
        if (inputMag > 0.1f) {
            val normX = inputX / inputMag
            val normY = inputY / inputMag
            val moveStep = currentSpeed * delta
            playerX = (playerX + normX * moveStep).coerceIn(40f, worldWidth - 40f)
            playerY = (playerY + normY * moveStep).coerceIn(40f, worldHeight - 40f)
            distanceTraveled += moveStep

            if (normX > 0.05f) playerFacingRight = true
            else if (normX < -0.05f) playerFacingRight = false

            isMoving = true
            walkAnimTime += delta * (if (canSprint) 16f else 10f)

            // Tail sway
            tailAngle = sin(walkAnimTime) * 18f

            // Stamina drain on sprint
            if (canSprint) {
                currentStamina = (currentStamina - 14f * delta).coerceAtLeast(0f)
                // Dust particle
                if (Math.random() < 0.3) {
                    spawnParticle(playerX, playerY + 8f, -normX * 15f, -10f, 6f, 0.4f, 0xFF8D6E63)
                }
            }
        } else {
            isMoving = false
            tailAngle = sin(walkAnimTime * 0.5f) * 6f
            walkAnimTime += delta * 2f
        }

        // Stamina regen
        if (!isSprinting) {
            currentStamina = (currentStamina + 18f * delta).coerceAtMost(100f)
        }

        // Jump Physics
        if (jumpRequested && playerZ <= 0.01f && currentStamina >= 8f) {
            playerVZ = 240f
            currentStamina -= 8f
            GameAudioEngine.playButtonClick()
            spawnParticle(playerX, playerY, 0f, -20f, 10f, 0.4f, 0xFF795548)
        }
        if (playerZ > 0f || playerVZ != 0f) {
            playerZ += playerVZ * delta
            playerVZ -= 550f * delta // gravity
            if (playerZ <= 0f) {
                playerZ = 0f
                playerVZ = 0f
                spawnParticle(playerX, playerY, 0f, 0f, 8f, 0.3f, 0xFF795548)
            }
        }

        // Attack input
        if (attackRequested && !isAttacking && currentStamina >= 10f) {
            isAttacking = true
            attackTimer = 0.35f
            currentStamina -= 10f
            GameAudioEngine.playAttack()
            triggerScreenShake(4f, 0.2f)

            // Check hit against wildlife
            val attackRange = 75f * activeStage.scaleMultiplier
            wildlifeList.forEach { w ->
                val dist = hypot(w.x - playerX, w.y - playerY)
                if (dist <= attackRange) {
                    val dmg = activeSpecies.baseDamage * activeStage.damageMultiplier
                    w.health -= dmg
                    // Knockback
                    val dirX = if (playerFacingRight) 1f else -1f
                    w.x += dirX * 35f
                    spawnParticle(w.x, w.y, dirX * 30f, -20f, 12f, 0.5f, 0xFFF04F52)

                    if (w.type == EntityType.NEUTRAL_TRICERATOPS) {
                        w.state = "CHASE"
                    }
                }
            }
        }

        // Special Ability
        if (abilityRequested && abilityCooldown <= 0.01f && currentStamina >= 20f) {
            abilityCooldown = 8f
            currentStamina -= 20f
            GameAudioEngine.playRoar()
            triggerScreenShake(8f, 0.4f)
            // Visual blast particles
            for (i in 0 until 12) {
                val angle = (i * PI * 2 / 12).toFloat()
                spawnParticle(
                    playerX, playerY,
                    cos(angle) * 80f, sin(angle) * 80f,
                    14f, 0.6f, activeSpecies.primaryColorHex
                )
            }
            // Stun or damage nearby enemies
            wildlifeList.forEach { w ->
                if (hypot(w.x - playerX, w.y - playerY) < 180f) {
                    w.state = "FLEE"
                    w.stateTimer = 4f
                }
            }
        }

        // Vitals ticking
        val heatMultiplier = if (currentWeather == WeatherType.HEATWAVE) 1.6f else 1.0f
        currentHunger = (currentHunger - 0.05f * delta).coerceIn(0f, 100f)
        currentHydration = (currentHydration - 0.06f * heatMultiplier * delta).coerceIn(0f, 100f)

        // Shelter proximity healing
        val nearShelter = resourceNodes.any {
            it.type == ResourceNodeType.SHELTER_SITE && hypot(it.x - playerX, it.y - playerY) < 120f
        }
        if (nearShelter) {
            currentHealth = (currentHealth + 6f * delta).coerceAtMost(100f)
            currentStamina = (currentStamina + 10f * delta).coerceAtMost(100f)
        } else if (currentHunger <= 0.01f || currentHydration <= 0.01f) {
            currentHealth = (currentHealth - 1.5f * delta).coerceAtLeast(10f)
        }

        // Volcanic disaster damage if not sheltered
        if (disasterActive && !nearShelter) {
            currentHealth = (currentHealth - 2.5f * delta).coerceAtLeast(15f)
            if (Math.random() < 0.1) {
                triggerScreenShake(6f, 0.3f)
            }
        }

        // Camera Smooth Follow
        val targetCamX = playerX
        val targetCamY = playerY
        cameraX += (targetCamX - cameraX) * 0.12f
        cameraY += (targetCamY - cameraY) * 0.12f

        // Update Wildlife AI
        updateWildlife(delta)

        // Update Resource nodes respawn
        resourceNodes.forEach { node ->
            if (node.isDepleted) {
                node.respawnTimer -= delta
                if (node.respawnTimer <= 0f) {
                    node.isDepleted = false
                }
            }
        }

        // Find nearby interactable node
        nearbyInteractable = resourceNodes.filter { !it.isDepleted }
            .minByOrNull { hypot(it.x - playerX, it.y - playerY) }
            ?.takeIf { hypot(it.x - playerX, it.y - playerY) <= it.radius }

        // Update particles
        val particleIter = particles.iterator()
        while (particleIter.hasNext()) {
            val p = particleIter.next()
            p.x += p.vx * delta
            p.y += p.vy * delta
            p.life -= delta
            p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            if (p.life <= 0f) {
                particleIter.remove()
            }
        }

        // Ambient particles (fireflies at night, ash during volcano)
        if (currentWeather == WeatherType.VOLCANIC_ASH && Math.random() < 0.3) {
            spawnParticle(
                cameraX + ((Math.random() - 0.5) * 800).toFloat(),
                cameraY - 300f,
                ((Math.random() - 0.5) * 40).toFloat(),
                (40 + Math.random() * 60).toFloat(),
                5f, 2.5f, 0xFFFF793D
            )
        }
    }

    private fun updateWildlife(delta: Float) {
        val iter = wildlifeList.iterator()
        while (iter.hasNext()) {
            val w = iter.next()
            if (w.health <= 0f) {
                // Spawn defeat items / particles
                spawnParticle(w.x, w.y, 0f, -10f, 16f, 0.8f, 0xFFFFC857)
                iter.remove()
                continue
            }

            val distToPlayer = hypot(playerX - w.x, playerY - w.y)
            w.stateTimer -= delta
            if (w.attackCooldown > 0f) w.attackCooldown -= delta

            when (w.type) {
                EntityType.PREY_GALLIMIMUS -> {
                    if (distToPlayer < w.detectionRadius) {
                        w.state = "FLEE"
                        val angle = atan2(w.y - playerY, w.x - playerX)
                        w.vx = cos(angle) * w.speed * 40f
                        w.vy = sin(angle) * w.speed * 40f
                        w.facingRight = w.vx > 0
                    } else if (w.stateTimer <= 0f) {
                        w.stateTimer = (2..5).random().toFloat()
                        if (Math.random() < 0.5) {
                            w.state = "ROAM"
                            val angle = (Math.random() * PI * 2).toFloat()
                            w.vx = cos(angle) * w.speed * 20f
                            w.vy = sin(angle) * w.speed * 20f
                            w.facingRight = w.vx > 0
                        } else {
                            w.state = "IDLE"
                            w.vx = 0f
                            w.vy = 0f
                        }
                    }
                }
                EntityType.NEUTRAL_TRICERATOPS -> {
                    if (w.state == "CHASE" && distToPlayer < 300f) {
                        val angle = atan2(playerY - w.y, playerX - w.x)
                        w.vx = cos(angle) * w.speed * 45f
                        w.vy = sin(angle) * w.speed * 45f
                        w.facingRight = w.vx > 0
                        if (distToPlayer < w.attackRadius && w.attackCooldown <= 0f) {
                            // Hit player
                            damagePlayer(15f)
                            w.attackCooldown = 1.5f
                        }
                    } else if (w.stateTimer <= 0f) {
                        w.state = "IDLE"
                        w.stateTimer = (3..6).random().toFloat()
                        w.vx = 0f
                        w.vy = 0f
                    }
                }
                EntityType.HOSTILE_ALLOSAURUS -> {
                    if (distToPlayer < w.detectionRadius) {
                        w.state = "CHASE"
                        val angle = atan2(playerY - w.y, playerX - w.x)
                        w.vx = cos(angle) * w.speed * 35f
                        w.vy = sin(angle) * w.speed * 35f
                        w.facingRight = w.vx > 0

                        if (distToPlayer < w.attackRadius && w.attackCooldown <= 0f) {
                            damagePlayer(25f)
                            w.attackCooldown = 1.8f
                            GameAudioEngine.playRoar()
                        }
                    } else if (w.stateTimer <= 0f) {
                        w.stateTimer = (3..6).random().toFloat()
                        w.state = if (Math.random() < 0.6) "ROAM" else "IDLE"
                        if (w.state == "ROAM") {
                            val angle = (Math.random() * PI * 2).toFloat()
                            w.vx = cos(angle) * w.speed * 18f
                            w.vy = sin(angle) * w.speed * 18f
                            w.facingRight = w.vx > 0
                        } else {
                            w.vx = 0f
                            w.vy = 0f
                        }
                    }
                }
            }

            // Move wildlife within bounds
            w.x = (w.x + w.vx * delta).coerceIn(100f, worldWidth - 100f)
            w.y = (w.y + w.vy * delta).coerceIn(100f, worldHeight - 100f)
        }
    }

    private fun damagePlayer(amount: Float) {
        if (invulnerableTimer > 0f) return
        currentHealth = (currentHealth - amount).coerceAtLeast(0f)
        invulnerableTimer = 1.0f
        triggerScreenShake(10f, 0.35f)
        GameAudioEngine.playAttack()
    }

    fun triggerScreenShake(intensity: Float, duration: Float) {
        screenShake.intensity = intensity
        screenShake.duration = duration
    }

    fun harvestNode(node: ResourceNode): Pair<String, Int>? {
        if (node.isDepleted) return null
        node.isDepleted = true
        node.respawnTimer = node.type.respawnSeconds

        spawnParticle(node.x, node.y, 0f, -25f, 14f, 0.6f, 0xFF39B86A)
        return when (node.type) {
            ResourceNodeType.SPRING_POOL -> {
                currentHydration = 100f
                GameAudioEngine.playDrink()
                Pair("clean_water", 1)
            }
            ResourceNodeType.BERRY_BUSH -> {
                currentHunger = (currentHunger + 25f).coerceAtMost(100f)
                currentHydration = (currentHydration + 10f).coerceAtMost(100f)
                GameAudioEngine.playCollect()
                Pair("berries", node.type.harvestCount)
            }
            ResourceNodeType.SHELTER_SITE -> {
                currentHealth = 100f
                currentStamina = 100f
                GameAudioEngine.playButtonClick()
                null
            }
            else -> {
                GameAudioEngine.playCollect()
                Pair(node.type.itemId, node.type.harvestCount)
            }
        }
    }

    fun spawnParticle(x: Float, y: Float, vx: Float, vy: Float, size: Float, life: Float, colorHex: Long) {
        if (particles.size > 80) particles.removeAt(0)
        particles.add(GameParticle(x, y, vx, vy, size, life, life, colorHex))
    }

    fun getCurrentDayPhase(): DayPhase {
        val hour = (dayTimeSeconds / 240f) * 24f
        return when {
            hour < 5f -> DayPhase.NIGHT
            hour < 7f -> DayPhase.DAWN
            hour < 12f -> DayPhase.MORNING
            hour < 17f -> DayPhase.NOON
            hour < 20f -> DayPhase.SUNSET
            else -> DayPhase.NIGHT
        }
    }
}
