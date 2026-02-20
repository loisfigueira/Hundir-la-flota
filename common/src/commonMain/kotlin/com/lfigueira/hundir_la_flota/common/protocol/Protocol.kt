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
    /**
     * Mensajes relacionados con el establecimiento y cierre de la conexión física.
     */
    @Serializable
    sealed class Connection : GameMessage() {
        /**
         * Primer mensaje enviado por el cliente para identificarse.
         * @param playerName Nombre elegido por el usuario.
         * @param clientVersion Versión del cliente para asegurar compatibilidad.
         */
        @Serializable @SerialName("connection_handshake")
        data class Handshake(val playerName: String, val clientVersion: String = "1.0.0") : Connection()
        
        /**
         * Respuesta del servidor confirmando la aceptación del cliente.
         * @param playerId ID único asignado al jugador para la sesión.
         * @param serverVersion Versión del servidor.
         * @param message Mensaje de bienvenida opcional.
         */
        @Serializable @SerialName("connection_welcome")
        data class Welcome(val playerId: String, val serverVersion: String, val message: String) : Connection()
        
        /**
         * Notificación de desconexión voluntaria.
         * @param reason Motivo del cierre (solicitud de usuario, error, etc.)
         */
        @Serializable @SerialName("connection_disconnect")
        data class Disconnect(val reason: String = "Client requested disconnect") : Connection()
    }
    
    /**
     * Mensajes de control de bajo nivel (latidos, pings).
     */
    @Serializable
    sealed class System : GameMessage() {
        /** Mensaje de comprobación de latencia enviado periódicamente. */
        @Serializable @SerialName("system_ping")
        data class Ping(val timestamp: Long = 0L) : System()
        
        /** Respuesta al ping. */
        @Serializable @SerialName("system_pong")
        data class Pong(val timestamp: Long = 0L) : System()
    }
    
    // ==================== MATCHMAKING ====================
    /**
     * Mensajes para la búsqueda de partidas y gestión de salas.
     */
    @Serializable
    sealed class Matchmaking : GameMessage() {
        /** Solicita buscar una partida contra otro jugador humano. */
        @Serializable @SerialName("matchmaking_find_pvp")
        data class FindPvP(val gameConfig: GameConfig) : Matchmaking()
        
        /** Solicita iniciar una partida contra la IA. */
        @Serializable @SerialName("matchmaking_find_pve")
        data class FindPvE(val gameConfig: GameConfig, val difficulty: AIDifficulty = AIDifficulty.MEDIUM) : Matchmaking()
        
        /** Cancela la búsqueda actual. */
        @Serializable @SerialName("matchmaking_cancel")
        data object CancelSearch : Matchmaking()
        
        /** Notifica que se ha encontrado un oponente y la partida va a comenzar. */
        @Serializable @SerialName("matchmaking_found")
        data class MatchFound(
            val gameId: String, 
            val opponentName: String, 
            val isPvE: Boolean,
            val config: GameConfig
        ) : Matchmaking()
        
        /** Informa sobre la posición en la cola de espera. */
        @Serializable @SerialName("matchmaking_waiting")
        data class Waiting(val playersInQueue: Int) : Matchmaking()

        /** Informa sobre el estado de un lobby (jugadores conectados, etc.). */
        @Serializable @SerialName("matchmaking_lobby_status")
        data class LobbyStatus(
            val lobbyId: String,
            val players: List<String>,
            val maxPlayers: Int = 2,
            val config: GameConfig,
            val roomCode: String? = null
        ) : Matchmaking()

        /** Solicita crear una sala privada protegida por código. */
        @Serializable @SerialName("matchmaking_create_room")
        data class CreateRoom(val config: GameConfig) : Matchmaking()

        /** Solicita unirse a una sala privada mediante su código. */
        @Serializable @SerialName("matchmaking_join_room")
        data class JoinRoom(val roomCode: String) : Matchmaking()

        /** Notifica que la sala privada se ha creado con éxito. */
        @Serializable @SerialName("matchmaking_room_created")
        data class RoomCreated(val roomId: String, val roomCode: String, val config: GameConfig) : Matchmaking()
        
        /** Notifica que un jugador se ha unido con éxito a la sala. */
        @Serializable @SerialName("matchmaking_room_joined")
        data class RoomJoined(val roomId: String, val roomCode: String, val config: GameConfig) : Matchmaking()

        /** Informa de un error al intentar crear o unirse a una sala. */
        @Serializable @SerialName("matchmaking_room_error")
        data class RoomError(val message: String) : Matchmaking()
    }
    
    /**
     * Acciones proactivas que un jugador envía durante la partida.
     */
    @Serializable
    sealed class Action : GameMessage() {
        
        /** Envía la disposición final de los barcos al servidor. */
        @Serializable
        @SerialName("action_place_ships")
        data class PlaceShips(
            val ships: List<ShipPlacement>
        ) : Action()
        
        /** Realiza un disparo en una coordenada específica. */
        @Serializable
        @SerialName("action_attack")
        data class Attack(
            val coordinate: Coordinate
        ) : Action()
        
        /** Notifica la rendición del jugador. */
        @Serializable
        @SerialName("action_surrender")
        data object Surrender : Action()

        /** Inicia directamente una partida contra la IA (usado tras matchmaking). */
        @Serializable
        @SerialName("action_start_pve")
        data class StartGamePVE(val config: GameConfig, val difficulty: AIDifficulty = AIDifficulty.MEDIUM) : Action()

        /** Confirma que el despliegue es válido (paso previo a comenzar BATTLE). */
        @Serializable
        @SerialName("action_confirm_deployment")
        data class ConfirmDeployment(val ships: List<ShipPlacement>) : Action()

        /** Abandona la partida actual. */
        @Serializable
        @SerialName("action_leave_game")
        data object LeaveGame : Action()
    }
    
    // ==================== RESPUESTAS DEL SERVIDOR ====================
    /**
     * Mensajes reactivos enviados por el servidor para informar de cambios de estado.
     */
    @Serializable
    sealed class Response : GameMessage() {
        
        /** Informa del estado completo de la partida (fase, turnos, tableros). */
        @Serializable
        @SerialName("response_game_state")
        data class GameState(
            val phase: GamePhase,
            val currentTurn: String,
            val turnTimeSeconds: Int,
            val myBoard: BoardState,
            val opponentBoard: BoardState,
            val status: String
        ) : Response()

        /** Notifica el inicio formal de una partida. */
        @Serializable
        @SerialName("response_game_start")
        data class GameStart(
            val gameId: String,
            val opponentName: String,
            val turnTimeSeconds: Int
        ) : Response()

        /** Informa del resultado de un ataque realizado por el jugador. */
        @Serializable
        @SerialName("response_attack_result")
        data class AttackResult(
            val pos: Coordinate,
            val result: ShotResult,
            val sunk: Boolean,
            val shipSunkType: ShipType? = null
        ) : Response()
        
        /** Informa del resultado de un ataque realizado por el oponente. */
        @Serializable
        @SerialName("response_opponent_attack")
        data class OpponentAttackResult(
            val pos: Coordinate,
            val result: ShotResult,
            val sunk: Boolean,
            val shipSunkType: ShipType? = null
        ) : Response()

        /** Notifica un cambio de turno o actualización del cronómetro. */
        @Serializable
        @SerialName("response_turn_update")
        data class TurnUpdate(
            val playerId: String,
            val secondsLeft: Int
        ) : Response()
        
        /** Notifica el fin de la partida. */
        @Serializable
        @SerialName("response_game_over")
        data class GameOver(
            val winnerId: String,
            val stats: GameStats
        ) : Response()
        
        /** Notifica el resultado de una ronda (en partidas al mejor de N). */
        @Serializable
        @SerialName("response_round_result")
        data class RoundResult(
            val roundNumber: Int,
            val winnerId: String,
            val player1Score: Int,
            val player2Score: Int,
            val isMatchOver: Boolean,
            val stats: GameStats
        ) : Response()
        
        /** Confirma que el servidor ha aceptado la colocación de un barco. */
        @Serializable
        @SerialName("response_placement_confirmed")
        data class PlacementConfirmed(
            val shipType: ShipType
        ) : Response()
    }
    
    /**
     * Mensajes para la consulta y reporte de estadísticas del jugador.
     */
    @Serializable
    sealed class Stats : GameMessage() {
        /** Solicita las estadísticas actuales y el leaderboard. */
        @Serializable @SerialName("stats_request")
        data object Request : Stats()
        
        /** Envía las estadísticas solicitadas. */
        @Serializable @SerialName("stats_response")
        data class Response(val playerStats: PlayerStats, val leaderboard: List<LeaderboardEntry>) : Stats()
        
        /** Notifica una actualización en el perfil del jugador. */
        @Serializable @SerialName("stats_update")
        data class Update(val playerStats: PlayerStats) : Stats()
    }
    
    /**
     * Mensajes de error para informar de fallos lógicos o de red.
     */
    @Serializable
    sealed class Error : GameMessage() {
        /** La acción enviada no es válida según el estado del juego. */
        @Serializable @SerialName("error_invalid")
        data class InvalidAction(val message: String, val details: String? = null) : Error()
        
        /** Error interno inesperado en el servidor. */
        @Serializable @SerialName("error_server")
        data class ServerError(val message: String, val code: Int = 500) : Error()
        
        /** Error crítico en la capa de transporte. */
        @Serializable @SerialName("error_connection")
        data class ConnectionError(val message: String) : Error()
    }
}

// ==================== ENUMS Y TIPOS ====================

/** Dificultad de la IA */
@Serializable
enum class AIDifficulty { EASY, MEDIUM, HARD }

/** Orientación de un barco en el tablero */
@Serializable
enum class Orientation { HORIZONTAL, VERTICAL }

/** Resultado de un disparo individual */
@Serializable
enum class ShotResult { MISS, HIT, SUNK }

/** Motivo por el cual la partida ha terminado */
@Serializable
enum class GameOverReason { ALL_SHIPS_SUNK, SURRENDER, DISCONNECT, TIMEOUT }

/** 
 * Tipos de barcos disponibles y su tamaño en casillas.
 * @param size Número de casillas que ocupa el barco.
 */
@Serializable
enum class ShipType(val size: Int) {
    CARRIER(5),    // Portaaviones
    BATTLESHIP(4), // Acorazado
    CRUISER(3),    // Crucero
    DESTROYER(2),  // Destructor
    PATROL_BOAT(1) // Patrullera
}

// ==================== MODELOS DE DATOS ====================

/** Coordenada (X, Y) bidimensional */
@Serializable
data class Coordinate(val x: Int, val y: Int)

/** Define la posición y orientación de un barco colocado */
@Serializable
data class ShipPlacement(
    val type: ShipType,
    val coordinate: Coordinate,
    val orientation: Orientation
)

/** Resumen de rendimiento de una única partida */
@Serializable
data class GameStats(
    val totalShots: Int,
    val successfulHits: Int,
    val shipsDestroyed: Int,
    val accuracy: Double,
    val winStreak: Int,
    val gameDurationSeconds: Long = 0
)

/** Perfil persistente con el histórico del jugador */
@Serializable
data class PlayerStats(
    val playerName: String,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    
    val pvpWon: Int = 0,
    val pvpLost: Int = 0,
    val pveWon: Int = 0,
    val pveLost: Int = 0,
    
    val totalShots: Int,
    val totalHits: Int,
    val winStreak: Int,
    val bestWinStreak: Int,
    val averageAccuracy: Double,
    
    val bestAccuracy: Double = 0.0,
    val fastestWinTurns: Int = Int.MAX_VALUE,
    
    val totalPlayTimeSeconds: Long
)

/** Entrada individual en el ranking global */
@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val playerName: String,
    val gamesWon: Int,
    val winRate: Double,
    val bestStreak: Int
)

/** Estado de una celda individual en el radar */
@Serializable
enum class CellState { EMPTY, SHIP, HIT, MISS }

/** Fase actual del flujo de juego */
@Serializable
enum class GamePhase { PLACEMENT, BATTLE, FINISHED }

/** Instantánea del estado de un tablero para enviar al cliente */
@Serializable
data class BoardState(
    val size: Int = 10,
    val cells: List<List<CellState>>,
    val ships: List<ShipState>? = null
)

/** Representación del estado actual de un barco específico */
@Serializable
data class ShipState(
    val type: ShipType,
    val coordinates: List<Coordinate>,
    val sunk: Boolean
)
