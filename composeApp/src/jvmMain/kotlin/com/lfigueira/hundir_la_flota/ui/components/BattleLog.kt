package com.lfigueira.hundir_la_flota.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfigueira.hundir_la_flota.ui.BattleEvent
import com.lfigueira.hundir_la_flota.ui.theme.ModernColors

/**
 * Componente que muestra una lista cronológica de eventos de combate.
 * Realiza auto-scroll al fondo cuando se añaden nuevos eventos.
 * @param events Lista de mensajes y sus colores asociados.
 * @param modifier Modificador de diseño.
 */
@Composable
fun BattleLog(
    events: List<BattleEvent>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom
    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            listState.animateScrollToItem(events.size - 1)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Text(
            "REGISTRO TÁCTICO",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(events) { event ->
                Text(
                    text = "> ${event.message}",
                    color = event.color,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
