package com.lfigueira.hundir_la_flota.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfigueira.hundir_la_flota.ui.GameViewModel
import com.lfigueira.hundir_la_flota.ui.theme.ModernColors

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
            text = "OPERACIONES EN RED",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground // Use theme token for headline color
        )
        
        Spacer(Modifier.height(48.dp))
        
        // 1. Matchmaking Rápido
        Button(
            onClick = { viewModel.findPvPMatch() },
            modifier = Modifier.width(320.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("MATCHMAKING OPERATIVO (PvP)", fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(32.dp))
        
        HorizontalDivider(color = ModernColors.primary.copy(alpha=0.2f), thickness = 1.dp, modifier = Modifier.width(240.dp))
        
        Spacer(Modifier.height(32.dp))
        
        // 2. Salas Privadas
        Text("ZONA PRIVADA", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        
        // Crear
        OutlinedButton(
            onClick = { viewModel.createRoom() },
            modifier = Modifier.width(320.dp).height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Text("GENERAR CÓDIGO DE ACCESO")
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Unirse        
        Row(Modifier.width(320.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it.uppercase() },
                placeholder = { Text("ID SALA") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                 colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.joinRoom(roomCode) },
                enabled = roomCode.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("UNIRSE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        
        Spacer(Modifier.height(64.dp))
        
        // Volver
        TextButton(onClick = onBack) {
            Text("Volver al Menú Principal")
        }
    }
}
