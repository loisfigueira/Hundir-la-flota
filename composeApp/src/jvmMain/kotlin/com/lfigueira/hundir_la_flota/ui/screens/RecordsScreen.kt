package com.lfigueira.hundir_la_flota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lfigueira.hundir_la_flota.common.protocol.LeaderboardEntry
import com.lfigueira.hundir_la_flota.common.protocol.PlayerStats
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.theme.ModernColors

@Composable
fun RecordsScreen(
    viewModel: GameViewModel,
    playerStats: PlayerStats?,
    leaderboard: List<LeaderboardEntry>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ModernColors.backgroundDark)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CENTRAL DE DATOS",
                            style = MaterialTheme.typography.headlineLarge,
                            color = ModernColors.primary
                        )
                        Text(
                            text = "REGISTROS TÁCTICOS Y RENDIMIENTO",
                            style = MaterialTheme.typography.bodySmall,
                            color = ModernColors.textSecondary
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.navigateToMainMenu() },
                        border = androidx.compose.foundation.BorderStroke(1.dp, ModernColors.primary.copy(alpha = 0.5f))
                    ) {
                        Text("SALIR AL NEXO", color = ModernColors.primary)
                    }
                }
            }

            // Stats Section
            if (playerStats != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "PERFIL OPERATIVO: ${playerStats.playerName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ModernColors.secondary
                        )
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatsCard(
                                title = "GENERAL",
                                modifier = Modifier.weight(1f),
                                color = ModernColors.primary
                            ) {
                                StatGridItem("Despliegues", playerStats.gamesPlayed.toString())
                                StatGridItem("Carrera W/L", "${playerStats.gamesWon} / ${playerStats.gamesLost}")
                                StatGridItem("Precisión Global", "${String.format("%.1f", playerStats.averageAccuracy)}%")
                            }
                            
                            StatsCard(
                                title = "COMBATE PVP / PVE",
                                modifier = Modifier.weight(1f),
                                color = ModernColors.secondary
                            ) {
                                StatGridItem("PvP Victoria/Baja", "${playerStats.pvpWon} / ${playerStats.pvpLost}")
                                StatGridItem("PvE Victoria/Baja", "${playerStats.pveWon} / ${playerStats.pveLost}")
                                StatGridItem("Total Victorias", playerStats.gamesWon.toString())
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatsCard(
                                title = "HISTORIAL DE RACHAS",
                                modifier = Modifier.weight(1f),
                                color = ModernColors.primary
                            ) {
                                StatGridItem("Racha Actual", playerStats.winStreak.toString())
                                StatGridItem("Récord de Racha", playerStats.bestWinStreak.toString())
                            }
                            
                            StatsCard(
                                title = "MÁXIMO RENDIMIENTO",
                                modifier = Modifier.weight(1f),
                                color = ModernColors.error
                            ) {
                                StatGridItem("Mejor Puntería", "${String.format("%.1f", playerStats.bestAccuracy)}%")
                                StatGridItem("Asalto más veloz", if(playerStats.fastestWinTurns == Int.MAX_VALUE) "N/A" else "${playerStats.fastestWinTurns} turnos")
                            }
                        }
                    }
                }
            }

            // Leaderboard Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "RANKING GLOBAL DE COMANDANTES",
                        style = MaterialTheme.typography.titleMedium,
                        color = ModernColors.primary
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, ModernColors.primary.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = ModernColors.DeepAsh.copy(alpha = 0.6f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            // Table Header
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RANGO", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f), color = ModernColors.secondary)
                                Text("COMANDANTE", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f), color = ModernColors.primary)
                                Text("WINS", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), color = ModernColors.textSecondary)
                                Text("RATIO", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), color = ModernColors.textSecondary)
                                Text("RACHA", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), color = ModernColors.textSecondary)
                            }
                            HorizontalDivider(color = ModernColors.primary.copy(alpha = 0.3f))
                            
                            leaderboard.forEach { entry ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("#${entry.rank}", modifier = Modifier.weight(0.5f), color = ModernColors.secondary)
                                    Text(entry.playerName, modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = ModernColors.primary)
                                    Text("${entry.gamesWon}", modifier = Modifier.weight(1f), color = ModernColors.textPrimary)
                                    Text("${String.format("%.1f", entry.winRate)}%", modifier = Modifier.weight(1f), color = ModernColors.secondary)
                                    Text("${entry.bestStreak}", modifier = Modifier.weight(1f), color = ModernColors.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    modifier: Modifier = Modifier,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = ModernColors.DeepAsh.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Black
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = color.copy(alpha = 0.2f))
            content()
        }
    }
}

@Composable
fun StatGridItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = ModernColors.textSecondary)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = ModernColors.textPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
