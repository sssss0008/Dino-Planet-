package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.core.model.ItemCategory
import com.example.core.model.ItemDefinition
import com.example.core.model.PrehistoricDatabase
import com.example.core.repository.GameRepository
import com.example.ui.theme.*

@Composable
fun InventoryCraftingScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val inventory by repository.inventory.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableStateOf<ItemDefinition?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("inventory_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldenReward)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PrehistoricDark,
                    contentColor = GoldenReward,
                    modifier = Modifier.weight(1f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Backpack", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Crafting", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }
        },
        containerColor = PrehistoricDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (selectedTab == 0) {
                // INVENTORY GRID
                val activeItems = remember(inventory) {
                    PrehistoricDatabase.items.filter { (inventory[it.id] ?: 0) > 0 }
                }

                if (activeItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Your backpack is empty. Forage in the jungle!", color = TextMuted)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 75.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeItems) { item ->
                                val count = inventory[item.id] ?: 0
                                Card(
                                    modifier = Modifier
                                        .size(75.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (selectedItem?.id == item.id) 2.dp else 1.dp,
                                            color = if (selectedItem?.id == item.id) GoldenReward else SurfaceBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedItem = item
                                            GameAudioEngine.playButtonClick()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val icon = when (item.category) {
                                                ItemCategory.FOOD -> Icons.Default.Restaurant
                                                ItemCategory.WATER -> Icons.Default.WaterDrop
                                                ItemCategory.MEDICINE -> Icons.Default.Healing
                                                ItemCategory.EGG -> Icons.Default.Egg
                                                ItemCategory.FOSSIL -> Icons.Default.Pets
                                                else -> Icons.Default.Category
                                            }
                                            Icon(icon, contentDescription = null, tint = GoldenReward, modifier = Modifier.size(24.dp))
                                            Text(
                                                text = item.name.split(" ").first(),
                                                fontSize = 9.sp,
                                                color = TextPrimary,
                                                maxLines = 1
                                            )
                                        }
                                        // Badge
                                        Text(
                                            text = "x$count",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldenReward,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Selected Item Detail Sidebar
                        selectedItem?.let { item ->
                            val count = inventory[item.id] ?: 0
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .fillMaxHeight()
                                    .padding(start = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(item.name, fontWeight = FontWeight.Bold, color = GoldenReward, fontSize = 13.sp)
                                    Text("Owned: $count", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(item.description, fontSize = 10.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.weight(1f))

                                    if (item.category == ItemCategory.FOOD || item.category == ItemCategory.WATER || item.category == ItemCategory.MEDICINE) {
                                        Button(
                                            onClick = {
                                                repository.consumeItem(item.id)
                                                if ((inventory[item.id] ?: 0) <= 1) {
                                                    selectedItem = null
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = JungleGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Consume", color = PrehistoricDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // CRAFTING RECIPES
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PrehistoricDatabase.recipes) { recipe ->
                        val canCraft = recipe.requiredMaterials.all { (mat, needed) -> (inventory[mat] ?: 0) >= needed }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, if (canCraft) JungleGreen else SurfaceBorder, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(recipe.name, fontWeight = FontWeight.Bold, color = GoldenReward, fontSize = 13.sp)
                                    Text(recipe.description, fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        recipe.requiredMaterials.forEach { (mat, needed) ->
                                            val have = inventory[mat] ?: 0
                                            Text(
                                                text = "$mat: $have/$needed",
                                                fontSize = 10.sp,
                                                color = if (have >= needed) JungleGreen else DangerRed
                                            )
                                        }
                                    }
                                }
                                Button(
                                    onClick = {
                                        repository.craftRecipe(recipe)
                                    },
                                    enabled = canCraft,
                                    colors = ButtonDefaults.buttonColors(containerColor = JungleGreen),
                                    modifier = Modifier.testTag("craft_button_${recipe.id}")
                                ) {
                                    Text("Craft", fontWeight = FontWeight.Bold, color = PrehistoricDark, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
