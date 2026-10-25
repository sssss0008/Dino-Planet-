package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.GameAudioEngine
import com.example.core.repository.GameRepository
import com.example.ui.theme.*

data class BiomeRegion(
    val name: String,
    val dangerLevel: String,
    val dangerColor: Color,
    val description: String,
    val xRatio: Float,
    val yRatio: Float,
    val isDiscovered: Boolean = true
)

@Composable
fun WorldMapScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val biomes = listOf(
        BiomeRegion("Verdant Valley", "Low Danger", JungleGreen, "Lush starting clearing with fresh berry bushes and river springs.", 0.28f, 0.45f),
        BiomeRegion("Crystal Caves", "Medium Danger", SkyBlue, "Subterranean caverns glowing with crystalline minerals.", 0.5f, 0.28f),
        BiomeRegion("Ember Volcano", "Extreme Danger", VolcanicOrange, "Volcanic peaks prone to ash tremors and lava vents.", 0.78f, 0.35f),
        BiomeRegion("Ancient Forest", "Medium Danger", Color(0xFF2E7D32), "Dense conifer groves inhabited by apex carnosaur packs.", 0.35f, 0.75f),
        BiomeRegion("Misty Swampland", "High Danger", Color(0xFF00897B), "Murky waters home to semi-aquatic Spinosaurs.", 0.65f, 0.78f)
    )

    var selectedBiome by remember { mutableStateOf(biomes.first()) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("map_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldenReward)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "PREHISTORIC ISLAND MAP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldenReward
                    )
                    Text(
                        text = "Surveyed Biomes, Landmarks & Survival Outposts",
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
            // Illustrated Map Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F261E))
                    .border(2.dp, Color(0xFF8D6E63), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Island continent shape
                    drawCircle(color = Color(0xFF1B3D30), radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f))

                    // River running across island
                    drawLine(
                        color = Color(0xFF00ACC1),
                        start = Offset(w * 0.45f, h * 0.15f),
                        end = Offset(w * 0.52f, h * 0.85f),
                        strokeWidth = 14f
                    )

                    // Volcanic mountain crater
                    drawCircle(color = Color(0xFFBF360C), radius = 35f, center = Offset(w * 0.78f, h * 0.35f))
                    drawCircle(color = Color(0xFFFF7043), radius = 18f, center = Offset(w * 0.78f, h * 0.35f))

                    // Caves
                    drawCircle(color = Color(0xFF0097A7), radius = 28f, center = Offset(w * 0.5f, h * 0.28f))
                }

                // Interactive Biome Pins
                biomes.forEach { b ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Button(
                            onClick = {
                                selectedBiome = b
                                GameAudioEngine.playButtonClick()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedBiome.name == b.name) GoldenReward else b.dangerColor
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(
                                    x = (b.xRatio * 320).dp,
                                    y = (b.yRatio * 200).dp
                                )
                        ) {
                            Text(
                                text = b.name.split(" ").first(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrehistoricDark
                            )
                        }
                    }
                }
            }

            // Selected Biome Details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedBiome.name, fontWeight = FontWeight.Bold, color = GoldenReward, fontSize = 15.sp)
                        Text(
                            selectedBiome.dangerLevel,
                            color = selectedBiome.dangerColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(selectedBiome.description, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}
