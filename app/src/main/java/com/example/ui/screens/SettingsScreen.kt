package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.GameAudioEngine
import com.example.core.repository.GameRepository
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val profile by repository.profile.collectAsState()

    var sfxVal by remember(profile.sfxVolume) { mutableFloatStateOf(profile.sfxVolume) }
    var musicVal by remember(profile.musicVolume) { mutableFloatStateOf(profile.musicVolume) }
    var shakeVal by remember(profile.screenShakeEnabled) { mutableStateOf(profile.screenShakeEnabled) }
    var leftHandedVal by remember(profile.leftHandedControls) { mutableStateOf(profile.leftHandedControls) }

    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldenReward)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SETTINGS & AUDIO",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldenReward
                    )
                    Text(
                        text = "Audio, Controls & Accessibility",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        containerColor = PrehistoricDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Audio Section
            Text("AUDIO SETTINGS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldenReward)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Sound Effects Volume: ${(sfxVal * 100).toInt()}%", fontSize = 12.sp, color = TextPrimary)
                    Slider(
                        value = sfxVal,
                        onValueChange = {
                            sfxVal = it
                            repository.updateSettings(sfxVal, musicVal, shakeVal, leftHandedVal)
                        },
                        colors = SliderDefaults.colors(thumbColor = JungleGreen, activeTrackColor = JungleGreen)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Ambient Music Volume: ${(musicVal * 100).toInt()}%", fontSize = 12.sp, color = TextPrimary)
                    Slider(
                        value = musicVal,
                        onValueChange = {
                            musicVal = it
                            repository.updateSettings(sfxVal, musicVal, shakeVal, leftHandedVal)
                        },
                        colors = SliderDefaults.colors(thumbColor = GoldenReward, activeTrackColor = GoldenReward)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gameplay & Accessibility Section
            Text("GAMEPLAY & ACCESSIBILITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldenReward)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Screen Shake Feedback", fontSize = 13.sp, color = TextPrimary)
                        Switch(
                            checked = shakeVal,
                            onCheckedChange = {
                                shakeVal = it
                                repository.updateSettings(sfxVal, musicVal, shakeVal, leftHandedVal)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Left-Handed Joystick Layout", fontSize = 13.sp, color = TextPrimary)
                        Switch(
                            checked = leftHandedVal,
                            onCheckedChange = {
                                leftHandedVal = it
                                repository.updateSettings(sfxVal, musicVal, shakeVal, leftHandedVal)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Data Section
            Text("SAVE DATA & SYSTEM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldenReward)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Version: 1.0.0 (Jurassic Native Release)",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Dino Planet: Jurassic Survival • Offline Persistent Engine",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.fillMaxWidth().testTag("reset_progress_button")
                    ) {
                        Text("Reset All Progress", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("Reset All Progress?", fontWeight = FontWeight.Bold, color = DangerRed) },
                text = { Text("Are you sure? This will delete all your unlocked dinosaurs, sanctuary structures, inventory, and quest milestones.", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            repository.resetGameProgress()
                            showResetConfirm = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Reset Permanently", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
