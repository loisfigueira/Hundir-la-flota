package com.lfigueira.hundir_la_flota

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lfigueira.hundir_la_flota.common.client.GameClient
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.UIState
import com.lfigueira.hundir_la_flota.ui.screens.MainMenuScreen
import com.lfigueira.hundir_la_flota.ui.screens.RecordsScreen
import com.lfigueira.hundir_la_flota.ui.screens.SettingsScreen
import com.lfigueira.hundir_la_flota.ui.screens.ShipPlacementScreen
import com.lfigueira.hundir_la_flota.ui.screens.BattleScreen
import com.lfigueira.hundir_la_flota.common.protocol.GameMessage
import com.lfigueira.hundir_la_flota.ui.theme.ModernTheme

@Composable
fun App() {
    // Manual ViewModel creation using factory to support Desktop (AndroidX Lifecycle KMP)
    // This avoids "UnsupportedOperationException: Factory.create(String, CreationExtras) is not implemented"
    // when using the default generic viewModel() call on non-Android targets in some versions.
    val viewModel: GameViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                // Dependency Injection: Create GameClient here or get it from a container
                val gameClient = GameClient() 
                GameViewModel(gameClient)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val notification by viewModel.notification.collectAsState()
    val playerStats by viewModel.playerStats.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    
    ModernTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Contenido principal basado en el estado
                when (uiState) {
                    is UIState.MainMenu -> {
                        MainMenuScreen(viewModel, connectionState)
                    }
                    
                    is UIState.Settings -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateToMainMenu() }
                        )
                    }
                    
                    is UIState.OnlineMenu -> {
                         com.lfigueira.hundir_la_flota.ui.screens.OnlineMenuScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateToMainMenu() }
                        )
                    }
                    
                    is UIState.Matchmaking -> {
                        MatchmakingScreen(viewModel)
                    }
                    
                    is UIState.Lobby -> {
                        val status = (uiState as UIState.Lobby).status
                        com.lfigueira.hundir_la_flota.ui.screens.LobbyScreen(
                            viewModel = viewModel,
                            lobbyState = status
                        )
                    }
                    
                    is UIState.ShipPlacement -> {
                        ShipPlacementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.surrender() } // Or cancel
                        )
                    }
                    
                    is UIState.Battle -> {
                        BattleScreen(viewModel)
                    }
                    
                    is UIState.Records -> {
                        RecordsScreen(viewModel, playerStats, leaderboard)
                    }
                    
                    is UIState.GameOver -> {
                        val result = (uiState as UIState.GameOver).result
                        GameOverScreen(viewModel, result)
                    }
                }
                
                // Snackbar para notificaciones
                notification?.let { message ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearNotification() }) {
                                Text("OK")
                            }
                        }
                    ) {
                        Text(message)
                    }
                }
            }
        }
    }
}

@Composable
fun MatchmakingScreen(viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Buscando partida...", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = { viewModel.cancelMatchmaking() }) {
            Text("Cancelar")
        }
    }
}

@Composable
fun GameOverScreen(
    viewModel: GameViewModel,
    result: GameMessage.Response.GameOver
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val isWinner = result.winnerId == viewModel.myPlayerId
        Text(
            text = if (isWinner) "¡VICTORIA!" else "DERROTA", 
            style = MaterialTheme.typography.displayLarge,
            color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text("Ganador: ${result.winnerId}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        Card(modifier = Modifier.width(400.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("Estadísticas", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                StatRow("Disparos", result.stats.totalShots.toString())
                StatRow("Aciertos", result.stats.successfulHits.toString())
                StatRow("Precisión", "${String.format("%.1f", result.stats.accuracy)}%")
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = { viewModel.navigateToMainMenu() }) { Text("Volver al Menú") }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label); Text(value, color = MaterialTheme.colorScheme.primary)
    }
}