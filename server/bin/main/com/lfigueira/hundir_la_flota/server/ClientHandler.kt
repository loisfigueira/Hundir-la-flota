package com.lfigueira.hundir_la_flota.server

import com.lfigueira.hundir_la_flota.common.AppLogger
import com.lfigueira.hundir_la_flota.common.protocol.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketException
import java.util.UUID

/**
 * Maneja la comunicación con un cliente individual.
 * Versión simplificada: Line-based JSON, Scanner, Robust Loop.
 */
class ClientHandler(
    private val socket: Socket,
    private val server: GameServer,
    private val scope: CoroutineScope
) {
    val clientId: String = UUID.randomUUID().toString()
    var playerName: String = "Unknown"
        private set
    
    // Using standard Java I/O as requested
    private val reader = socket.getInputStream().bufferedReader()
    private val writer = java.io.PrintWriter(socket.getOutputStream(), true) // autoFlush = true
    
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }
    
    private var lastPingTime = System.currentTimeMillis()
    private var isConnected = true
    
    /**
     * Inicia la lógica del cliente (Bloqueante/Suspend).
     */
    suspend fun startLogic() = coroutineScope {
        AppLogger.debug("ClientHandler", "[ClientHandler-$clientId] Iniciando startLogic para player: $playerName")
        try {
            // Configurar socket
            socket.keepAlive = true
            socket.tcpNoDelay = true
            
            AppLogger.info("ClientHandler", "[ClientHandler-$clientId] Cliente conectado desde ${socket.inetAddress.hostAddress}:${socket.port}")
            
            // Lanzar monitor de keep-alive en paralelo
            val keepAliveJob = launch { monitorKeepAlive() }
            
            try {
                // Bucle de lectura estándar basado en líneas
                while (isConnected && !socket.isClosed) {
                    val line = withContext(Dispatchers.IO) {
                        try {
                            reader.readLine()
                        } catch (e: Exception) {
                             AppLogger.error("ClientHandler", "[ClientHandler-$clientId] Excepción física en lectura de socket: ${e.message}", e)
                             null
                        }
                    }
                    
                    if (line == null) {
                        AppLogger.info("ClientHandler", "[ClientHandler-$clientId] Conexión cerrada (EOF detectado). Finalizando bucle de lectura.")
                        break
                    }
                    
                    if (line.isBlank()) continue
                    
                    AppLogger.debug("ClientHandler", "[ClientHandler-$clientId] <<< RECIBIDO: $line")
                    
                    try {
                        val message = json.decodeFromString<GameMessage>(line)
                        handleMessage(message)
                    } catch (t: Throwable) {
                        AppLogger.error("ClientHandler", "[ClientHandler-$clientId] Error fatal procesando mensaje: ${t.message}", t)
                    }
                }
            } finally {
                AppLogger.debug("ClientHandler", "[ClientHandler-$clientId] Saliendo del bucle de lectura. Cancelando keepAliveJob.")
                keepAliveJob.cancel()
            }
            
        } catch (e: SocketException) {
            AppLogger.info("ClientHandler", "[ClientHandler-$clientId] Socket se cerró abruptamente: ${e.message}")
        } catch (e: Exception) {
            AppLogger.error("ClientHandler", "[ClientHandler-$clientId] Error inesperado en startLogic: ${e.message}", e)
        } finally {
            AppLogger.debug("ClientHandler", "[ClientHandler-$clientId] Ejecutando bloque finally de startLogic.")
            disconnect()
        }
    }
    
    // Método legacy start() eliminado o adaptado si fuera necesario
    fun start() {
        scope.launch { startLogic() }
    }
    
    /**
     * Procesa un mensaje recibido del cliente.
     */
    private suspend fun handleMessage(message: GameMessage) {
        when (message) {
            is GameMessage.Connection.Handshake -> {
                playerName = message.playerName
                AppLogger.info("ClientHandler", "[ClientHandler] HANDSHAKE recibido de $playerName")
                sendMessage(GameMessage.Connection.Welcome(
                    playerId = clientId,
                    serverVersion = "1.0.0",
                    message = "Bienvenido a Hundir la Flota, $playerName!"
                ))
                
                // Enviar estadísticas (Safe call in case records fails)
                try {
                    val stats = server.getRecordsManager().getPlayerStats(playerName)
                    val leaderboard = server.getRecordsManager().getLeaderboard()
                    sendMessage(GameMessage.Stats.Response(stats, leaderboard))
                } catch(e: Exception) {
                    AppLogger.error("ClientHandler", "[ClientHandler] Error recuperando stats: ${e.message}", e)
                }
            }
            
            is GameMessage.Matchmaking.FindPvP -> {
                AppLogger.info("ClientHandler", "[ClientHandler] Solicitud Matchmaking PVP de $playerName")
                server.gameLogic.findMatch(this, message.gameConfig, isPvE = false)
            }
            
            is GameMessage.Matchmaking.FindPvE -> {
                AppLogger.info("ClientHandler", "[ClientHandler] Solicitud Matchmaking PVE de $playerName")
                server.gameLogic.findMatch(this, message.gameConfig, isPvE = true, message.difficulty)
            }
            
            is GameMessage.Matchmaking.CancelSearch -> {
                AppLogger.info("ClientHandler", "[ClientHandler] $playerName cancela búsqueda")
                server.gameLogic.cancelSearch(this)
            }

            is GameMessage.Matchmaking.CreateRoom -> {
                AppLogger.info("ClientHandler", "[ClientHandler] $playerName crea sala privada")
                server.gameLogic.createRoom(this, message.config)
            }

            is GameMessage.Matchmaking.JoinRoom -> {
                AppLogger.info("ClientHandler", "[ClientHandler] $playerName se une a sala ${message.roomCode}")
                server.gameLogic.joinRoom(this, message.roomCode)
            }
            
            is GameMessage.System.Ping -> {
                lastPingTime = System.currentTimeMillis()
                sendMessage(GameMessage.System.Pong())
            }
            
            is GameMessage.System.Pong -> {
                lastPingTime = System.currentTimeMillis()
            }
            
            is GameMessage.Action -> {
                server.gameLogic.handleGameAction(this, message)
            }
            
            is GameMessage.Stats.Request -> {
                 try {
                    val stats = server.getRecordsManager().getPlayerStats(playerName)
                    val leaderboard = server.getRecordsManager().getLeaderboard()
                    sendMessage(GameMessage.Stats.Response(stats, leaderboard))
                } catch(e: Exception) {}
            }
            
            is GameMessage.Connection.Disconnect -> {
                AppLogger.info("ClientHandler", "[ClientHandler] $playerName solicitó desconexión.")
                disconnect()
            }
            
            else -> {
                AppLogger.info("ClientHandler", "[ClientHandler] Mensaje desconocido de $playerName: ${message::class.simpleName}")
            }
        }
    }
    
    /**
     * Monitorea el keep-alive del cliente.
     */
    private suspend fun monitorKeepAlive() {
        while (isConnected) {
            delay(10000) 
            val timeSinceLastPing = System.currentTimeMillis() - lastPingTime
            if (timeSinceLastPing > 60000) { 
                AppLogger.info("ClientHandler", "[ClientHandler] Timeout Keep-Alive ($playerName). Desconectando.")
                disconnect()
                break
            }
        }
    }
    
    /**
     * Envía un mensaje al cliente.
     */
    suspend fun sendMessage(message: GameMessage) {
        try {
            val jsonString = json.encodeToString(message)
            AppLogger.debug("ClientHandler", "[ClientHandler-$clientId] >>> ENVIANDO: $jsonString")
            withContext(Dispatchers.IO) {
                writer.println(jsonString)
            }
        } catch (e: Exception) {
            AppLogger.error("ClientHandler", "[ClientHandler-$clientId] Error enviando mensaje a $playerName: ${e.message}", e)
            disconnect()
        }
    }
    
    /**
     * Desconecta al cliente.
     */
    fun disconnect() {
        if (!isConnected) {
            AppLogger.debug("ClientHandler", "[ClientHandler-$clientId] Intento de desconexión pero ya estaba desconectado.")
            return
        }
        isConnected = false
        AppLogger.info("ClientHandler", "[ClientHandler-$clientId] Iniciando desconexión de $playerName...")
        try {
            scope.launch { 
                AppLogger.debug("ClientHandler", "[ClientHandler-$clientId] Notificando desconexión a GameLogic...")
                server.gameLogic.handleDisconnect(this@ClientHandler) 
            }
            if (!socket.isClosed) {
                socket.close()
                AppLogger.info("ClientHandler", "[ClientHandler-$clientId] Socket cerrado con éxito.")
            }
            server.removeClient(this)
            AppLogger.info("ClientHandler", "[ClientHandler-$clientId] Cliente removido del servidor.")
        } catch (e: Exception) { 
            AppLogger.error("ClientHandler", "[ClientHandler-$clientId] Error durante disconnect: ${e.message}", e)
        }
    }
}
