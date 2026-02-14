package com.lfigueira.hundir_la_flota.server

import com.lfigueira.hundir_la_flota.common.AppLogger
import com.lfigueira.hundir_la_flota.server.logic.GameLogic
import com.lfigueira.hundir_la_flota.server.persistence.RecordsManager
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Collections
import java.util.Properties
import java.io.File
import java.net.Socket
import java.util.UUID

/**
 * Servidor TCP principal del juego Hundir la Flota.
 * Utiliza coroutines para manejar múltiples clientes de forma concurrente.
 */
class GameServer(private val configPath: String = "server.properties") {
    
    private val config = loadConfig(configPath)
    private val recordsManager = RecordsManager(config["server.records_file"] ?: "records.json")
    private val clients = Collections.synchronizedList(mutableListOf<ClientHandler>())
    
    // Scope con SupervisorJob para que el fallo de un cliente no afecte a otros
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Componentes del servidor
    // Componentes del servidor
    val gameLogic = GameLogic(recordsManager)
    
    private lateinit var serverSocket: ServerSocket
    private var isRunning = false
    
    private fun loadConfig(path: String): Map<String, String> {
        val props = Properties()
        val configMap = mutableMapOf<String, String>()
        
        try {
            // Intentar cargar desde resources primero
            val resourceStream = this::class.java.classLoader.getResourceAsStream("server.properties")
            if (resourceStream != null) {
                props.load(resourceStream)
            }
            
            // Sobrescribir con archivo externo si existe
            val file = java.io.File(path)
            if (file.exists()) {
                FileInputStream(file).use { props.load(it) }
            }
            
            for (name in props.stringPropertyNames()) {
                configMap[name] = props.getProperty(name)
            }
        } catch (e: Exception) {
            AppLogger.error("GameServer", "Error al cargar configuración: ${e.message}", e)
        }
        
        return configMap
    }
    
    /**
     * Inicia el servidor.
     */
    fun start() {
        val host = config["server.host"] ?: "127.0.0.1" // Default to 127.0.0.1 for Windows compatibility
        val port = config["server.port"]?.toIntOrNull() ?: 5678 // Default to 5678
        val maxClients = config["server.max_clients"]?.toIntOrNull() ?: 10
        
        try {
            // Requisito estricto: Backlog de 50 y uso de InetAddress
            val bindAddress = InetAddress.getByName(host)
            serverSocket = ServerSocket(port, 50, bindAddress)
            isRunning = true
            
            AppLogger.info("GameServer", "SERVIDOR HUNDIR LA FLOTA - INICIADO")
            AppLogger.info("GameServer", "Host: $host | Puerto: $port | Max Clientes: $maxClients")
            AppLogger.debug("GameServer", "Servidor escuchando activamente en $host:$port")
            
            // Coroutine para aceptar clientes
            serverScope.launch(Dispatchers.IO) {
                while (isRunning && !serverSocket.isClosed) {
                    try {
                        AppLogger.debug("GameServer", "Esperando nueva conexión...")
                        val clientSocket = serverSocket.accept()
                        AppLogger.info("GameServer", "Nueva conexión aceptada desde ${clientSocket.inetAddress.hostAddress}")
                        
                        if (clients.size >= maxClients) {
                            AppLogger.info("GameServer", "Rechazando conexión: Servidor lleno (${clients.size}/$maxClients)")
                            clientSocket.close()
                            continue
                        }
                        
                        // Manejo de cada cliente en su propia coroutine
                        launch(Dispatchers.IO) {
                            handleClient(clientSocket)
                        }
                        
                    } catch (e: Exception) {
                        if (isRunning) {
                            AppLogger.error("GameServer", "Error aceptando conexión: ${e.message}", e)
                        }
                    }
                }
            }
            
        } catch (e: java.net.BindException) {
            AppLogger.error("GameServer", "ERROR CRÍTICO: El puerto $port ya está en uso.", e)
            throw e
        } catch (e: Exception) {
            AppLogger.error("GameServer", "Error al iniciar servidor: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Ciclo de vida del cliente: Handshake -> Auth -> Loop
     */
    private suspend fun handleClient(socket: Socket) {
        val clientId = UUID.randomUUID().toString().take(8)
        AppLogger.debug("GameServer", "[Client-$clientId] Iniciando handleClient para socket: ${socket.inetAddress.hostAddress}")
        val handler = ClientHandler(socket, this, serverScope)
        clients.add(handler)
        AppLogger.info("GameServer", "[Client-$clientId] Cliente añadido a la lista activa. Total: ${clients.size}")
        
        try {
            handler.startLogic() 
        } catch (e: Exception) {
            AppLogger.error("GameServer", "Error en handleClient (ID: $clientId): ${e.message}", e)
        } finally {
            AppLogger.info("GameServer", "[Client-$clientId] Finalizando handleClient. Removiendo de la lista.")
            clients.remove(handler)
            handler.disconnect()
        }
    }
    
    fun removeClient(client: ClientHandler) {
        clients.remove(client)
    }
    
    fun getRecordsManager() = recordsManager
    
    fun stop() {
        isRunning = false
        try {
            if (::serverSocket.isInitialized && !serverSocket.isClosed) {
                serverSocket.close()
            }
        } catch (e: Exception) { }
        
        synchronized(clients) {
            clients.toList().forEach { it.disconnect() }
            clients.clear()
        }
        serverScope.cancel()
        AppLogger.info("GameServer", "Servidor detenido")
    }
    
    fun getStats(): String {
        return """
            Estado: ${if(isRunning) "Online" else "Offline"}
            Clientes: ${clients.size}
        """.trimIndent()
    }
}
