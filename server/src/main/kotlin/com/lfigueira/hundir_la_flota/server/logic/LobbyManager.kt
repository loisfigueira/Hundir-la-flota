package com.lfigueira.hundir_la_flota.server.logic

import com.lfigueira.hundir_la_flota.common.AppLogger
import com.lfigueira.hundir_la_flota.common.models.GameConfig
import com.lfigueira.hundir_la_flota.common.protocol.AIDifficulty
import com.lfigueira.hundir_la_flota.common.protocol.GameMessage
import com.lfigueira.hundir_la_flota.server.ClientHandler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class Lobby(
    val id: String,
    val config: GameConfig,
    val isPvE: Boolean,
    val difficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val players: MutableList<ClientHandler> = mutableListOf()
) {
    val maxPlayers = 2
}

class LobbyManager(private val gameLogic: GameLogic) {
    private val lobbies = ConcurrentHashMap<String, Lobby>()
    private val mutex = Mutex()

    /**
     * Procesa la solicitud a una sala (PvP o PvE).
     */
    suspend fun joinLobby(client: ClientHandler, config: GameConfig, isPvE: Boolean, difficulty: AIDifficulty = AIDifficulty.MEDIUM) {
        mutex.withLock {
            if (isPvE) {
                createPvELobby(client, config, difficulty)
            } else {
                joinOrCreatePvPLobby(client, config)
            }
        }
    }

    private suspend fun createPvELobby(client: ClientHandler, config: GameConfig, difficulty: AIDifficulty) {
        val lobbyId = UUID.randomUUID().toString()
        val lobby = Lobby(lobbyId, config, isPvE = true, difficulty = difficulty)
        lobby.players.add(client)
        
        AppLogger.info("LobbyManager", "Lobby PvE creado $lobbyId para ${client.playerName}")
        lobbies[lobbyId] = lobby
        
        // PvE inicia inmediatamente
        gameLogic.startGameFromLobby(lobby)
        lobbies.remove(lobbyId)
    }

    private suspend fun joinOrCreatePvPLobby(client: ClientHandler, config: GameConfig) {
        // Buscar sala PvP con hueco y configuración compatible (por ahora solo miramos hueco)
        // Podríamos filtrar por boardSize si quisiéramos matchmaking estricto
        val availableLobby = lobbies.values.find { !it.isPvE && it.players.size < it.maxPlayers }

        if (availableLobby != null) {
            AppLogger.info("LobbyManager", "${client.playerName} uniéndose a Lobby ${availableLobby.id}")
            availableLobby.players.add(client)
            
            // Notificar estado a todos
            broadcastLobbyStatus(availableLobby)
            
            if (availableLobby.players.size == availableLobby.maxPlayers) {
                AppLogger.info("LobbyManager", "Lobby ${availableLobby.id} lleno. Iniciando partida.")
                gameLogic.startGameFromLobby(availableLobby)
                lobbies.remove(availableLobby.id)
            }
        } else {
            // Crear nueva sala
            val lobbyId = UUID.randomUUID().toString()
            val newLobby = Lobby(lobbyId, config, isPvE = false) // El creador define la config
            newLobby.players.add(client)
            lobbies[lobbyId] = newLobby
            
            AppLogger.info("LobbyManager", "Lobby PvP creado $lobbyId por ${client.playerName}. Esperando oponente...")
            broadcastLobbyStatus(newLobby)
        }
    }

    private suspend fun broadcastLobbyStatus(lobby: Lobby) {
        val playerNames = lobby.players.map { it.playerName }
        val msg = GameMessage.Matchmaking.LobbyStatus(
            lobbyId = lobby.id,
            players = playerNames,
            maxPlayers = lobby.maxPlayers,
            config = lobby.config
        )
        lobby.players.forEach { it.sendMessage(msg) }
    }

    /**
     * Maneja la desconexión de un jugador en fase de Lobby.
     */
    suspend fun handleDisconnect(client: ClientHandler) {
        mutex.withLock {
            val lobby = lobbies.values.find { it.players.contains(client) } ?: return
            
            AppLogger.info("LobbyManager", "${client.playerName} salió del Lobby ${lobby.id}")
            lobby.players.remove(client)
            
            if (lobby.players.isEmpty()) {
                lobbies.remove(lobby.id)
                AppLogger.info("LobbyManager", "Lobby ${lobby.id} eliminado por estar vacío.")
            } else {
                broadcastLobbyStatus(lobby)
            }
        }
    }
}
