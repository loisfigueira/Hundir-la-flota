package com.lfigueira.hundir_la_flota.common.network

import kotlinx.coroutines.flow.Flow

/**
 * Abstracción de red multiplataforma.
 * Define la interfaz para conectar, enviar y recibir mensajes.
 */
expect class NetworkClient() {
    /**
     * Estado de conexión observable.
     * Emite true cuando el socket está abierto, false en caso contrario.
     */
    val isConnected: Flow<Boolean>
    
    /**
     * Conecta al servidor.
     * @param host Host del servidor.
     * @param port Puerto del servidor.
     * @throws Exception Si falla la conexión.
     */
    suspend fun connect(host: String, port: Int)
    
    /**
     * Envía un mensaje (String) al servidor.
     * @param message Mensaje a enviar.
     */
    suspend fun send(message: String)
    
    /**
     * Flujo de mensajes recibidos del servidor.
     * Cada elemento es una línea de texto completa (JSON).
     */
    fun receive(): Flow<String>
    
    /**
     * Desconecta del servidor.
     */
    suspend fun disconnect()
}
