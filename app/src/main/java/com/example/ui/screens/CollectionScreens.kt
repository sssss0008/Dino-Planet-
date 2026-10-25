package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.GameAudioEngine
import com.example.core.model.*
import com.example.core.repository.GameRepository
import com.example.ui.theme.*

@Composable
fun CollectionScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val profile by repository.profile.collectAsState()
    val dinoProgress by repository.dinoProgress.collectAsState()

    var selectedDiet by remember { mutableStateOf<DietType?>(null) }
    var selectedSpecies by remember { mutableStateOf<DinosaurSpecies?>(null) }

    BackHandler {
        if (selectedSpecies != null) {
            selectedSpecies = null
        } else {
            onBack()
        }
    }

    if (selectedSpecies != null) {
        val sp = selectedSpecies!!
        val progress = dinoProgress[sp.id]
        DinoDetailScreen(
            species = sp,
            progress = progress,
            isActive = profile.activeDinoId == sp.id,
            onSelectActive = {
                repository.setActiveDinosaur(sp.id)
                selectedSpecies = null
            },
            onEvolve = {
                repository.evolveDinosaur(sp.id)
            },
            onClose = { selectedSpecies = null }
        )
        return
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("collection_back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldenReward)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "DINOSAUR ROSTER",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldenReward
                    )
                    Text(
                        text = "12 Prehistoric Species • Evolution & Traits",
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
                .padding(horizontal = 16.dp)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDiet == null,
                    onClick = { selectedDiet = null },
                    label = { Text("All", fontSize = 11.sp) }
                )
                DietType.entries.forEach { diet ->
                    FilterChip(
                        selected = selectedDiet == diet,
                        onClick = { selectedDiet = diet },
                        label = { Text(diet.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                    )
                }
            }

            val filteredList = remember(selectedDiet) {
                if (selectedDiet == null) PrehistoricDatabase.dinosaurs
                else PrehistoricDatabase.dinosaurs.filter { it.diet == selectedDiet }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp)
            ) {
                items(filteredList) { species ->
                    val progress = dinoProgress[species.id]
                    val isUnlocked = progress?.isUnlocked == true
                    val isActive = profile.activeDinoId == species.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) GoldenReward else if (isUnlocked) JungleGreen.copy(alpha = 0.5f) else SurfaceBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                GameAudioEngine.playButtonClick()
                                selectedSpecies = species
                            }
                            .testTag("dino_card_${species.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnlocked) SurfaceDark else Color(0xFF0F1A17)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Dinosaur Preview Circle / Silhouette
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isUnlocked) Color(species.primaryColorHex).copy(alpha = 0.25f)
                                        else Color(0xFF1E2824)
                                    )
                                    .border(1.5.dp, if (isUnlocked) Color(species.primaryColorHex) else Color.DarkGray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Pets,
                                        contentDescription = null,
                                        tint = Color(species.primaryColorHex),
                                        modifier = Modifier.size(34.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = species.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) TextPrimary else TextMuted,
                                fontSize = 13.sp
                            )

                            Text(
                                text = species.rarity.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (species.rarity) {
                                    Rarity.COMMON -> RarityCommon
                                    Rarity.UNCOMMON -> RarityUncommon
                                    Rarity.RARE -> RarityRare
                                    Rarity.EPIC -> RarityEpic
                                    Rarity.LEGENDARY -> RarityLegendary
                                }
                            )

                            if (isActive) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoldenReward
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DinoDetailScreen(
    species: DinosaurSpecies,
    progress: com.example.core.database.DinoProgressEntity?,
    isActive: Boolean,
    onSelectActive: () -> Unit,
    onEvolve: () -> Unit,
    onClose: () -> Unit
) {
    val isUnlocked = progress?.isUnlocked == true
    val currentStage = remember(progress?.growthStage) {
        try {
            GrowthStage.valueOf(progress?.growthStage ?: "JUVENILE")
        } catch (_: Exception) {
            GrowthStage.JUVENILE
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "detail")
    val animScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldenReward)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = species.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldenReward
                )
            }
        },
        containerColor = PrehistoricDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Dinosaur Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F1F19))
                    .border(1.dp, Color(species.primaryColorHex).copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val scale = animScale * currentStage.scaleMultiplier

                    val prim = Color(species.primaryColorHex)
                    val acc = Color(species.accentColorHex)

                    // Torso
                    drawOval(color = prim, topLeft = Offset(cx - 50f * scale, cy - 25f * scale), size = Size(100f * scale, 50f * scale))
                    // Head
                    drawCircle(color = prim, radius = 30f * scale, center = Offset(cx + 45f * scale, cy - 30f * scale))
                    // Eye
                    drawCircle(color = Color(0xFFFFB300), radius = 7f * scale, center = Offset(cx + 56f * scale, cy - 34f * scale))
                    // Tail
                    drawLine(color = prim, start = Offset(cx - 35f * scale, cy), end = Offset(cx - 95f * scale, cy - 20f * scale), strokeWidth = 14f * scale)
                    // Accent Plumage
                    drawLine(color = acc, start = Offset(cx + 30f * scale, cy - 45f * scale), end = Offset(cx + 10f * scale, cy - 65f * scale), strokeWidth = 6f * scale)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Growth Stage Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Growth Stage: ${currentStage.displayName}",
                    fontWeight = FontWeight.Bold,
                    color = GoldenReward,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attributes Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttributePill("Health", "${(species.baseHealth * currentStage.healthMultiplier).toInt()}", HealthRed)
                AttributePill("Damage", "${(species.baseDamage * currentStage.damageMultiplier).toInt()}", DangerRed)
                AttributePill("Speed", "${species.baseSpeed}m/s", StaminaGreen)
                AttributePill("Defense", "${species.baseDefense.toInt()}", SkyBlue)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Special Ability Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Special Ability: ${species.specialAbility}",
                        fontWeight = FontWeight.Bold,
                        color = GoldenReward,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = species.abilityDescription,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = species.description,
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isUnlocked) {
                    OutlinedButton(
                        onClick = onEvolve,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenReward)
                    ) {
                        Text("Evolve (+Stats)")
                    }

                    Button(
                        onClick = onSelectActive,
                        enabled = !isActive,
                        colors = ButtonDefaults.buttonColors(containerColor = JungleGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isActive) "Active Dino" else "Set as Active", color = PrehistoricDark, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Discover via Egg Hatching to Unlock")
                    }
                }
            }
        }
    }
}

@Composable
fun AttributePill(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, fontSize = 9.sp, color = TextMuted)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
