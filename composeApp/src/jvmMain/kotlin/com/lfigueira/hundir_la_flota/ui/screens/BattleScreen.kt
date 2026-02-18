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
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.components.BattleLog
import com.lfigueira.hundir_la_flota.ui.components.SonarGrid
import com.lfigueira.hundir_la_flota.ui.theme.CyberColors
import kotlin.math.min

@Composable
fun BattleScreen(viewModel: GameViewModel) {
    val gameState by viewModel.currentGameState.collectAsState()
    val battleEvents by viewModel.battleEvents.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    
    if (gameState == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CyberColors.NeonBlue)
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
                    listOf(CyberColors.DeepNavy, CyberColors.DarkSpace)
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            
            Column(Modifier.fillMaxSize()) {
                // Military Header
                MilitaryHeader(
                    playerName = "COMANDANTE: ${viewModel.myPlayerId.take(8)}",
                    status = if(isMyTurn) "TU TURNO - SOLICITANDO ATAQUE" else "TURNO ENEMIGO - EVALUANDO DAÑOS",
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
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(4.dp)
                                        .border(1.dp, CyberColors.NeonBlue.copy(alpha = 0.3f))
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberColors.NeonRed),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(CyberColors.NeonRed, Color.Transparent)))
                    ) {
                        Text("ABORTAR MISIÓN", fontSize = 11.sp, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun MilitaryHeader(
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
                style = MaterialTheme.typography.titleSmall,
                color = CyberColors.NeonBlue,
                softWrap = false
            )
            Text(
                text = "ESTADO: $status",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                softWrap = false
            )
        }
        
        Spacer(Modifier.width(16.dp))

        // Digital Timer
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(CyberColors.NeonBlue, Color.Transparent)))
        ) {
             val timerColor = if (timeLeft < 10) CyberColors.NeonRed else CyberColors.NeonGreen
             Text(
                text = "T-MINUS: ${timeLeft.toString().padStart(2, '0')}S",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = timerColor
                ),
                softWrap = false
             )
        }

        Spacer(Modifier.width(16.dp))

        // Connection Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dotColor = if (isConnected) CyberColors.NeonGreen else CyberColors.NeonRed
            Box(Modifier.size(6.dp).background(dotColor, shape = CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isConnected) "P-LINK OK" else "LINK LOST",
                color = dotColor,
                fontSize = 10.sp,
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
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 4.dp),
            fontSize = 10.sp,
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
