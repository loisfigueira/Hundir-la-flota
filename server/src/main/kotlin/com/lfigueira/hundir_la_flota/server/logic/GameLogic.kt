package com.lfigueira.hundir_la_flota.server.logic

import com.lfigueira.hundir_la_flota.common.AppLogger
import com.lfigueira.hundir_la_flota.common.models.GameConfig
import com.lfigueira.hundir_la_flota.common.protocol.*
import com.lfigueira.hundir_la_flota.server.ClientHandler
import com.lfigueira.hundir_la_flota.server.game.GameSession
import com.lfigueira.hundir_la_flota.server.persistence.RecordsManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Lógica autoritativa del juego.
 * Facade que conecta ClientHandler con GameSession.
 */
class GameLogic(private val recordsManager: RecordsManager) {
    
    private val activeGames = ConcurrentHashMap<String, GameSession>()
    // Reemplazamos matchmakingQueue con LobbyManager
    private val lobbyManager = LobbyManager(this)
    private val roomManager = RoomManager(this)

    
    /**
     * Busca una partida para el cliente (ahora delega al LobbyManager).
     */
    suspend fun findMatch(
        client: ClientHandler,
        config: GameConfig,
        isPvE: Boolean,
        difficulty: AIDifficulty = AIDifficulty.MEDIUM
    ) {
        lobbyManager.joinLobby(client, config, isPvE, difficulty)
    }

    /**
     * Inicia una partida desde un Lobby completo.
     * Llamado por LobbyManager.
     */
    suspend fun startGameFromLobby(lobby: Lobby) {
        val gameId = "game_${if (lobby.isPvE) "pve" else "pvp"}_${System.currentTimeMillis()}"
        
        val player1 = lobby.players[0]
        val player2 = if (lobby.isPvE) null else lobby.players.getOrNull(1)
        
        // En PvE, si player2 es null, GameSession lo maneja. En PvP aseguramos que haya 2.
        if (!lobby.isPvE && player2 == null) {
            AppLogger.error("GameLogic", "Intento de iniciar PvP con un solo jugador.")
            return
        }

        val gameSession = GameSession(
            gameId = gameId,
            player1 = player1,
            player2 = player2,
            isPvE = lobby.isPvE,
            difficulty = lobby.difficulty,
            config = lobby.config, // ¡Configuración del Lobby!
            recordsManager = recordsManager,
            onFinished = { gid -> removeGame(gid) }
        )
        activeGames[gameId] = gameSession
        
        AppLogger.info("GameLogic", "Partida iniciada: $gameId (PvE=${lobby.isPvE}) Config=${lobby.config.boardSize}x${lobby.config.boardSize}")
        
        // Enviar MatchFound con la CONFIGURACIÓN AUTORITATIVA
        val opponentName = if (lobby.isPvE) "Bot Hunter (${lobby.difficulty.name})" else player2?.playerName ?: "Unknown"
        
        player1.sendMessage(GameMessage.Matchmaking.MatchFound(
            gameId = gameId,
            opponentName = opponentName,
            isPvE = lobby.isPvE,
            config = lobby.config
        ))
        
        player2?.sendMessage(GameMessage.Matchmaking.MatchFound(
            gameId = gameId,
            opponentName = player1.playerName,
            isPvE = lobby.isPvE,
            config = lobby.config
        ))
        
        gameSession.start()
    }
    
    /**
     * Cancela la búsqueda de partida (salir del Lobby).
     */
    suspend fun cancelSearch(client: ClientHandler) {
        lobbyManager.handleDisconnect(client)
    }

    /**
     * Crea una sala privada.
     */
    suspend fun createRoom(client: ClientHandler, config: GameConfig) {
        roomManager.createRoom(client, config)
    }

    /**
     * Se une a una sala privada.
     */
    suspend fun joinRoom(client: ClientHandler, code: String) {
        roomManager.joinRoom(client, code)
    }

    /**
     * Delega acciones de juego a la GameSession correspondiente.
     */
    suspend fun handleGameAction(client: ClientHandler, action: GameMessage.Action) {
        // StartGamePVE legacy ya no se usa directamente, todo va por findMatch
        if (action is GameMessage.Action.StartGamePVE) {
            findMatch(client, action.config, true, action.difficulty)
            return
        }

        val game = findGameByClient(client) ?: run {
            // AppLogger.debug("GameLogic", "${client.playerName} intentó acción sin estar en partida activa.")
            return
        }
        
        game.handleAction(client, action)
    }
    
    /**
     * Maneja la desconexión de un cliente.
     */
    suspend fun handleDisconnect(client: ClientHandler) {
        // 1. Si está en Lobby, removerlo
        lobbyManager.handleDisconnect(client)
        roomManager.handleDisconnect(client)
        
        // 2. Si está en Partida, rendirse/limpiar
        val game = findGameByClient(client)
        game?.let {
            it.handleAction(client, GameMessage.Action.LeaveGame) 
            // LeaveGame ya hace dispose internamente en GameSession (según cambios anteriores)
        }
    }
    
    /**
     * Encuentra la partida de un cliente.
     */
    private fun findGameByClient(client: ClientHandler): GameSession? {
        return activeGames.values.firstOrNull { 
            it.player1 == client || it.player2 == client 
        }
    }
    
    fun removeGame(gameId: String) {
        activeGames.remove(gameId)
    }
    
    /**
     * Llamado cuando un jugador completa la conexión (Handshake).
     */
    suspend fun onPlayerConnected(client: ClientHandler) {
        // 1. Asegurar que tenga records (Crear si es nuevo)
        val stats = recordsManager.ensurePlayerStats(client.playerName)
        
        // 2. Enviar stats al cliente
        val leaderboard = recordsManager.getLeaderboard()
        client.sendMessage(GameMessage.Stats.Response(stats, leaderboard))
        
        AppLogger.info("GameLogic", "Stats enviadas a ${client.playerName} tras conexión.")
    }
}
