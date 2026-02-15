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
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.theme.CyberColors

@Composable
fun OnlineMenuScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var roomCode by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "JUEGO ONLINE",
            style = MaterialTheme.typography.displayMedium,
            color = CyberColors.NeonBlue
        )
        
        Spacer(Modifier.height(48.dp))
        
        // 1. Matchmaking Rápido
        Button(
            onClick = { viewModel.findPvPMatch() },
            modifier = Modifier.width(300.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberColors.NeonGreen),
             shape = RoundedCornerShape(8.dp)
        ) {
            Text("BUSCAR PARTIDA RÁPIDA (PvP)", color = Color.Black)
        }
        
        Spacer(Modifier.height(32.dp))
        
        Divider(color = CyberColors.NeonBlue.copy(alpha=0.3f), thickness = 1.dp, modifier = Modifier.width(200.dp))
        
        Spacer(Modifier.height(32.dp))
        
        // 2. Salas Privadas
        Text("SALA PRIVADA", color = CyberColors.textPrimary)
        Spacer(Modifier.height(16.dp))
        
        // Crear
        OutlinedButton(
            onClick = { viewModel.createRoom() },
            modifier = Modifier.width(300.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberColors.NeonBlue),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberColors.NeonBlue)
        ) {
            Text("CREAR SALA")
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Unirse        
        Row(Modifier.width(300.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it.uppercase() },
                placeholder = { Text("CÓDIGO") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                 colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberColors.NeonBlue,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.joinRoom(roomCode) },
                enabled = roomCode.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = CyberColors.NeonBlue)
            ) {
                Text("UNIRSE")
            }
        }
        
        Spacer(Modifier.height(64.dp))
        
        // Volver
        TextButton(onClick = onBack) {
            Text("Volver al Menú Principal")
        }
    }
}
