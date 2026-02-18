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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfigueira.hundir_la_flota.common.models.GameConfig
import com.lfigueira.hundir_la_flota.common.models.ShipDefinition
import com.lfigueira.hundir_la_flota.common.protocol.ShipType
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.theme.CyberColors
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
                        CyberColors.backgroundDark,
                        CyberColors.backgroundLight
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
                text = "⚙️ CONFIGURACIÓN",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = CyberColors.cyan,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Panel de configuración
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CyberColors.cardBackground
                )
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
                            text = "Rango: 7x7 a 14x14",
                            fontSize = 12.sp,
                            color = CyberColors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = CyberColors.cyan.copy(alpha = 0.3f)
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
                            text = "Rango: 30 a 120 segundos",
                            fontSize = 12.sp,
                            color = CyberColors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = CyberColors.cyan.copy(alpha = 0.3f)
                    )
                    
                    // Número de rondas
                    ConfigSection(title = "Número de Rondas") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RoundButton(
                                text = "1",
                                selected = maxRounds == 1,
                                onClick = { maxRounds = 1 }
                            )
                            RoundButton(
                                text = "Best of 3",
                                selected = maxRounds == 3,
                                onClick = { maxRounds = 3 }
                            )
                            RoundButton(
                                text = "Best of 5",
                                selected = maxRounds == 5,
                                onClick = { maxRounds = 5 }
                            )
                        }
                    }
                    
                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = CyberColors.cyan.copy(alpha = 0.3f)
                    )
                    
                    // Configuración de flota
                    ConfigSection(title = "Configuración de Flota") {
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
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberColors.error.copy(alpha = 0.8f)
                    )
                ) {
                    Text("Cancelar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberColors.cyan
                    )
                ) {
                    Text("💾 Guardar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
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
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = CyberColors.cyan,
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
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyberColors.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = CyberColors.cyan,
                activeTrackColor = CyberColors.cyan,
                inactiveTrackColor = CyberColors.cyan.copy(alpha = 0.3f)
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
            .height(48.dp)
            .widthIn(min = 100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) CyberColors.cyan else CyberColors.cardBackground,
            contentColor = if (selected) CyberColors.backgroundDark else CyberColors.textPrimary
        ),
        elevation = if (selected) ButtonDefaults.buttonElevation(8.dp) else ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun FleetButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(56.dp)
            .widthIn(min = 150.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) CyberColors.cyan else CyberColors.cardBackground,
            contentColor = if (selected) CyberColors.backgroundDark else CyberColors.textPrimary
        ),
        elevation = if (selected) ButtonDefaults.buttonElevation(8.dp) else ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun FleetDetails(fleet: List<ShipDefinition>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberColors.backgroundDark.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Detalles de la Flota:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyberColors.cyan,
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
                        color = CyberColors.textPrimary
                    )
                    Text(
                        text = "(${shipDef.type.size} casillas)",
                        fontSize = 13.sp,
                        color = CyberColors.textSecondary
                    )
                }
            }
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = CyberColors.cyan.copy(alpha = 0.2f)
            )
            Text(
                text = "Total: ${fleet.sumOf { it.count }} barcos",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyberColors.cyan
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
