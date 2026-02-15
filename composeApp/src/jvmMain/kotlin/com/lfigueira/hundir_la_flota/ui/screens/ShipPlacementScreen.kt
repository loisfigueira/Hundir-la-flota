package com.lfigueira.hundir_la_flota.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.lfigueira.hundir_la_flota.common.protocol.*
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.components.SonarGrid
import com.lfigueira.hundir_la_flota.ui.theme.CyberColors
import kotlin.math.min

@Composable
fun ShipPlacementScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    var placements by remember { mutableStateOf(emptyList<ShipPlacement>()) }
    var selectedShipType by remember { mutableStateOf<ShipType?>(null) }
    var currentOrientation by remember { mutableStateOf(Orientation.HORIZONTAL) }
    
    val currentConfig by viewModel.currentConfig.collectAsState()
    val boardSize = currentConfig.boardSize
    
    // Construir requiredCounts dinámicamente desde la configuración
    val requiredCounts = remember(currentConfig) {
        currentConfig.shipsConfig.associate { it.type to it.count }
    }
    
    val currentCounts = remember(placements, requiredCounts) {
        val placed = placements.groupingBy { it.type }.eachCount()
        requiredCounts.mapValues { (type, total) -> total - (placed[type] ?: 0) }
    }
    
    val isFleetComplete = currentCounts.all { it.value == 0 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CyberColors.DeepNavy, CyberColors.DarkSpace)))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val isPortrait = maxWidth < maxHeight * 0.8f
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val notification by viewModel.notification.collectAsState()
                
                Text(
                    "DESPLIEGUE DE FLOTA",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                if (notification?.startsWith("Error") == true) {
                    Text(
                        text = notification!!,
                        color = CyberColors.NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Text(
                        "POSICIONA TUS ACTIVOS EN EL SECTOR AZUL",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isPortrait) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                            PlacementGridArea(placements, selectedShipType, currentOrientation, currentCounts, viewModel, boardSize) { placements = it }
                        }
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.weight(0.8f)) {
                            InventoryPanel(requiredCounts, currentCounts, selectedShipType, currentOrientation, isFleetComplete, viewModel, placements, { selectedShipType = it }, { currentOrientation = it })
                        }
                    }
                } else {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                        Box(modifier = Modifier.weight(1.2f).aspectRatio(1f)) {
                            PlacementGridArea(placements, selectedShipType, currentOrientation, currentCounts, viewModel, boardSize) { placements = it }
                        }
                        
                        Spacer(modifier = Modifier.width(32.dp))
                        
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            InventoryPanel(requiredCounts, currentCounts, selectedShipType, currentOrientation, isFleetComplete, viewModel, placements, { selectedShipType = it }, { currentOrientation = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlacementGridArea(
    placements: List<ShipPlacement>,
    selectedShipType: ShipType?,
    currentOrientation: Orientation,
    currentCounts: Map<ShipType, Int>,
    viewModel: GameViewModel,
    boardSize: Int,
    onPlacementsChange: (List<ShipPlacement>) -> Unit
) {
    SonarGrid(
        boardState = BoardState(
            cells = List(boardSize) { List(boardSize) { CellState.EMPTY } },
            ships = placements.map { ship -> 
                ShipState(ship.type, getShipCoordinates(ship), false)
            },
            size = boardSize
        ),
        isRadar = false,
        onCellClick = { coord ->
             val shipAtCell = placements.find { ship -> getShipCoordinates(ship).contains(coord) }
             if (shipAtCell != null) {
                 onPlacementsChange(placements - shipAtCell)
             } else if (selectedShipType != null) {
                 val type = selectedShipType
                 if (currentCounts[type] ?: 0 > 0) {
                     val newShip = ShipPlacement(type, coord, currentOrientation)
                     if (isValidPlacement(newShip, placements, boardSize)) {
                         onPlacementsChange(placements + newShip)
                     }
                 }
             }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun InventoryPanel(
    requiredCounts: Map<ShipType, Int>,
    currentCounts: Map<ShipType, Int>,
    selectedShipType: ShipType?,
    currentOrientation: Orientation,
    isFleetComplete: Boolean,
    viewModel: GameViewModel,
    placements: List<ShipPlacement>,
    onSelectShip: (ShipType) -> Unit,
    onRotate: (Orientation) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "INVENTARIO TÁCTICO",
            style = MaterialTheme.typography.titleSmall,
            color = CyberColors.NeonBlue
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            requiredCounts.forEach { (type, total) ->
                val remaining = currentCounts[type] ?: 0
                val isSelected = selectedShipType == type
                
                InventoryItem(
                    type = type,
                    remaining = remaining,
                    isSelected = isSelected,
                    onClick = { onSelectShip(type) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { 
                    onRotate(if (currentOrientation == Orientation.HORIZONTAL) 
                        Orientation.VERTICAL else Orientation.HORIZONTAL)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberColors.MetallicGray),
                modifier = Modifier.weight(0.4f).height(40.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(if(currentOrientation == Orientation.HORIZONTAL) "ROTAR: H" else "ROTAR: V", fontSize = 10.sp)
            }
            
            Button(
                onClick = { viewModel.confirmDeployment(placements) },
                enabled = isFleetComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFleetComplete) CyberColors.NeonGreen else CyberColors.MetallicGray,
                    contentColor = Color.Black
                ),
                modifier = Modifier.weight(0.6f).height(40.dp)
            ) {
                Text(
                    if (isFleetComplete) "ORDENAR DESPLIEGUE" else "FALTA FLOTA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun InventoryItem(
    type: ShipType,
    remaining: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(if (isSelected) CyberColors.NeonBlue else Color.Transparent)
    val bgColor = if (isSelected) CyberColors.NeonBlue.copy(alpha = 0.1f) else CyberColors.DarkSpace.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .background(bgColor, RoundedCornerShape(4.dp))
            .clickable(enabled = remaining > 0, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val textColor = if (remaining > 0) Color.White else Color.Gray
        Text("${type.name} [${type.size}U]", color = textColor, fontSize = 11.sp, modifier = Modifier.weight(1f), softWrap = false)
        Spacer(Modifier.width(8.dp))
        Text("x$remaining", color = if (remaining > 0) CyberColors.NeonGreen else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

fun getShipCoordinates(ship: ShipPlacement): List<Coordinate> {
    val coords = mutableListOf<Coordinate>()
    for (i in 0 until ship.type.size) {
        val x = if (ship.orientation == Orientation.HORIZONTAL) ship.coordinate.x + i else ship.coordinate.x
        val y = if (ship.orientation == Orientation.VERTICAL) ship.coordinate.y + i else ship.coordinate.y
        coords.add(Coordinate(x, y))
    }
    return coords
}

fun isValidPlacement(newShip: ShipPlacement, existingShips: List<ShipPlacement>, boardSize: Int = 10): Boolean {
    val newCoords = getShipCoordinates(newShip)
    if (newCoords.any { it.x !in 0 until boardSize || it.y !in 0 until boardSize }) return false
    val occupied = existingShips.flatMap { getShipCoordinates(it) }.toSet()
    if (newCoords.any { occupied.contains(it) }) return false
    return true
}
