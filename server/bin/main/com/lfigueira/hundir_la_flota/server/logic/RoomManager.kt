package com.lfigueira.hundir_la_flota.server.logic

import com.lfigueira.hundir_la_flota.common.AppLogger
import com.lfigueira.hundir_la_flota.common.models.GameConfig
import com.lfigueira.hundir_la_flota.common.protocol.GameMessage
import com.lfigueira.hundir_la_flota.server.ClientHandler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class Room(
    val id: String,
    val code: String,
    val host: ClientHandler,
    val config: GameConfig,
    val players: MutableList<ClientHandler> = mutableListOf()
)

class RoomManager(private val gameLogic: GameLogic) {
    private val rooms = ConcurrentHashMap<String, Room>() // Key by RoomID
    private val roomCodes = ConcurrentHashMap<String, String>() // Key: Code, Value: RoomID
    private val mutex = Mutex()
    private val random = SecureRandom()
    private val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    /**
     * Crea una sala privada.
     */
    suspend fun createRoom(host: ClientHandler, config: GameConfig) {
        mutex.withLock {
            val roomId = UUID.randomUUID().toString()
            val code = generateUniqueCode()
            
            val room = Room(
                id = roomId,
                code = code,
                host = host,
                config = config
            )
            room.players.add(host)
            
            rooms[roomId] = room
            roomCodes[code] = roomId
            
            AppLogger.info("RoomManager", "Sala creada: $code por ${host.playerName}")
            
            host.sendMessage(GameMessage.Matchmaking.RoomCreated(roomId, code, config))
            broadcastRoomStatus(room)
        }
    }

    /**
     * Intenta unir a un jugador a una sala mediante código.
     */
    suspend fun joinRoom(client: ClientHandler, code: String) {
        mutex.withLock {
            val roomId = roomCodes[code?.uppercase()]
            if (roomId == null) {
                client.sendMessage(GameMessage.Matchmaking.RoomError("Sala no encontrada"))
                return
            }
            
            val room = rooms[roomId]
            if (room == null) {
                client.sendMessage(GameMessage.Matchmaking.RoomError("Error interno: Sala fantasma"))
                return
            }
            
            if (room.players.size >= 2) {
                client.sendMessage(GameMessage.Matchmaking.RoomError("Sala llena"))
                return
            }
            
            // Unir jugador
            room.players.add(client)
            AppLogger.info("RoomManager", "${client.playerName} se unió a sala $code")
            
            client.sendMessage(GameMessage.Matchmaking.RoomJoined(room.id, room.code, room.config))
            broadcastRoomStatus(room)
            
            // Si está lleno (2 jugadores), iniciar partida
            if (room.players.size == 2) {
                startGame(room)
            }
        }
    }
    
    /**
     * Maneja desconexión.
     */
    suspend fun handleDisconnect(client: ClientHandler) {
        mutex.withLock {
            val room = rooms.values.find { it.players.contains(client) } ?: return
            
            AppLogger.info("RoomManager", "${client.playerName} salió de sala ${room.code}")
            room.players.remove(client)
            
            if (room.players.isEmpty()) {
                rooms.remove(room.id)
                roomCodes.remove(room.code)
                AppLogger.info("RoomManager", "Sala ${room.code} eliminada por estar vacía")
            } else {
                broadcastRoomStatus(room)
            }
        }
    }

    private suspend fun startGame(room: Room) {
        AppLogger.info("RoomManager", "Iniciando partida en sala ${room.code}")
        
        // Delegamos a GameLogic para crear la sesión REAL
        // Emulamos un objeto Lobby para integrarnos con startGameFromLobby existente
        // o llamamos a una nueva función. Reutilizaremos startGameFromLobby pasando un Lobby ad-hoc.
        
        val pseudoLobby = Lobby(
            id = room.id,
            config = room.config,
            isPvE = false,
            players = room.players
        )
        
        gameLogic.startGameFromLobby(pseudoLobby)
        
        // Limpiamos la sala del manager ya que GameLogic toma el control sesión
        rooms.remove(room.id)
        roomCodes.remove(room.code)
    }

    private suspend fun broadcastRoomStatus(room: Room) {
        val playerNames = room.players.map { it.playerName }
        // Reutilizamos LobbyStatus, añadiendo el código si es necesario
        // Como LobbyStatus ya se modificó con roomCode en Protocolo (opcional):
        val msg = GameMessage.Matchmaking.LobbyStatus(
            lobbyId = room.id,
            players = playerNames,
            maxPlayers = 2,
            config = room.config,
            roomCode = room.code
        )
        room.players.forEach { it.sendMessage(msg) }
    }

    private fun generateUniqueCode(): String {
        var code = ""
        do {
            code = (1..5) 
                .map { charPool[random.nextInt(charPool.length)] }
                .joinToString("")
        } while (roomCodes.containsKey(code))
        return code
    }
}
