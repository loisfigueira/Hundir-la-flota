package com.lfigueira.hundir_la_flota.common.protocol

import com.lfigueira.hundir_la_flota.common.models.GameConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Protocolo de comunicación para el juego Hundir la Flota.
 * Adaptado a la lógica "1x4, 2x3, 3x2, 4x1".
 */
@Serializable
sealed class GameMessage {
    
    // ==================== CONEXIÓN ====================
    @Serializable
    sealed class Connection : GameMessage() {
        @Serializable @SerialName("connection_handshake")
        data class Handshake(val playerName: String, val clientVersion: String = "1.0.0") : Connection()
        
        @Serializable @SerialName("connection_welcome")
        data class Welcome(val playerId: String, val serverVersion: String, val message: String) : Connection()
        
        @Serializable @SerialName("connection_disconnect")
        data class Disconnect(val reason: String = "Client requested disconnect") : Connection()
    }
    
    // ==================== SISTEMA ====================
    @Serializable
    sealed class System : GameMessage() {
        @Serializable @SerialName("system_ping")
        data class Ping(val timestamp: Long = 0L) : System()
        
        @Serializable @SerialName("system_pong")
        data class Pong(val timestamp: Long = 0L) : System()
    }
    
    // ==================== MATCHMAKING ====================
    @Serializable
    sealed class Matchmaking : GameMessage() {
        @Serializable @SerialName("matchmaking_find_pvp")
        data class FindPvP(val gameConfig: GameConfig) : Matchmaking()
        
        @Serializable @SerialName("matchmaking_find_pve")
        data class FindPvE(val gameConfig: GameConfig, val difficulty: AIDifficulty = AIDifficulty.MEDIUM) : Matchmaking()
        
        @Serializable @SerialName("matchmaking_cancel")
        data object CancelSearch : Matchmaking()
        
        @Serializable @SerialName("matchmaking_found")
        data class MatchFound(
            val gameId: String, 
            val opponentName: String, 
            val isPvE: Boolean,
            val config: GameConfig // Configuración autoritativa de la partida
        ) : Matchmaking()
        
        @Serializable @SerialName("matchmaking_waiting")
        data class Waiting(val playersInQueue: Int) : Matchmaking()

        @Serializable @SerialName("matchmaking_lobby_status")
        data class LobbyStatus(
            val lobbyId: String,
            val players: List<String>, // Nombres de jugadores conectados
            val maxPlayers: Int = 2,
            val config: GameConfig,
            val roomCode: String? = null // Add roomCode here if relevant, or use specific message
        ) : Matchmaking()

        // --- Nuevos Mensajes para Salas Privadas ---
        @Serializable @SerialName("matchmaking_create_room")
        data class CreateRoom(val config: GameConfig) : Matchmaking()

        @Serializable @SerialName("matchmaking_join_room")
        data class JoinRoom(val roomCode: String) : Matchmaking()

        @Serializable @SerialName("matchmaking_room_created")
        data class RoomCreated(val roomId: String, val roomCode: String, val config: GameConfig) : Matchmaking()
        
        @Serializable @SerialName("matchmaking_room_joined")
        data class RoomJoined(val roomId: String, val roomCode: String, val config: GameConfig) : Matchmaking()

        @Serializable @SerialName("matchmaking_room_error")
        data class RoomError(val message: String) : Matchmaking()
    }
    
    // ==================== ACCIONES DEL JUEGO ====================
    @Serializable
    sealed class Action : GameMessage() {
        
        @Serializable
        @SerialName("action_place_ships")
        data class PlaceShips(
            val ships: List<ShipPlacement>
        ) : Action()
        
        @Serializable
        @SerialName("action_attack")
        data class Attack(
            val coordinate: Coordinate
        ) : Action()
        
        @Serializable
        @SerialName("action_surrender")
        data object Surrender : Action()

        @Serializable
        @SerialName("action_start_pve")
        data class StartGamePVE(val config: GameConfig, val difficulty: AIDifficulty = AIDifficulty.MEDIUM) : Action()

        @Serializable
        @SerialName("action_confirm_deployment")
        data class ConfirmDeployment(val ships: List<ShipPlacement>) : Action()

        @Serializable
        @SerialName("action_leave_game")
        data object LeaveGame : Action()
    }
    
    // ==================== RESPUESTAS DEL SERVIDOR ====================
    @Serializable
    sealed class Response : GameMessage() {
        
        @Serializable
        @SerialName("response_game_state")
        data class GameState(
            val phase: GamePhase,
            val currentTurn: String, // PlayerID
            val turnTimeSeconds: Int,
            val myBoard: BoardState,
            val opponentBoard: BoardState,
            val status: String
        ) : Response()

        @Serializable
        @SerialName("response_game_start")
        data class GameStart(
            val gameId: String,
            val opponentName: String,
            val turnTimeSeconds: Int
        ) : Response()

        @Serializable
        @SerialName("response_attack_result")
        data class AttackResult(
            val pos: Coordinate,
            val result: ShotResult,
            val sunk: Boolean,
            val shipSunkType: ShipType? = null // Opcional, para feedback extra
        ) : Response()
        
        @Serializable
        @SerialName("response_opponent_attack")
        data class OpponentAttackResult(
            val pos: Coordinate,
            val result: ShotResult,
            val sunk: Boolean,
            val shipSunkType: ShipType? = null
        ) : Response()

        @Serializable
        @SerialName("response_turn_update")
        data class TurnUpdate(
            val playerId: String, // ID del jugador cuyo turno es
            val secondsLeft: Int
        ) : Response()
        
        @Serializable
        @SerialName("response_game_over")
        data class GameOver(
            val winnerId: String,
            val stats: GameStats
        ) : Response()
        
        @Serializable
        @SerialName("response_round_result")
        data class RoundResult(
            val roundNumber: Int,
            val winnerId: String,
            val player1Score: Int, // Rondas ganadas por jugador 1
            val player2Score: Int, // Rondas ganadas por jugador 2
            val isMatchOver: Boolean, // true si se alcanzó el objetivo (ej: 2 de 3)
            val stats: GameStats
        ) : Response()
        
        @Serializable
        @SerialName("response_placement_confirmed")
        data class PlacementConfirmed(
            val shipType: ShipType // Keep for compatibility or feedback, using BATTLESHIP as dummy or meaningful
        ) : Response()
    }
    
    // ==================== ESTADÍSTICAS ====================
    @Serializable
    sealed class Stats : GameMessage() {
        @Serializable @SerialName("stats_request")
        data object Request : Stats()
        
        @Serializable @SerialName("stats_response")
        data class Response(val playerStats: PlayerStats, val leaderboard: List<LeaderboardEntry>) : Stats()
        
        @Serializable @SerialName("stats_update")
        data class Update(val playerStats: PlayerStats) : Stats()
    }
    
    // ==================== ERROR ====================
    @Serializable
    sealed class Error : GameMessage() {
        @Serializable @SerialName("error_invalid")
        data class InvalidAction(val message: String, val details: String? = null) : Error()
        
        @Serializable @SerialName("error_server")
        data class ServerError(val message: String, val code: Int = 500) : Error()
        
        @Serializable @SerialName("error_connection")
        data class ConnectionError(val message: String) : Error()
    }
}

// ==================== ENUMS Y TIPOS ====================

@Serializable
enum class AIDifficulty { EASY, MEDIUM, HARD }

@Serializable
enum class Orientation { HORIZONTAL, VERTICAL }

@Serializable
enum class ShotResult { MISS, HIT, SUNK }

@Serializable
enum class GameOverReason { ALL_SHIPS_SUNK, SURRENDER, DISCONNECT, TIMEOUT }

@Serializable
enum class ShipType(val size: Int) {
    CARRIER(5),    // Portaaviones - 1 de 5 (nueva flota)
    BATTLESHIP(4), // Acorazado - 2 de 4
    CRUISER(3),    // Crucero - 3 de 3
    DESTROYER(2),  // Destructor - 4 de 2
    PATROL_BOAT(1) // Patrullera - 4 de 1 (flota clásica)
}

// ==================== MODELOS DE DATOS ====================

@Serializable
data class Coordinate(val x: Int, val y: Int)

@Serializable
data class ShipPlacement(
    val type: ShipType,
    val coordinate: Coordinate,
    val orientation: Orientation
)

@Serializable
data class GameStats(
    val totalShots: Int,
    val successfulHits: Int,
    val shipsDestroyed: Int,
    val accuracy: Double,
    val winStreak: Int, // Añadido para actualizar persistencia
    val gameDurationSeconds: Long = 0
)

@Serializable
data class PlayerStats(
    val playerName: String,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    
    // Desglose PVP / PVE
    val pvpWon: Int = 0,
    val pvpLost: Int = 0,
    val pveWon: Int = 0,
    val pveLost: Int = 0,
    
    val totalShots: Int,
    val totalHits: Int,
    val winStreak: Int,
    val bestWinStreak: Int,
    val averageAccuracy: Double,
    
    // Records de rendimiento
    val bestAccuracy: Double = 0.0,
    val fastestWinTurns: Int = Int.MAX_VALUE, // Menor número de turnos para ganar
    
    val totalPlayTimeSeconds: Long
)

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val playerName: String,
    val gamesWon: Int,
    val winRate: Double,
    val bestStreak: Int
)

@Serializable
enum class CellState { EMPTY, SHIP, HIT, MISS }

@Serializable
enum class GamePhase { PLACEMENT, BATTLE, FINISHED }

@Serializable
data class BoardState(
    val size: Int = 10,
    val cells: List<List<CellState>>,
    val ships: List<ShipState>? = null
)

@Serializable
data class ShipState(
    val type: ShipType,
    val coordinates: List<Coordinate>,
    val sunk: Boolean
)
