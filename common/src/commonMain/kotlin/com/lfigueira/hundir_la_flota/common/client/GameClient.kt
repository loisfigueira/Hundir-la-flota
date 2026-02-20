package com.lfigueira.hundir_la_flota.common.client

import com.lfigueira.hundir_la_flota.common.AppLogger
import com.lfigueira.hundir_la_flota.common.network.NetworkClient
import com.lfigueira.hundir_la_flota.common.protocol.*
import com.lfigueira.hundir_la_flota.common.PlatformUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.min

/**
 * Interfaz para el cliente de juego.
 * Permite mockear la comunicación en tests de ViewModel.
 */
interface IGameClient {
    /** Estado actual de la conexión (desconectado, conectando, conectado, error). */
    val connectionState: StateFlow<ConnectionState>
    /** Flujo de mensajes entrantes procesados y decodificados. */
    val incomingMessages: SharedFlow<GameMessage>
    
    /**
     * Inicia el proceso de conexión y autenticación.
     * @return True si se estableció la conexión con éxito.
     */
    suspend fun connectAndInitialize(host: String, port: Int, playerName: String): Boolean
    /** Envía un mensaje al servidor. */
    suspend fun send(message: GameMessage)
    /** Cierra la conexión de forma limpia. */
    suspend fun disconnect()
}

/**
 * Cliente de juego con lógica de negocio compartida.
 * Maneja el ciclo de vida, reconexión y estado.
 */
class GameClient : IGameClient {
    private val networkClient = NetworkClient()
    private val scope = CoroutineScope(Dispatchers.Default)
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        classDiscriminator = "type"
    }
    
    // Estados internos
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _incomingMessages = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)
    override val incomingMessages: SharedFlow<GameMessage> = _incomingMessages.asSharedFlow()
    
    private var keepAliveJob: Job? = null
    private var messageCollectionJob: Job? = null
    
    // Configuración de reconexión
    private var currentHost = "127.0.0.1"
    private var currentPort = 5678
    private var currentPlayerName = ""
    
    /**
     * Inicia la conexión, handshake y descarga de records.
     * @return True si la inicialización fue exitosa (se recibieron records), False en caso contrario.
     */
    override suspend fun connectAndInitialize(host: String, port: Int, playerName: String): Boolean {
        currentHost = host
        currentPort = port
        currentPlayerName = playerName
        
        return try {
            connectInternal(host, port, playerName)
            true
        } catch (e: Exception) {
            AppLogger.error("GameClient", "Error de inicialización: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Error de inicialización: ${e.message}")
            false
        }
    }
    
    private suspend fun connectInternal(host: String, port: Int, playerName: String) {
        AppLogger.debug("GameClient", "Intentando connectInternal a $host:$port para $playerName")
        _connectionState.value = ConnectionState.Connecting(1, 1)
        
        networkClient.connect(host, port)
        AppLogger.info("GameClient", "Socket conectado. Iniciando recolección de mensajes y handshake.")
        
        // Iniciar recolección de mensajes
        startMessageCollection()
        
        // Enviar Handshake
        AppLogger.debug("GameClient", ">>> ENVIANDO Handshake para: $playerName")
        send(GameMessage.Connection.Handshake(playerName))
        
        // Iniciar Keep-Alive
        startKeepAlive()
        
        _connectionState.value = ConnectionState.Connected
        AppLogger.info("GameClient", "Conexión establecida y autenticada.")
    }
    
    private fun startMessageCollection() {
        messageCollectionJob?.cancel()
        messageCollectionJob = scope.launch {
            try {
                networkClient.receive()
                    .catch { e ->
                        AppLogger.error("GameClient", "Error recibiendo mensajes: $e", e)
                        handleDisconnection()
                    }
                    .collect { line ->
                        if (line.isNotBlank()) {
                            AppLogger.debug("GameClient", "<<< RECIBIDO: $line")
                            try {
                                val message = json.decodeFromString<GameMessage>(line)
                                handleInternalMessage(message)
                                _incomingMessages.emit(message)
                            } catch (e: Exception) {
                                AppLogger.error("GameClient", "Error decodificando mensaje: $e | Contenido: $line", e)
                            }
                        }
                    }
            } catch (e: Exception) {
                // Socket cerrado u otro error
                AppLogger.error("GameClient", "Excepción inesperada en message collection: ${e.message}", e)
                handleDisconnection()
            }
        }
    }
    
    private suspend fun handleInternalMessage(message: GameMessage) {
        when (message) {
            is GameMessage.System.Ping -> send(GameMessage.System.Pong())
            else -> {} // Otros mensajes se pasan a la UI
        }
    }
    
    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            try {
                while (true) {
                    val now = PlatformUtils.getCurrentTimeMillis()
                    send(GameMessage.System.Ping(now))
                    delay(30000) // Un latido cada 30 segundos
                }
            } catch (e: Exception) {
                AppLogger.error("GameClient", "Error en Keep-Alive: ${e.message}", e)
            }
        }
    }
    
    override suspend fun send(message: GameMessage) {
        try {
            val jsonString = json.encodeToString(GameMessage.serializer(), message)
            AppLogger.debug("GameClient", ">>> ENVIANDO: $jsonString")
            networkClient.send(jsonString)
        } catch (e: Exception) {
            AppLogger.error("GameClient", "Error enviando mensaje: $e", e)
        }
    }
    
    override suspend fun disconnect() {
        AppLogger.info("GameClient", "Desconectando...")
        try {
            send(GameMessage.Connection.Disconnect())
        } catch (e: Exception) { /* Ignorar */ }
        
        networkClient.disconnect()
        cleanup()
        _connectionState.value = ConnectionState.Disconnected
    }
    
    private fun cleanup() {
        keepAliveJob?.cancel()
        messageCollectionJob?.cancel()
    }
    
    private suspend fun handleDisconnection() {
        AppLogger.info("GameClient", "Manejando desconexión inesperada.")
        cleanup()
        if (_connectionState.value is ConnectionState.Disconnected) return
        
        _connectionState.value = ConnectionState.Reconnecting
        attemptReconnect()
    }
    
    private suspend fun attemptReconnect() {
        var attempt = 1
        val maxAttempts = 5
        var delayMs = 1000L
        
        while (attempt <= maxAttempts) {
            _connectionState.value = ConnectionState.Connecting(attempt, maxAttempts)
            AppLogger.info("GameClient", "Intento de reconexión $attempt/$maxAttempts...")
            
            try {
                networkClient.connect(currentHost, currentPort)
                startMessageCollection()
                send(GameMessage.Connection.Handshake(currentPlayerName)) // Re-identificarse
                startKeepAlive()
                
                _connectionState.value = ConnectionState.Connected
                AppLogger.info("GameClient", "Reconexión exitosa!")
                return
            } catch (e: Exception) {
                AppLogger.error("GameClient", "Fallo reconexión: ${e.message}", e)
            }
            
            delay(delayMs)
            delayMs = min(delayMs * 2, 16000) // Backoff exponencial
            attempt++
        }
        
        AppLogger.error("GameClient", "No se pudo reconectar tras $maxAttempts intentos", null)
        _connectionState.value = ConnectionState.Error("No se pudo reconectar tras $maxAttempts intentos")
    }
}

/**
 * Representa los diferentes estados en los que puede estar la conexión del cliente.
 */
sealed class ConnectionState {
    /** El cliente no está intentando conectar. */
    data object Disconnected : ConnectionState()
    /** El cliente está intentando establecer la conexión inicial. */
    data class Connecting(val attempt: Int, val maxAttempts: Int) : ConnectionState()
    /** La conexión está activa y estable. */
    data object Connected : ConnectionState()
    /** La conexión se perdió y se está intentando recuperar automáticamente. */
    data object Reconnecting : ConnectionState()
    /** Se ha producido un error crítico que impide la conexión. */
    data class Error(val message: String) : ConnectionState()
}
