package com.lfigueira.hundir_la_flota.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfigueira.hundir_la_flota.common.client.ConnectionState
import com.lfigueira.hundir_la_flota.common.protocol.AIDifficulty
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.theme.ModernColors

/**
 * Pantalla de bienvenida y menú principal.
 * Permite al usuario identificarse, conectar al servidor y elegir el modo de juego.
 * @param viewModel Instancia del ViewModel para gestionar acciones de navegación y conexión.
 * @param connectionState Estado actual de la conexión de red.
 */
@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    connectionState: ConnectionState
) {
    var playerName by remember { mutableStateOf("") }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título
        Text(
            text = "HUNDIR LA FLOTA",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Indicador de conexión
        ConnectionIndicator(connectionState)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Campo de nombre
        if (connectionState is ConnectionState.Disconnected) {
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Nombre del jugador") },
                singleLine = true,
                modifier = Modifier.width(300.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.connect(playerName) },
                enabled = playerName.isNotBlank(),
                modifier = Modifier.width(300.dp)
            ) {
                Text("Conectar al Servidor")
            }
        } else if (connectionState is ConnectionState.Connected) {
            // Menú principal
            Button(
                onClick = { viewModel.navigateToOnlineMenu() },
                modifier = Modifier.width(300.dp)
            ) {
                Text("JUGAR ONLINE")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { showDifficultyDialog = true },
                modifier = Modifier.width(300.dp)
            ) {
                Text("Nueva Partida PVE (Solo)")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { viewModel.navigateToSettings() },
                modifier = Modifier.width(300.dp)
            ) {
                Text("Ajustes")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { viewModel.navigateToRecords() },
                modifier = Modifier.width(300.dp)
            ) {
                Text("Ver Records")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
    

            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedButton(
                onClick = { viewModel.disconnect() },
                modifier = Modifier.width(300.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Desconectar")
            }
        }
    }
    
    // Diálogo de selección de dificultad
    if (showDifficultyDialog) {
        AlertDialog(
            onDismissRequest = { showDifficultyDialog = false },
            title = { Text("Selecciona la dificultad") },
            text = {
                Column {
                    AIDifficulty.entries.forEach { difficulty ->
                        TextButton(
                            onClick = {
                                viewModel.findPvEMatch(difficulty)
                                showDifficultyDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(difficulty.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDifficultyDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Indicador visual del estado de la conexión con el servidor.
 * @param connectionState Objeto que describe si el cliente está conectado, desconectado o en error.
 */
@Composable
fun ConnectionIndicator(connectionState: ConnectionState) {
    val (text, color) = when (connectionState) {
        is ConnectionState.Disconnected -> "Desconectado" to MaterialTheme.colorScheme.error
        is ConnectionState.Connecting -> "Conectando (${connectionState.attempt}/${connectionState.maxAttempts})..." to MaterialTheme.colorScheme.tertiary
        is ConnectionState.Reconnecting -> "Reconectando..." to MaterialTheme.colorScheme.tertiary
        is ConnectionState.Connected -> "Conectado" to MaterialTheme.colorScheme.primary
        is ConnectionState.Error -> "Error: ${connectionState.message}" to MaterialTheme.colorScheme.error
    }
    
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
