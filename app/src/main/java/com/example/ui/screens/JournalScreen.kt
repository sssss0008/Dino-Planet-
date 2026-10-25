package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.core.model.Rarity
import com.example.core.repository.GameRepository
import com.example.ui.theme.*

@Composable
fun JournalScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val quests by repository.quests.collectAsState()
    val achievements by repository.achievements.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("journal_back_button")) {
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
                        text = { Text("Quests", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Achievements", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
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
                // QUESTS LIST
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quests) { quest ->
                        val pct = (quest.currentProgress.toFloat() / quest.targetCount.toFloat()).coerceIn(0f, 1f)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (quest.isCompleted && !quest.isClaimed) 1.5.dp else 1.dp,
                                    color = if (quest.isClaimed) SurfaceBorder else if (quest.isCompleted) GoldenReward else JungleGreen.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (quest.isClaimed) Color(0xFF14241E) else SurfaceDark
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = quest.title,
                                        fontWeight = FontWeight.Bold,
                                        color = if (quest.isClaimed) TextMuted else GoldenReward,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${quest.currentProgress}/${quest.targetCount}",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = quest.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Progress Bar
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (quest.isCompleted) GoldenReward else JungleGreen,
                                    trackColor = Color(0x33FFFFFF)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rewards: +${quest.xpReward} XP, +${quest.fossilReward} Fossils",
                                        fontSize = 10.sp,
                                        color = GoldenReward
                                    )

                                    if (quest.isCompleted && !quest.isClaimed) {
                                        Button(
                                            onClick = { repository.claimQuestReward(quest.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldenReward),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("claim_quest_${quest.id}")
                                        ) {
                                            Text("Claim", color = PrehistoricDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    } else if (quest.isClaimed) {
                                        Text("Claimed", fontSize = 11.sp, color = JungleGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ACHIEVEMENTS LIST
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(achievements) { ach ->
                        val pct = (ach.currentProgress.toFloat() / ach.targetProgress.toFloat()).coerceIn(0f, 1f)
                        val tierColor = when (ach.tier) {
                            Rarity.COMMON -> RarityCommon
                            Rarity.UNCOMMON -> RarityUncommon
                            Rarity.RARE -> RarityRare
                            Rarity.EPIC -> RarityEpic
                            Rarity.LEGENDARY -> RarityLegendary
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, if (ach.isCompleted) tierColor else SurfaceBorder, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ach.title, fontWeight = FontWeight.Bold, color = tierColor, fontSize = 14.sp)
                                    Text("${ach.currentProgress}/${ach.targetProgress}", fontSize = 11.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(ach.description, fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = tierColor,
                                    trackColor = Color(0x33FFFFFF)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("+${ach.crystalReward} Crystals", fontSize = 11.sp, color = SkyBlue, fontWeight = FontWeight.Bold)

                                    if (ach.isCompleted && !ach.isClaimed) {
                                        Button(
                                            onClick = { repository.claimAchievementReward(ach.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                        ) {
                                            Text("Claim", color = PrehistoricDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    } else if (ach.isClaimed) {
                                        Text("Completed", fontSize = 11.sp, color = JungleGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
