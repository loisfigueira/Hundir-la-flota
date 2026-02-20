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
import com.lfigueira.hundir_la_flota.ui.theme.ModernColors
import kotlin.math.min

/**
 * Pantalla para la fase inicial de colocación de barcos.
 * Permite al usuario posicionar su flota antes de que comience el combate.
 * @param viewModel Instancia del ViewModel para gestionar la confirmación del despliegue.
 * @param onNavigateBack Callback para regresar al menú anterior.
 */
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
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val isPortrait = maxWidth < maxHeight * 0.8f
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val notification by viewModel.notification.collectAsState()
                
                Text(
                    "DESPLIEGUE ESTRATÉGICO",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                if (notification?.startsWith("Error") == true) {
                    Text(
                        text = notification!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Text(
                        "POSICIONA TUS UNIDADES EN EL SECTOR TÁCTICO",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
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

/**
 * Área del tablero interactiva para colocar o remover barcos.
 * @param placements Lista actual de barcos posicionados.
 * @param selectedShipType Tipo de barco seleccionado para colocar.
 * @param currentOrientation Orientación (H/V) para el siguiente barco.
 * @param currentCounts Cantidad de barcos que faltan por colocar por tipo.
 * @param viewModel ViewModel para acceder a la configuración.
 * @param boardSize Dimensión del tablero.
 * @param onPlacementsChange Callback disparado al actualizar la lista de posiciones.
 */
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
            "RESERVAS DISPONIBLES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(0.4f).height(44.dp),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(if(currentOrientation == Orientation.HORIZONTAL) "EJE: H" else "EJE: V", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Button(
                onClick = { viewModel.confirmDeployment(placements) },
                enabled = isFleetComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFleetComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isFleetComplete) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(0.6f).height(44.dp)
            ) {
                Text(
                    if (isFleetComplete) "INICIAR COMBATE" else "FLOTA INCOMPLETA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
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
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

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
        val textColor = if (remaining > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        Text("${type.name} [${type.size}S]", color = textColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), softWrap = false)
        Spacer(Modifier.width(8.dp))
        Text("x$remaining", color = if (remaining > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Calcula todas las coordenadas ocupadas por un barco según su posición, tipo y orientación.
 * @param ship Definición del barco y su origen.
 * @return Lista de coordenadas (x, y) que ocupa el barco.
 */
fun getShipCoordinates(ship: ShipPlacement): List<Coordinate> {
    val coords = mutableListOf<Coordinate>()
    for (i in 0 until ship.type.size) {
        val x = if (ship.orientation == Orientation.HORIZONTAL) ship.coordinate.x + i else ship.coordinate.x
        val y = if (ship.orientation == Orientation.VERTICAL) ship.coordinate.y + i else ship.coordinate.y
        coords.add(Coordinate(x, y))
    }
    return coords
}

/**
 * Valida si un barco puede colocarse en una posición determinada sin solaparse ni salirse del tablero.
 * @param newShip El barco que se intenta colocar.
 * @param existingShips Lista de barcos ya colocados.
 * @param boardSize Tamaño del tablero.
 * @return True si la posición es legal.
 */
fun isValidPlacement(newShip: ShipPlacement, existingShips: List<ShipPlacement>, boardSize: Int = 10): Boolean {
    val newCoords = getShipCoordinates(newShip)
    if (newCoords.any { it.x !in 0 until boardSize || it.y !in 0 until boardSize }) return false
    val occupied = existingShips.flatMap { getShipCoordinates(it) }.toSet()
    if (newCoords.any { occupied.contains(it) }) return false
    return true
}
