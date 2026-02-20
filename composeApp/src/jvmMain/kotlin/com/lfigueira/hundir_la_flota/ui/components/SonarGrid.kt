package com.lfigueira.hundir_la_flota.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfigueira.hundir_la_flota.common.protocol.*
import com.lfigueira.hundir_la_flota.generated.resources.Res
import com.lfigueira.hundir_la_flota.generated.resources.hit_marker
import com.lfigueira.hundir_la_flota.ui.theme.ModernColors
import org.jetbrains.compose.resources.painterResource

@Composable
fun SonarGrid(
    boardState: BoardState,
    isRadar: Boolean,
    onCellClick: (Coordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isRadar) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val boardSize = boardState.size
    
    // Contenedor principal
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(1.dp)
    ) {
        // Cálculo de celda dinámico - FUERZA EL TAMAÑO SEGÚN EL ANCHO DISPONIBLE
        // val availableSize = minOf(maxWidth, maxHeight) // Eliminamos el minOf para seguir la instrucción verbatim
        val cellSize = maxWidth / boardSize
        
        // Ajustamos grosores y tamaños de fuente según el tamaño de celda resultante
        val lineThickness = if (cellSize < 30.dp) 0.5.dp else 1.dp
        val fontSize = (cellSize.value / 2.5f).sp
        val iconScale = if (cellSize < 30.dp) 0.8f else 1f
        
        // Scroll de emergencia si la celda es demasiado pequeña para ser usable (ej < 15dp)
        // Aunque intentaremos que encaje, esto previene colapso total en pantallas diminutas
        val availableSize = maxWidth
        val contentModifier = if (cellSize < 15.dp) {
             Modifier
                .size((20.dp * boardSize)) // Forzamos mínimo usable
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        } else {
             Modifier.size(availableSize)
        }

        Box(modifier = contentModifier) {
            Column {
                for (y in 0 until boardSize) {
                    Row(modifier = Modifier.height(cellSize)) {
                        for (x in 0 until boardSize) {
                            val coord = Coordinate(x, y)
                            val rowIndex = if (y < boardState.cells.size) boardState.cells[y] else emptyList()
                            val cellState = if (x < rowIndex.size) rowIndex[x] else CellState.EMPTY
                            
                            SonarCell(
                                coord = coord,
                                state = cellState,
                                isRadar = isRadar,
                                accentColor = accentColor,
                                ships = boardState.ships,
                                onClick = { onCellClick(coord) },
                                modifier = Modifier
                                    .size(cellSize)
                                    .border(lineThickness, accentColor.copy(alpha = 0.1f)),
                                fontSize = fontSize,
                                iconScale = iconScale
                            )
                        }
                    }
                }
            }
            
            // Efecto de barrido (Sonar scan line)
            if (isRadar) {
                val infiniteTransition = rememberInfiniteTransition()
                val scanY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val yPos = scanY * size.height
                    drawLine(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.7f to accentColor.copy(alpha = 0.3f),
                            1f to accentColor.copy(alpha = 0.6f)
                        ),
                        start = Offset(0f, yPos),
                        end = Offset(size.width, yPos),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
fun SonarCell(
    coord: Coordinate,
    state: CellState,
    isRadar: Boolean,
    accentColor: Color,
    ships: List<ShipState>?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit,
    iconScale: Float
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val backgroundColor by animateColorAsState(
        if (isHovered) accentColor.copy(alpha = 0.1f) else Color.Transparent
    )
    
    // Animation for hits
    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by if (state == CellState.HIT) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            CellState.HIT -> {
                Box(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f * blinkAlpha))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f * blinkAlpha))
                )
                Image(
                    painter = painterResource(Res.drawable.hit_marker),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(0.65f * iconScale)
                        .graphicsLayer {
                            alpha = blinkAlpha
                            scaleX = 0.9f + (0.1f * blinkAlpha)
                            scaleY = 0.9f + (0.1f * blinkAlpha)
                        }
                )
            }
            CellState.MISS -> {
                // Wave ripple effect
                val rippleScale by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000),
                        repeatMode = RepeatMode.Restart
                    )
                )
                val rippleColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize(0.7f * iconScale)) {
                    drawCircle(
                        color = rippleColor.copy(alpha = 0.4f * (1f - rippleScale)),
                        radius = (size.minDimension / 2) * rippleScale,
                        style = Stroke(width = (size.minDimension / 20))
                    )
                }
            }
            else -> {
                // Specific for Player Board: Show Ships
                if (!isRadar && ships != null) {
                    val shipAtTail = ships.find { it.coordinates.contains(coord) }
                    if (shipAtTail != null) {
                        // Drawing metallic ship part
                        Box(
                            Modifier.fillMaxSize(0.85f * iconScale)
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                .graphicsLayer {
                                    shadowElevation = 4f
                                    clip = true
                                }
                        )
                    }
                }
            }
        }
        
        // Debug coord display (subtle)
        if (isHovered) {
            Text(
                text = "${coord.x},${coord.y}",
                color = accentColor.copy(alpha = 0.6f),
                fontSize = fontSize * 0.55f, // Coordenadas más pequeñas
                modifier = Modifier.align(Alignment.BottomEnd).padding(1.dp)
            )
        }
    }
}
