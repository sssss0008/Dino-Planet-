package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.core.database.SanctuaryEntity
import com.example.core.model.DinosaurSpecies
import com.example.core.model.StructureType
import com.example.core.repository.GameRepository
import com.example.ui.theme.*

@Composable
fun SanctuaryScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val sanctuaryStructures by repository.sanctuary.collectAsState()
    val inventory by repository.inventory.collectAsState()

    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedStructureToBuild by remember { mutableStateOf<StructureType?>(null) }
    var hatchedSpeciesModal by remember { mutableStateOf<DinosaurSpecies?>(null) }

    val hasEgg = (inventory["common_egg"] ?: 0) > 0 || (inventory["rare_egg"] ?: 0) > 0
    val hasIncubationNest = sanctuaryStructures.any { it.structureType == StructureType.INCUBATION_NEST.name }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("sanctuary_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldenReward)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "DINOSAUR SANCTUARY",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldenReward
                    )
                    Text(
                        text = "Base Building • Egg Incubation • Defense",
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
            // Sanctuary Header & Egg Hatching Banner
            if (hasEgg && hasIncubationNest) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Egg, contentDescription = null, tint = GoldenReward, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Egg Ready to Hatch!", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                Text("Incubation Nest is warm", fontSize = 11.sp, color = JungleGreen)
                            }
                        }
                        Button(
                            onClick = {
                                val eggKey = if ((inventory["rare_egg"] ?: 0) > 0) "rare_egg" else "common_egg"
                                val sp = repository.hatchEgg(eggKey)
                                hatchedSpeciesModal = sp
                                GameAudioEngine.playHatch()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenReward),
                            modifier = Modifier.testTag("hatch_egg_button")
                        ) {
                            Text("Hatch Egg", color = PrehistoricDark, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            Text(
                text = "Sanctuary Base Grid (Tap any plot to build)",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // 5x4 Sanctuary Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D1E17))
                    .border(1.5.dp, JungleGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (row in 0..3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0..4) {
                            val placed = sanctuaryStructures.find { it.gridX == col && it.gridY == row }
                            val isSelected = selectedCell?.first == col && selectedCell?.second == row

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (placed != null) SurfaceElevated
                                        else if (isSelected) JungleGreen.copy(alpha = 0.3f)
                                        else Color(0xFF142921)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) GoldenReward else if (placed != null) JungleGreen else SurfaceBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        GameAudioEngine.playButtonClick()
                                        selectedCell = Pair(col, row)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (placed != null) {
                                    val icon = when (placed.structureType) {
                                        StructureType.BASIC_SHELTER.name -> Icons.Default.Home
                                        StructureType.INCUBATION_NEST.name -> Icons.Default.Egg
                                        StructureType.CAMPFIRE.name -> Icons.Default.LocalFireDepartment
                                        StructureType.STORAGE_BASKET.name -> Icons.Default.Inventory2
                                        StructureType.WATER_BASIN.name -> Icons.Default.Water
                                        StructureType.BONE_BARRICADE.name -> Icons.Default.Shield
                                        else -> Icons.Default.AutoAwesome
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(icon, contentDescription = null, tint = GoldenReward, modifier = Modifier.size(20.dp))
                                        Text("Lv.${placed.level}", fontSize = 8.sp, color = TextSecondary)
                                    }
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0x44FFFFFF), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Panel: Selected Structure info or Build Drawer
            val currentSelected = selectedCell?.let { (x, y) ->
                sanctuaryStructures.find { it.gridX == x && it.gridY == y }
            }

            if (currentSelected != null) {
                // Info & remove
                val type = StructureType.entries.find { it.name == currentSelected.structureType } ?: StructureType.BASIC_SHELTER
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(type.displayName, fontWeight = FontWeight.Bold, color = GoldenReward, fontSize = 14.sp)
                            Text(type.description, fontSize = 11.sp, color = TextSecondary)
                        }
                        IconButton(
                            onClick = {
                                repository.removeStructure(currentSelected.id)
                                selectedCell = null
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = DangerRed)
                        }
                    }
                }
            } else if (selectedCell != null) {
                // Build options list
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Select Structure to Construct:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StructureType.entries) { structType ->
                            val canAfford = structType.baseCost.all { (mat, count) -> (inventory[mat] ?: 0) >= count }
                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, if (canAfford) JungleGreen else SurfaceBorder, RoundedCornerShape(12.dp))
                                    .clickable(enabled = canAfford) {
                                        selectedCell?.let { (x, y) ->
                                            repository.placeStructure(structType, x, y)
                                            selectedCell = null
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = if (canAfford) SurfaceDark else Color(0xFF15221D))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(structType.displayName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    structType.baseCost.forEach { (mat, needed) ->
                                        val have = inventory[mat] ?: 0
                                        Text(
                                            text = "$mat: $have/$needed",
                                            fontSize = 9.sp,
                                            color = if (have >= needed) JungleGreen else DangerRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Hatching celebration dialog
        if (hatchedSpeciesModal != null) {
            val sp = hatchedSpeciesModal!!
            AlertDialog(
                onDismissRequest = { hatchedSpeciesModal = null },
                confirmButton = {
                    Button(
                        onClick = { hatchedSpeciesModal = null },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenReward)
                    ) {
                        Text("Add to Sanctuary", color = PrehistoricDark, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text("NEW SPECIES HATCHED!", fontWeight = FontWeight.Black, color = GoldenReward)
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color(sp.primaryColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = PrehistoricDark, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(sp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text(sp.scientificName, fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(sp.description, fontSize = 12.sp, color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
