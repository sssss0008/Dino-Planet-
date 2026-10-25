package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.GameAudioEngine
import com.example.core.model.*
import com.example.core.repository.GameRepository
import com.example.game.engine.GameWorldEngine
import com.example.game.engine.GameWorldRenderer
import com.example.game.engine.ResourceNodeType
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.isActive

@Composable
fun GamePlayScreen(
    repository: GameRepository,
    onOpenInventory: () -> Unit,
    onOpenSanctuary: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitToMenu: () -> Unit
) {
    val profile by repository.profile.collectAsState()
    val quests by repository.quests.collectAsState()

    val activeSpecies = remember(profile.activeDinoId) {
        PrehistoricDatabase.dinosaurs.find { it.id == profile.activeDinoId } ?: PrehistoricDatabase.dinosaurs.first()
    }
    val activeStage = remember(profile.activeGrowthStage) {
        try {
            GrowthStage.valueOf(profile.activeGrowthStage)
        } catch (_: Exception) {
            GrowthStage.JUVENILE
        }
    }

    val engine = remember { GameWorldEngine() }

    var joystickX by remember { mutableFloatStateOf(0f) }
    var joystickY by remember { mutableFloatStateOf(0f) }
    var sprintRequested by remember { mutableStateOf(false) }
    var jumpRequested by remember { mutableStateOf(false) }
    var attackRequested by remember { mutableStateOf(false) }
    var abilityRequested by remember { mutableStateOf(false) }

    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }

    val activeQuest = remember(quests) {
        quests.firstOrNull { !it.isCompleted } ?: quests.firstOrNull()
    }

    BackHandler {
        if (!isPaused) isPaused = true else onExitToMenu()
    }

    // Game loop running on frame nanoseconds
    LaunchedEffect(isPaused, isGameOver) {
        var lastTime = System.nanoTime()
        while (isActive && !isPaused && !isGameOver) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                engine.update(
                    dt = dt,
                    inputX = joystickX,
                    inputY = joystickY,
                    sprintRequested = sprintRequested,
                    jumpRequested = jumpRequested,
                    attackRequested = attackRequested,
                    abilityRequested = abilityRequested,
                    activeSpecies = activeSpecies,
                    activeStage = activeStage
                )

                // Track movement quest
                if (engine.isMoving) {
                    repository.incrementQuestProgress("quest_1", 1)
                    repository.incrementAchievementProgress("ach_steps", 1)
                }

                // Check Game Over
                if (engine.currentHealth <= 0f) {
                    isGameOver = true
                    GameAudioEngine.playVolcanoWarning()
                }

                // Reset instantaneous button triggers
                jumpRequested = false
                attackRequested = false
                abilityRequested = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrehistoricDark)
    ) {
        // Real-Time 2D Prehistoric Gameplay Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            GameWorldRenderer.draw(
                drawScope = this,
                engine = engine,
                activeSpecies = activeSpecies,
                activeStage = activeStage
            )
        }

        // TOP HUD BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // TOP-LEFT: Survival Vital Bars
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dino Level & Species Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xBB000000))
                            .border(1.dp, JungleGreen, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${activeSpecies.name} (L${profile.level})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldenReward
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Vitals
                Row {
                    SurvivalVitalBar(
                        icon = Icons.Default.Favorite,
                        current = engine.currentHealth,
                        max = 100f,
                        color = HealthRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SurvivalVitalBar(
                        icon = Icons.Default.Restaurant,
                        current = engine.currentHunger,
                        max = 100f,
                        color = HungerOrange
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row {
                    SurvivalVitalBar(
                        icon = Icons.Default.WaterDrop,
                        current = engine.currentHydration,
                        max = 100f,
                        color = HydrationBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SurvivalVitalBar(
                        icon = Icons.Default.FlashOn,
                        current = engine.currentStamina,
                        max = 100f,
                        color = StaminaGreen
                    )
                }
            }

            // TOP-CENTER: Biome & Active Objective Pill
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Biome & Weather pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xAA000000))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Landscape,
                        contentDescription = null,
                        tint = JungleGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verdant Valley • ${engine.currentWeather.displayName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                // Active Quest Banner
                if (activeQuest != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC182823))
                            .border(1.dp, GoldenReward.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = GoldenReward,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${activeQuest.title}: ${activeQuest.currentProgress}/${activeQuest.targetCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // TOP-RIGHT: Map, Inventory, Pause Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Map Button
                IconButton(
                    onClick = {
                        GameAudioEngine.playButtonClick()
                        onOpenMap()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .border(1.dp, SkyBlue, CircleShape)
                        .testTag("game_map_button")
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Map", tint = SkyBlue, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Inventory Button
                IconButton(
                    onClick = {
                        GameAudioEngine.playButtonClick()
                        onOpenInventory()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .border(1.dp, GoldenReward, CircleShape)
                        .testTag("game_inventory_button")
                ) {
                    Icon(Icons.Default.Backpack, contentDescription = "Inventory", tint = GoldenReward, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pause Button
                IconButton(
                    onClick = {
                        GameAudioEngine.playButtonClick()
                        isPaused = true
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .testTag("game_pause_button")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // VOLCANIC DISASTER WARNING BANNER
        if (engine.disasterActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 70.dp)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = GoldenReward)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXTINCTION HAZARD: Volcanic tremor! Seek Leaf Shelter immediately!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // CONTEXTUAL INTERACTIVE PROMPT
        engine.nearbyInteractable?.let { node ->
            InteractivePromptBadge(
                title = node.type.displayName,
                actionText = node.type.actionText,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp),
                onInteract = {
                    val result = engine.harvestNode(node)
                    if (result != null) {
                        repository.addItem(result.first, result.second)
                        if (node.type == ResourceNodeType.SPRING_POOL) {
                            repository.incrementQuestProgress("quest_3", 1)
                        }
                    }
                }
            )
        }

        // BOTTOM CONTROLS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Virtual Joystick
            VirtualJoystick(
                onDirectionChange = { dx, dy ->
                    joystickX = dx
                    joystickY = dy
                }
            )

            // Right-side Action Buttons Cluster
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sprint Toggle / Hold
                ActionButton(
                    icon = Icons.Default.DirectionsRun,
                    label = "Sprint",
                    accentColor = StaminaGreen,
                    onClick = {
                        sprintRequested = !sprintRequested
                        GameAudioEngine.playButtonClick()
                    }
                )

                // Jump Button
                ActionButton(
                    icon = Icons.Default.ArrowUpward,
                    label = "Jump",
                    accentColor = SkyBlue,
                    onClick = {
                        jumpRequested = true
                    }
                )

                // Attack / Bite Button
                ActionButton(
                    icon = Icons.Default.FlashOn,
                    label = "Strike",
                    accentColor = DangerRed,
                    onClick = {
                        attackRequested = true
                    }
                )

                // Special Ability Button
                ActionButton(
                    icon = Icons.Default.Whatshot,
                    label = "Roar",
                    accentColor = GoldenReward,
                    isCooldown = engine.abilityCooldown > 0f,
                    onClick = {
                        abilityRequested = true
                    }
                )
            }
        }

        // PAUSE DIALOG
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD000000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .padding(24.dp)
                        .border(2.dp, JungleGreen, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ADVENTURE PAUSED",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldenReward
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { isPaused = false },
                            colors = ButtonDefaults.buttonColors(containerColor = JungleGreen),
                            modifier = Modifier.fillMaxWidth().testTag("resume_game_button")
                        ) {
                            Text("Resume Game", fontWeight = FontWeight.Bold, color = PrehistoricDark)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                isPaused = false
                                onOpenInventory()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Backpack & Crafting", color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                isPaused = false
                                onOpenSanctuary()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Return to Sanctuary Base", color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                isPaused = false
                                onOpenSettings()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Settings", color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onExitToMenu,
                            modifier = Modifier.fillMaxWidth().testTag("exit_to_menu_button")
                        ) {
                            Text("Save & Exit to Main Menu", color = DangerRed)
                        }
                    }
                }
            }
        }

        // GAME OVER CHECKPOINT DIALOG
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE1A0000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .padding(24.dp)
                        .border(2.dp, DangerRed, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(42.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DINOSAUR EXHAUSTED",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = DangerRed
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your dinosaur fell to the prehistoric wilderness! Rescuing you back to the safe Leaf Shelter. All collected resources are preserved.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                engine.currentHealth = 100f
                                engine.currentHunger = 60f
                                engine.currentHydration = 60f
                                engine.currentStamina = 100f
                                engine.playerX = 450f
                                engine.playerY = 480f
                                isGameOver = false
                                GameAudioEngine.playCollect()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JungleGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Respawn at Shelter", fontWeight = FontWeight.Bold, color = PrehistoricDark)
                        }
                    }
                }
            }
        }
    }
}
