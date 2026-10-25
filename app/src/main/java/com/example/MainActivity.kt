package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.audio.GameAudioEngine
import com.example.core.repository.GameRepository
import com.example.ui.screens.*
import com.example.ui.theme.DinoPlanetTheme
import com.example.ui.theme.GoldenReward
import com.example.ui.theme.PrehistoricDark
import com.example.ui.theme.SurfaceDark

enum class GameScreenState {
    SPLASH,
    MAIN_MENU,
    GAMEPLAY,
    COLLECTION,
    SANCTUARY,
    INVENTORY,
    JOURNAL,
    EXCAVATION,
    MAP,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = GameRepository(applicationContext)

        setContent {
            DinoPlanetTheme {
                var currentScreen by remember { mutableStateOf(GameScreenState.SPLASH) }
                val notification by repository.activeNotification.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(notification) {
                    notification?.let { msg ->
                        snackbarHostState.showSnackbar(
                            message = msg,
                            duration = SnackbarDuration.Short
                        )
                        repository.dismissNotification()
                    }
                }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(16.dp)
                        ) { data ->
                            Snackbar(
                                containerColor = SurfaceDark,
                                contentColor = GoldenReward,
                                snackbarData = data
                            )
                        }
                    },
                    containerColor = PrehistoricDark
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen) {
                            GameScreenState.SPLASH -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        currentScreen = GameScreenState.MAIN_MENU
                                    }
                                )
                            }
                            GameScreenState.MAIN_MENU -> {
                                MainMenuScreen(
                                    repository = repository,
                                    onStartAdventure = {
                                        currentScreen = GameScreenState.GAMEPLAY
                                    },
                                    onOpenCollection = {
                                        currentScreen = GameScreenState.COLLECTION
                                    },
                                    onOpenSanctuary = {
                                        currentScreen = GameScreenState.SANCTUARY
                                    },
                                    onOpenJournal = {
                                        currentScreen = GameScreenState.JOURNAL
                                    },
                                    onOpenExcavation = {
                                        currentScreen = GameScreenState.EXCAVATION
                                    },
                                    onOpenSettings = {
                                        currentScreen = GameScreenState.SETTINGS
                                    }
                                )
                            }
                            GameScreenState.GAMEPLAY -> {
                                GamePlayScreen(
                                    repository = repository,
                                    onOpenInventory = {
                                        currentScreen = GameScreenState.INVENTORY
                                    },
                                    onOpenSanctuary = {
                                        currentScreen = GameScreenState.SANCTUARY
                                    },
                                    onOpenMap = {
                                        currentScreen = GameScreenState.MAP
                                    },
                                    onOpenSettings = {
                                        currentScreen = GameScreenState.SETTINGS
                                    },
                                    onExitToMenu = {
                                        currentScreen = GameScreenState.MAIN_MENU
                                    }
                                )
                            }
                            GameScreenState.COLLECTION -> {
                                CollectionScreen(
                                    repository = repository,
                                    onBack = { currentScreen = GameScreenState.MAIN_MENU }
                                )
                            }
                            GameScreenState.SANCTUARY -> {
                                SanctuaryScreen(
                                    repository = repository,
                                    onBack = { currentScreen = GameScreenState.MAIN_MENU }
                                )
                            }
                            GameScreenState.INVENTORY -> {
                                InventoryCraftingScreen(
                                    repository = repository,
                                    onBack = { currentScreen = GameScreenState.GAMEPLAY }
                                )
                            }
                            GameScreenState.JOURNAL -> {
                                JournalScreen(
                                    repository = repository,
                                    onBack = { currentScreen = GameScreenState.MAIN_MENU }
                                )
                            }
                            GameScreenState.EXCAVATION -> {
                                FossilExcavationScreen(
                                    repository = repository,
                                    onBack = { currentScreen = GameScreenState.MAIN_MENU }
                                )
                            }
                            GameScreenState.MAP -> {
                                WorldMapScreen(
                                    repository = repository,
                                    onBack = { currentScreen = GameScreenState.GAMEPLAY }
                                )
                            }
                            GameScreenState.SETTINGS -> {
                                SettingsScreen(
                                    repository = repository,
                                    onBack = { currentScreen = GameScreenState.MAIN_MENU }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        GameAudioEngine.stopAmbientLoop()
    }

    override fun onResume() {
        super.onResume()
        GameAudioEngine.startAmbientLoop()
    }
}
