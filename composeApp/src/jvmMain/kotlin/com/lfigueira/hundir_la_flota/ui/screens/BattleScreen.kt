package com.lfigueira.hundir_la_flota.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfigueira.hundir_la_flota.common.protocol.*
import com.lfigueira.hundir_la_flota.common.client.ConnectionState
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.components.BattleLog
import com.lfigueira.hundir_la_flota.ui.components.SonarGrid
import com.lfigueira.hundir_la_flota.ui.theme.ModernColors
import kotlin.math.min

/**
 * Pantalla principal de la fase de batalla.
 * Muestra el radar enemigo, la flota propia en un minimapa y el registro de eventos.
 * @param viewModel Instancia del ViewModel para observar el estado del juego.
 */
@Composable
fun BattleScreen(viewModel: GameViewModel) {
    val gameState by viewModel.currentGameState.collectAsState()
    val battleEvents by viewModel.battleEvents.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    
    if (gameState == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ModernColors.primary)
        }
        return
    }
    
    val state = gameState!!
    val isMyTurn = state.currentTurn == viewModel.myPlayerId
    
    // Background Gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            
            Column(Modifier.fillMaxSize()) {
                // Tactical Header
                TacticalHeader(
                    playerName = "OPERADOR: ${viewModel.myPlayerId.take(8)}",
                    status = if(isMyTurn) "ORDENANDO FUEGO" else "ESPERANDO CONFIRMACIÓN",
                    timeLeft = state.turnTimeSeconds,
                    isConnected = connState.toString().contains("Connected")
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(Modifier.weight(1f)) {
                    // Battle Log (20% weight) - Estrecho y táctico
                    BattleLog(
                        events = battleEvents,
                        modifier = Modifier.weight(0.2f).fillMaxHeight()
                    )
                    
                    Spacer(Modifier.width(24.dp))
                    
                    // Boards Container (80% weight)
                    BoxWithConstraints(modifier = Modifier.weight(0.8f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        val availableWidth = maxWidth
                        val availableHeight = maxHeight
                        
                        // 1. Fuente de Verdad Absoluta: El tamaño viene directamente del servidor
                        // Usamos la configuración actual como fallback definitivo si el estado aún no tiene celdas
                        val boardSize = state.opponentBoard.cells.size.takeIf { it > 0 } 
                            ?: viewModel.currentConfig.value.boardSize
                        
                        // 2. Cálculo de Tamaños Relativos
                        // El Radar Enemigo es el principal. El minimapa es un overlay.
                        val mainBoardScreenSize = min(availableWidth.value, availableHeight.value).dp * 0.88f
                        val minimapSize = mainBoardScreenSize * 0.28f

                        Box(modifier = Modifier.fillMaxSize()) {
                            // CAPA 1: RADAR ENEMIGO (Principal)
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                BoardContainer(label = "RADAR ENEMIGO (1:${boardSize})", size = mainBoardScreenSize) {
                                    // LA CLAVE: key(boardSize) obliga a Compose a reconstruir el componente
                                    // si el número de celdas cambia, eliminando cualquier rastro del 10x10.
                                    key(boardSize) {
                                        SonarGrid(
                                            boardState = state.opponentBoard,
                                            isRadar = true,
                                            onCellClick = { coord ->
                                                if (isMyTurn) viewModel.sendAttack(coord)
                                            },
                                            modifier = Modifier.size(mainBoardScreenSize)
                                        )
                                    }
                                }
                            }

                            // CAPA 2: MI FLOTA (Minimap Overlay)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 12.dp, end = 12.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(ModernColors.surface.copy(alpha = 0.9f))
                                        .padding(4.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    BoardContainer(label = "MI SECTOR", size = minimapSize) {
                                        key(boardSize) {
                                            SonarGrid(
                                                boardState = state.myBoard,
                                                isRadar = false,
                                                onCellClick = {},
                                                modifier = Modifier.size(minimapSize)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { viewModel.endSession() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.error, Color.Transparent)))
                    ) {
                        Text("ABORTAR MISIÓN", fontSize = 11.sp, softWrap = false)
                    }
                }
            }
        }
    }
}

/**
 * Cabecera táctica que muestra información del jugador, estado del sistema y tiempo restante.
 * @param playerName Identificador visual del operador.
 * @param status Mensaje de estado de la operación actual.
 * @param timeLeft Segundos restantes para el turno.
 * @param isConnected Estado del enlace de datos con el servidor.
 */
@Composable
fun TacticalHeader(
    playerName: String,
    status: String,
    timeLeft: Int,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = playerName,
                style = MaterialTheme.typography.titleMedium,
                color = ModernColors.primary,
                softWrap = false
            )
            Text(
                text = "SYSTEM: $status",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                softWrap = false
            )
        }
        
        Spacer(Modifier.width(16.dp))

        // Amber Timer
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), Color.Transparent)))
        ) {
             val timerColor = if (timeLeft < 10) ModernColors.error else ModernColors.secondary
             Text(
                text = "T-REM: ${timeLeft.toString().padStart(2, '0')}s",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = timerColor
                ),
                softWrap = false
             )
        }

        Spacer(Modifier.width(20.dp))

        // Connection Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dotColor = if (isConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            Box(Modifier.size(8.dp).background(dotColor, shape = CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isConnected) "LINK ONLINE" else "LINK OFFLINE",
                color = dotColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false
            )
        }
    }
}

@Composable
fun BoardContainer(label: String, size: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
            softWrap = false
        )
        Box(
            modifier = Modifier
                .size(size)
                .aspectRatio(1f)
        ) {
            content()
        }
    }
}
