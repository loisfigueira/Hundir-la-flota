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
import com.lfigueira.hundir_la_flota.ui.theme.CyberColors

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
                    listOf(CyberColors.DeepNavy, Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = CyberColors.DarkSpace.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberColors.NeonBlue)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TÍTULO
                Text(
                    text = if (lobbyState.roomCode != null) "SALA PRIVADA" else "SALA DE ESPERA",
                    style = MaterialTheme.typography.titleLarge,
                    color = CyberColors.NeonBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                // CÓDIGO DE SALA (Si existe)
                if (lobbyState.roomCode != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "CÓDIGO: ${lobbyState.roomCode}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = CyberColors.NeonGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
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
                    text = "JUGADORES CONECTADOS (${lobbyState.players.size}/${lobbyState.maxPlayers})",
                    color = CyberColors.textPrimary,
                    fontSize = 14.sp
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
                                    modifier = Modifier.size(20.dp),
                                    color = CyberColors.NeonGreen,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Esperando oponente...",
                                    color = CyberColors.NeonGreen,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 30.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.cancelMatchmaking() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberColors.NeonRed.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberColors.NeonRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SALIR DE LA SALA", color = CyberColors.NeonRed)
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
            .background(CyberColors.NeonBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder Icon (Box) to avoid missing dependency issues
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(CyberColors.NeonBlue, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}
