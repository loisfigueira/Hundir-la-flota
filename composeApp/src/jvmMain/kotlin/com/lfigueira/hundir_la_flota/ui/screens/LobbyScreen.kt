package com.lfigueira.hundir_la_flota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfigueira.hundir_la_flota.common.protocol.GameMessage
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.theme.ModernColors

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    lobbyState: GameMessage.Matchmaking.LobbyStatus
) {
    // Estado local para el código de sala
    var roomCodeInput by remember { mutableStateOf("") }
    var showingJoinUI by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ModernColors.backgroundDark, ModernColors.backgroundLight)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = ModernColors.cardBackground.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ModernColors.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TÍTULO
                Text(
                    text = if (lobbyState.roomCode != null) "SALA ESTRATÉGICA" else "CENTRAL DE RECLUTAMIENTO",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ModernColors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                // CÓDIGO DE SALA (Si existe)
                if (lobbyState.roomCode != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "ID SALA: ${lobbyState.roomCode}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ModernColors.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Comparte este código con tu amigo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "ID: ${lobbyState.lobbyId.take(8)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.3f)
                )
                
                Spacer(Modifier.height(24.dp))
                
                // SECCIÓN DE UNIRSE (Solo si no estamos ya en una sala con código)
                if (lobbyState.roomCode == null && lobbyState.players.size == 1) { // Solo mostrar si soy el único (asumiendo lógica de entrada)
                     // O mejor: Esta pantalla es cuando YA estamos en LobbyStatus.
                     // Si estamos en LobbyStatus, ya estamos "dentro".
                     // Pero el requerimiento dice: "Pantalla con dos opciones: Crear Sala / Unirse".
                     // Eso debería estar ANTES de entrar al Lobby, quizas en MainMenu o un paso intermedio.
                     // Pero LobbyScreen recibe LobbyStatus, o sea ya estamos dentro.
                     // REVISIÓN: El usuario pidió "Botón Crear Sala / Campo Unirse". 
                     // Esto implica que LobbyScreen debe manejar el estado "Sin Lobby" O 
                     // debemos mover esto a MainMenu / MatchmakingScreen.
                     // Como LobbyScreen recibe LobbyStatus, asume que ya estamos en una sala.
                     // VOY A MODIFICAR MainMenuScreen para tener estas opciones, y LobbyScreen solo muestra el resultado.
                     // PERO, si el usuario quiere que sea LobbyScreen... asumo que rediseñaré MainMenu.
                } 
                
                // Vizualización de Jugadores (Igual que antes)
                Text(
                    text = "RECLUTAS EN POSICIÓN (${lobbyState.players.size}/${lobbyState.maxPlayers})",
                    style = MaterialTheme.typography.labelMedium,
                    color = ModernColors.textSecondary
                )
                
                Spacer(Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lobbyState.players) { player ->
                        PlayerLobbyItem(player)
                    }
                    
                    if (lobbyState.players.size < lobbyState.maxPlayers) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = ModernColors.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Buscando enlace...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ModernColors.primary
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.cancelMatchmaking() },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernColors.error.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("ABORTAR OPERACIÓN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlayerLobbyItem(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ModernColors.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(ModernColors.primary, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(name, color = ModernColors.textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}
