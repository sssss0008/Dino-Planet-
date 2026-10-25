package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.GameAudioEngine
import com.example.core.repository.GameRepository
import com.example.ui.theme.*

data class FossilTile(
    val id: Int,
    val hasBone: Boolean,
    var isRevealed: Boolean = false,
    var isCracked: Boolean = false
)

@Composable
fun FossilExcavationScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    // Generate 5x5 excavation grid with 7 hidden bone fragments
    var tiles by remember {
        mutableStateOf(
            List(25) { id ->
                FossilTile(
                    id = id,
                    hasBone = id in listOf(2, 6, 7, 8, 12, 17, 22) // T-Rex skull pattern
                )
            }
        )
    }

    var chiselsLeft by remember { mutableIntStateOf(16) }
    var bonesFound by remember { mutableIntStateOf(0) }
    var isExcavationComplete by remember { mutableStateOf(false) }

    fun resetExcavation() {
        val boneIndices = (0..24).shuffled().take(7).toSet()
        tiles = List(25) { id ->
            FossilTile(id = id, hasBone = id in boneIndices)
        }
        chiselsLeft = 16
        bonesFound = 0
        isExcavationComplete = false
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
                IconButton(onClick = onBack, modifier = Modifier.testTag("excavation_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldenReward)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "FOSSIL EXCAVATION SITE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldenReward
                    )
                    Text(
                        text = "Brush away rock layers to uncover ancient bones",
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
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = GoldenReward, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tools: $chiselsLeft", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Pets, contentDescription = null, tint = GoldenReward, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bones Found: $bonesFound/7", fontWeight = FontWeight.Bold, color = GoldenReward, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5x5 Rock Matrix Grid
            Column(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF271E18))
                    .border(2.dp, Color(0xFF5D4037), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (row in 0..4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0..4) {
                            val index = row * 5 + col
                            val tile = tiles[index]

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            tile.isRevealed && tile.hasBone -> Color(0xFFFFE082)
                                            tile.isRevealed -> Color(0xFF3E2723)
                                            else -> Color(0xFF5D4037)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (tile.isRevealed && tile.hasBone) GoldenReward else Color(0xFF4E342E),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !tile.isRevealed && chiselsLeft > 0 && !isExcavationComplete) {
                                        chiselsLeft--
                                        val newTiles = tiles.toMutableList()
                                        newTiles[index] = tile.copy(isRevealed = true)
                                        tiles = newTiles

                                        if (tile.hasBone) {
                                            bonesFound++
                                            GameAudioEngine.playCollect()
                                            if (bonesFound >= 7) {
                                                isExcavationComplete = true
                                                repository.addItem("fossil_fragments", 5)
                                                repository.addCurrencies(50, 10)
                                                repository.addXp(120)
                                                GameAudioEngine.playLevelUp()
                                            }
                                        } else {
                                            GameAudioEngine.playButtonClick()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (tile.isRevealed) {
                                    if (tile.hasBone) {
                                        Icon(
                                            imageVector = Icons.Default.Pets,
                                            contentDescription = null,
                                            tint = Color(0xFF5D4037),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    } else {
                                        Text("•", color = Color(0x66FFFFFF), fontSize = 16.sp)
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.GridOn,
                                        contentDescription = null,
                                        tint = Color(0x33FFFFFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Completion Banner or Out of Tools
            AnimatedVisibility(visible = isExcavationComplete) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = JungleGreen.copy(alpha = 0.9f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SKELETON EXCAVATED!", fontWeight = FontWeight.Black, color = PrehistoricDark, fontSize = 15.sp)
                        Text("+5 Fossil Fragments, +50 Fossils, +10 Crystals awarded!", fontSize = 11.sp, color = PrehistoricDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { resetExcavation() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrehistoricDark)
                        ) {
                            Text("Excavate Another Site", color = GoldenReward, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (chiselsLeft <= 0 && !isExcavationComplete) {
                Text(
                    text = "Out of tools! You recovered $bonesFound/7 bone fragments.",
                    color = DangerRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { resetExcavation() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenReward)
                ) {
                    Text("Try Again", color = PrehistoricDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
