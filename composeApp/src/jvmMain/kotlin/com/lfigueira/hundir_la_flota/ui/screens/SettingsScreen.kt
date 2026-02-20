package com.lfigueira.hundir_la_flota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfigueira.hundir_la_flota.common.models.GameConfig
import com.lfigueira.hundir_la_flota.common.models.ShipDefinition
import com.lfigueira.hundir_la_flota.common.protocol.ShipType
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.theme.ModernColors
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val currentConfig by viewModel.currentConfig.collectAsState()
    
    var boardSize by remember { mutableStateOf(currentConfig.boardSize) }
    var turnTime by remember { mutableStateOf(currentConfig.turnTimeSeconds) }
    var maxRounds by remember { mutableStateOf(currentConfig.maxRounds) }
    var useDefaultFleet by remember { mutableStateOf(true) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título
            Text(
                text = "AJUSTES DE SISTEMA",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Panel de configuración
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Tamaño del tablero
                    ConfigSection(title = "Tamaño del Tablero") {
                        SliderWithValue(
                            value = boardSize.toFloat(),
                            onValueChange = { boardSize = it.roundToInt() },
                            valueRange = 7f..14f,
                            steps = 6,
                            label = "${boardSize}x${boardSize}"
                        )
                        Text(
                            text = "Escala operativa: 7x7 a 14x14",
                            fontSize = 12.sp,
                            color = ModernColors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = ModernColors.primary.copy(alpha = 0.15f)
                    )
                    
                    // Tiempo de turno
                    ConfigSection(title = "Tiempo por Turno") {
                        SliderWithValue(
                            value = turnTime.toFloat(),
                            onValueChange = { turnTime = it.roundToInt() },
                            valueRange = 30f..120f,
                            steps = 17,
                            label = "$turnTime segundos"
                        )
                        Text(
                            text = "Ventana de respuesta: 30 a 120s",
                            fontSize = 12.sp,
                            color = ModernColors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = ModernColors.primary.copy(alpha = 0.15f)
                    )
                    
                    // Configuración de flota
                    ConfigSection(title = "ESTRATEGIA") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FleetButton(
                                text = "Nueva (1x5, 2x4, 3x3, 4x2)",
                                selected = useDefaultFleet,
                                onClick = { useDefaultFleet = true }
                            )
                            FleetButton(
                                text = "Clásica (1x4, 2x3, 3x2, 4x1)",
                                selected = !useDefaultFleet,
                                onClick = { useDefaultFleet = false }
                            )
                        }
                        
                        // Mostrar detalles de la flota
                        Spacer(modifier = Modifier.height(12.dp))
                        val fleet = if (useDefaultFleet) GameConfig.DEFAULT_FLEET else GameConfig.CLASSIC_FLEET
                        FleetDetails(fleet)
                    }
                }
            }
            
            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("DESCARTAR", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = {
                        val newConfig = GameConfig(
                            boardSize = boardSize,
                            turnTimeSeconds = turnTime,
                            maxRounds = maxRounds,
                            shipsConfig = if (useDefaultFleet) GameConfig.DEFAULT_FLEET else GameConfig.CLASSIC_FLEET
                        )
                        viewModel.updateConfig(newConfig)
                        onBack()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("GUARDAR Y APLICAR", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp)) // Add some space before the new button
            Button(
                onClick = { viewModel.findPvPMatch() },
                modifier = Modifier
                    .width(320.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("MATCHMAKING OPERATIVO (PvP)", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConfigSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun SliderWithValue(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    label: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun RoundButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 100.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(if (selected) 4.dp else 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun RowScope.FleetButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .weight(1f),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(if (selected) 4.dp else 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            softWrap = false
        )
    }
}

@Composable
private fun FleetDetails(fleet: List<ShipDefinition>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "CARACTERÍSTICAS OPERATIVAS:",
                style = MaterialTheme.typography.labelSmall,
                color = ModernColors.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            fleet.forEach { shipDef ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${shipDef.count}x ${getShipName(shipDef.type)}",
                        fontSize = 13.sp,
                        color = ModernColors.textPrimary
                    )
                    Text(
                        text = "[${shipDef.type.size} SECTORES]",
                        fontSize = 11.sp,
                        color = ModernColors.textSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ModernColors.primary.copy(alpha = 0.1f)
            )
            Text(
                text = "TOTAL: ${fleet.sumOf { it.count }} UNIDADES",
                style = MaterialTheme.typography.labelSmall,
                color = ModernColors.primary
            )
        }
    }
}

private fun getShipName(type: ShipType): String = when (type) {
    ShipType.CARRIER -> "Portaaviones"
    ShipType.BATTLESHIP -> "Acorazado"
    ShipType.CRUISER -> "Crucero"
    ShipType.DESTROYER -> "Destructor"
    ShipType.PATROL_BOAT -> "Patrullera"
}
