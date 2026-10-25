package com.example.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.GameAudioEngine
import com.example.core.database.PlayerProfileEntity
import com.example.core.model.GrowthStage
import com.example.core.model.PrehistoricDatabase
import com.example.core.repository.GameRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun"
    )

    val dinoWalk by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 450f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dinoWalk"
    )

    LaunchedEffect(Unit) {
        GameAudioEngine.playCollect()
        delay(2600)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrehistoricDark),
        contentAlignment = Alignment.Center
    ) {
        // Dramatic prehistoric landscape canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sunset gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2C1654), Color(0xFFD84315), Color(0xFFFFB300)),
                    startY = 0f,
                    endY = h * 0.75f
                ),
                size = Size(w, h * 0.75f)
            )

            // Glowing sun behind mountain peak
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF9C4), Color(0xFFFF9800).copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = 140f * sunPulse
                ),
                radius = 140f * sunPulse,
                center = Offset(w * 0.5f, h * 0.42f)
            )

            // Mountain silhouette
            val mountain = Path().apply {
                moveTo(0f, h * 0.75f)
                lineTo(w * 0.2f, h * 0.48f)
                lineTo(w * 0.5f, h * 0.38f) // High peak
                lineTo(w * 0.75f, h * 0.52f)
                lineTo(w, h * 0.42f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(mountain, color = Color(0xFF0F221B))

            // Ground ridge silhouette
            drawRect(
                color = Color(0xFF0A1813),
                topLeft = Offset(0f, h * 0.72f),
                size = Size(w, h * 0.28f)
            )

            // Dinosaur silhouette crossing the ridge
            val dx = (dinoWalk % (w + 100f))
            val dy = h * 0.71f
            drawOval(
                color = Color(0xFF050D0A),
                topLeft = Offset(dx, dy - 20f),
                size = Size(46f, 26f)
            )
            drawCircle(
                color = Color(0xFF050D0A),
                radius = 12f,
                center = Offset(dx + 38f, dy - 22f)
            )
            drawLine(
                color = Color(0xFF050D0A),
                start = Offset(dx, dy - 12f),
                end = Offset(dx - 32f, dy - 26f),
                strokeWidth = 6f
            )
        }

        // Title branding overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 60.dp)
        ) {
            Text(
                text = "DINO PLANET",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = GoldenReward,
                letterSpacing = 4.sp
            )
            Text(
                text = "JURASSIC SURVIVAL",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                color = JungleGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun MainMenuScreen(
    repository: GameRepository,
    onStartAdventure: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenSanctuary: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenExcavation: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val profile by repository.profile.collectAsState()
    val activeDino = remember(profile.activeDinoId) {
        PrehistoricDatabase.dinosaurs.find { it.id == profile.activeDinoId } ?: PrehistoricDatabase.dinosaurs.first()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "menuDino")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    LaunchedEffect(Unit) {
        GameAudioEngine.startAmbientLoop()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrehistoricDark)
    ) {
        // Prehistoric animated valley background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Dawn/Day Sky
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E3C33), Color(0xFF102821)),
                    startY = 0f,
                    endY = h
                ),
                size = Size(w, h)
            )

            // Volcanic mountain silhouette with glowing caldera
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF793D).copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(w * 0.72f, h * 0.38f),
                    radius = 160f
                ),
                radius = 160f,
                center = Offset(w * 0.72f, h * 0.38f)
            )

            // Midground prehistoric valley hills
            val hillPath = Path().apply {
                moveTo(0f, h * 0.65f)
                quadraticTo(w * 0.3f, h * 0.52f, w * 0.6f, h * 0.62f)
                quadraticTo(w * 0.85f, h * 0.7f, w, h * 0.58f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hillPath, color = Color(0xFF122C23))

            // Prehistoric fern silhouettes
            for (i in 0..8) {
                val fx = (i * 120f) % w
                drawCircle(color = Color(0xFF0B1F18), radius = 32f, center = Offset(fx, h * 0.88f))
            }
        }

        // TOP BAR: Profile & Currencies
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile & Level
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x88000000))
                    .border(1.dp, JungleGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(JungleGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "L${profile.level}",
                        fontWeight = FontWeight.Black,
                        color = PrehistoricDark,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = profile.displayName,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "XP ${profile.currentXp}/${profile.requiredXp}",
                            fontSize = 10.sp,
                            color = GoldenReward
                        )
                    }
                }
            }

            // Currencies & Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fossils
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x88000000))
                        .border(1.dp, Color(0xFF8D6E63), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Fossils",
                        tint = GoldenReward,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${profile.fossils}",
                        fontWeight = FontWeight.Bold,
                        color = GoldenReward,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Crystals
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x88000000))
                        .border(1.dp, SkyBlue, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Crystals",
                        tint = SkyBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${profile.crystals}",
                        fontWeight = FontWeight.Bold,
                        color = SkyBlue,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Settings button
                IconButton(
                    onClick = {
                        GameAudioEngine.playButtonClick()
                        onOpenSettings()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .testTag("menu_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // CENTER-RIGHT: Hero dinosaur illustration with breathing scale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 50.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Canvas(
                modifier = Modifier
                    .size(280.dp)
                    .clickable {
                        GameAudioEngine.playRoar()
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val scale = breatheScale

                // Ground shadow
                drawOval(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(cx - 90f * scale, cy + 70f),
                    size = Size(180f * scale, 36f)
                )

                // Large stylized hero dinosaur matching active species
                val prim = Color(activeDino.primaryColorHex)
                val acc = Color(activeDino.accentColorHex)

                // Torso
                drawOval(
                    color = prim,
                    topLeft = Offset(cx - 70f * scale, cy - 40f * scale),
                    size = Size(140f * scale, 90f * scale)
                )

                // Head
                drawCircle(color = prim, radius = 45f * scale, center = Offset(cx + 60f * scale, cy - 45f * scale))
                // Snout
                drawRoundRect(
                    color = prim,
                    topLeft = Offset(cx + 75f * scale, cy - 40f * scale),
                    size = Size(55f * scale, 40f * scale),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )

                // Eye
                drawCircle(color = Color(0xFFFFB300), radius = 10f * scale, center = Offset(cx + 80f * scale, cy - 50f * scale))
                drawCircle(color = Color.Black, radius = 5f * scale, center = Offset(cx + 82f * scale, cy - 50f * scale))

                // Tail
                drawLine(
                    color = prim,
                    start = Offset(cx - 50f * scale, cy),
                    end = Offset(cx - 130f * scale, cy - 35f * scale),
                    strokeWidth = 24f * scale
                )

                // Crest plumage
                drawLine(
                    color = acc,
                    start = Offset(cx + 40f * scale, cy - 65f * scale),
                    end = Offset(cx + 10f * scale, cy - 95f * scale),
                    strokeWidth = 10f * scale
                )
            }
        }

        // BOTTOM BAR / MAIN ACTIONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // BOTTOM LEFT: Daily Reward & Excavation
            Column {
                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, GoldenReward, RoundedCornerShape(16.dp))
                        .clickable {
                            val claimed = repository.claimDailyReward()
                            if (!claimed) {
                                GameAudioEngine.playButtonClick()
                            }
                        }
                        .testTag("daily_reward_card"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.95f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = GoldenReward,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Daily Survival Pack",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Day ${profile.consecutiveDailyClaims + 1} Available",
                                color = GoldenReward,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        GameAudioEngine.playButtonClick()
                        onOpenExcavation()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenReward),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(GoldenReward, AmberGlow))),
                    modifier = Modifier.testTag("excavation_minigame_button")
                ) {
                    Icon(imageVector = Icons.Default.Hardware, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fossil Dig", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // BOTTOM CENTER: Primary Play Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        GameAudioEngine.playRoar()
                        onStartAdventure()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JungleGreen),
                    shape = RoundedCornerShape(26.dp),
                    contentPadding = PaddingValues(horizontal = 38.dp, vertical = 14.dp),
                    modifier = Modifier
                        .border(2.dp, GoldenReward, RoundedCornerShape(26.dp))
                        .testTag("start_adventure_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = PrehistoricDark,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY ADVENTURE",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = PrehistoricDark,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chapter 1: A New Beginning",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // BOTTOM RIGHT: Secondary Hub Buttons
            Row {
                Button(
                    onClick = {
                        GameAudioEngine.playButtonClick()
                        onOpenCollection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(SurfaceBorder, JungleGreen))),
                    modifier = Modifier.testTag("nav_collection_button")
                ) {
                    Icon(Icons.Default.Pets, contentDescription = null, tint = GoldenReward, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dinos", fontSize = 12.sp, color = TextPrimary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        GameAudioEngine.playButtonClick()
                        onOpenSanctuary()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(SurfaceBorder, JungleGreen))),
                    modifier = Modifier.testTag("nav_sanctuary_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = JungleGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Base", fontSize = 12.sp, color = TextPrimary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        GameAudioEngine.playButtonClick()
                        onOpenJournal()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(SurfaceBorder, JungleGreen))),
                    modifier = Modifier.testTag("nav_journal_button")
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quests", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}
