package com.lfigueira.hundir_la_flota.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lfigueira.hundir_la_flota.common.AppLogger
import com.lfigueira.hundir_la_flota.common.client.ConnectionState
import com.lfigueira.hundir_la_flota.common.client.GameClient
import com.lfigueira.hundir_la_flota.common.models.GameConfig
import com.lfigueira.hundir_la_flota.common.protocol.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.lfigueira.hundir_la_flota.ui.theme.CyberColors
import androidx.compose.ui.graphics.Color

/**
 * ViewModel para gestión de estado de la UI.
 * Integra el GameClient con la UI de Compose usando Flows reactivos.
 */
class GameViewModel(
    private val gameClient: GameClient = GameClient()
) : ViewModel() {
    
    // Estado de conexión (expuesto desde el cliente)
    val connectionState: StateFlow<ConnectionState> = gameClient.connectionState
    
    // Estado de la UI
    private val _uiState = MutableStateFlow<UIState>(UIState.MainMenu)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()
    
    // Estadísticas del jugador
    private val _playerStats = MutableStateFlow<PlayerStats?>(null)
    val playerStats: StateFlow<PlayerStats?> = _playerStats.asStateFlow()
    
    // Leaderboard
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()
    
    // Estado del juego actual
    private val _currentGameState = MutableStateFlow<GameMessage.Response.GameState?>(null)
    val currentGameState: StateFlow<GameMessage.Response.GameState?> = _currentGameState.asStateFlow()
    
    // Mensajes de error/notificación
    private val _notification = MutableStateFlow<String?>(null)
    val notification: StateFlow<String?> = _notification.asStateFlow()

    // Historial de batalla
    private val _battleEvents = MutableStateFlow<List<BattleEvent>>(emptyList())
    val battleEvents: StateFlow<List<BattleEvent>> = _battleEvents.asStateFlow()
    
    // Configuración actual del juego
    private val _currentConfig = MutableStateFlow(GameConfig())
    val currentConfig: StateFlow<GameConfig> = _currentConfig.asStateFlow()
    
    // Nombre del jugador
    private var playerName: String = ""
    
    init {
        // Observar mensajes entrantes del servidor
        viewModelScope.launch {
            gameClient.incomingMessages.collect { message ->
                handleServerMessage(message)
            }
        }
        
        // Observar estado de conexión para navegación automática
        viewModelScope.launch {
            gameClient.connectionState.collect { state ->
                if (state is ConnectionState.Disconnected && _uiState.value !is UIState.MainMenu) {
                    AppLogger.info("GameViewModel", "Desconexión detectada mientras no estaba en menú. Navegando al menú.")
                    // Asegurar que estamos en el hilo principal si fuera necesario, aunque StateFlow es thread-safe
                    resetGameState()
                    _uiState.value = UIState.MainMenu
                }
            }
        }
    }
    
    /**
     * Conecta al servidor.
     */
    fun connect(name: String, host: String = "localhost", port: Int = 5678) {
        playerName = name
        viewModelScope.launch {
            try {
                AppLogger.info("GameViewModel", "Iniciando conexión para $name a $host:$port")
                val success = gameClient.connectAndInitialize(host, port, playerName)
                if (!success) {
                    _notification.value = "Error: No se pudieron cargar los datos iniciales"
                    AppLogger.error("GameViewModel", "Fallo inicialización tras conexión", null)
                }
            } catch (e: Exception) {
                _notification.value = "Error al conectar: ${e.message}"
                AppLogger.error("GameViewModel", "Excepción conectando: ${e.message}", e)
            }
        }
    }
    
    /**
     * Desconecta del servidor.
     */
    fun disconnect() {
        viewModelScope.launch {
            AppLogger.info("GameViewModel", "Usuario solicitó desconexión.")
            gameClient.disconnect()
            _uiState.value = UIState.MainMenu
        }
    }
    
    /**
     * Busca una partida PVP.
     */
    fun findPvPMatch() {
        viewModelScope.launch {
            val config = _currentConfig.value
            AppLogger.info("GameViewModel", "Iniciando matchmaking PVP. BoardSize=${config.boardSize}, TurnTime=${config.turnTimeSeconds}")
            
            if (config.boardSize == 0 || config.boardSize < 8 || config.boardSize > 15) {
                AppLogger.error("GameViewModel", "Config inválido: boardSize=${config.boardSize}. Usando default.", null)
                _currentConfig.value = GameConfig(boardSize = 10)
            }
            
            gameClient.send(GameMessage.Matchmaking.FindPvP(_currentConfig.value))
            _uiState.value = UIState.Matchmaking
        }
    }
    
    /**
     * Busca una partida PVE.
     */
    fun findPvEMatch(difficulty: AIDifficulty = AIDifficulty.MEDIUM) {
        viewModelScope.launch {
            val config = _currentConfig.value
            AppLogger.info("GameViewModel", "Iniciando partida PVE vs $difficulty. BoardSize=${config.boardSize}, TurnTime=${config.turnTimeSeconds}, MaxRounds=${config.maxRounds}")
            
            if (config.boardSize == 0 || config.boardSize < 8 || config.boardSize > 15) {
                AppLogger.error("GameViewModel", "Config inválido: boardSize=${config.boardSize}. Usando default.", null)
                _currentConfig.value = GameConfig(boardSize = 10)
            }
            
            gameClient.send(GameMessage.Action.StartGamePVE(_currentConfig.value, difficulty))
        }
    }
    
    // --- MÉTODOS SALA PRIVADA ---
    fun createRoom() {
        viewModelScope.launch {
            val config = _currentConfig.value
            AppLogger.info("GameViewModel", "Creando sala privada. BoardSize=${config.boardSize}, TurnTime=${config.turnTimeSeconds}")
            
            if (config.boardSize == 0 || config.boardSize < 8 || config.boardSize > 15) {
                AppLogger.error("GameViewModel", "Config inválido: boardSize=${config.boardSize}. Usando default.", null)
                _currentConfig.value = GameConfig(boardSize = 10)
            }
            
            gameClient.send(GameMessage.Matchmaking.CreateRoom(_currentConfig.value))
        }
    }
    
    fun joinRoom(code: String) {
        viewModelScope.launch {
            AppLogger.info("GameViewModel", "Uniéndose a sala privada: $code")
            gameClient.send(GameMessage.Matchmaking.JoinRoom(code))
        }
    }
    // ----------------------------
    
    /**
     * Actualiza la configuración del juego.
     */
    fun updateConfig(newConfig: GameConfig) {
        _currentConfig.value = newConfig
        AppLogger.info("GameViewModel", "Configuración actualizada: $newConfig")
        // TODO: Persistir en archivo JSON local
    }
    
    /**
     * Cancela la búsqueda de partida.
     */
    fun cancelMatchmaking() {
        viewModelScope.launch {
            AppLogger.info("GameViewModel", "Matchmaking cancelado.")
            gameClient.send(GameMessage.Matchmaking.CancelSearch)
            _uiState.value = UIState.MainMenu
        }
    }
    
    /**
     * Navega a la pantalla de configuración.
     */
    fun navigateToSettings() {
        _uiState.value = UIState.Settings
    }
    
    // ID del jugador (asignado por el servidor)
    var myPlayerId: String = ""
        private set

    /**
     * Confirma el despliegue de la flota.
     */
    fun confirmDeployment(ships: List<ShipPlacement>) {
        viewModelScope.launch {
            AppLogger.debug("GameViewModel", "Confirmando despliegue de ${ships.size} barcos.")
            gameClient.send(GameMessage.Action.ConfirmDeployment(ships))
        }
    }

    /**
     * Dispara a una coordenada.
     */
    fun sendAttack(coordinate: Coordinate) {
        val currentTurn = _currentGameState.value?.currentTurn
        val myId = myPlayerId
        
        AppLogger.debug("GameViewModel", "Click en (${coordinate.x}, ${coordinate.y}). CurrentTurn=$currentTurn, MyPlayerId='$myId', Match=${currentTurn == myId}")
        
        if (currentTurn != myId) {
            AppLogger.info("GameViewModel", "Intento de disparo fuera de turno. CurrentTurn=$currentTurn, MyId=$myId")
            _notification.value = "No es tu turno"
            return
        }
        viewModelScope.launch {
            AppLogger.info("GameViewModel", "Enviando ataque a (${coordinate.x}, ${coordinate.y})")
            gameClient.send(GameMessage.Action.Attack(coordinate))
        }
    }
    
    /**
     * Se rinde.
     */
    fun surrender() {
        viewModelScope.launch {
            AppLogger.info("GameViewModel", "Usuario se rinde.")
            gameClient.send(GameMessage.Action.Surrender)
        }
    }
    
    /**
     * Aborta la partida y vuelve al menú principal.
     * Envía mensaje de abandono al servidor y limpia el estado local inmediatamente.
     */
    fun endSession() {
        AppLogger.info("GameViewModel", "Usuario solicita endSession(). Limpiando estado UI inmediatamente.")
        
        // 1. Limpieza Local Inmediata (Prioridad Crítica)
        _currentGameState.value = null // Detiene observadores de UI
        _battleEvents.value = emptyList()
        _notification.value = null
        
        viewModelScope.launch {
            // 2. Notificación al Servidor (Best Effort)
            try {
                gameClient.send(GameMessage.Action.LeaveGame)
            } catch (e: Exception) {
                AppLogger.error("GameViewModel", "Error enviando LeaveGame en endSession", e)
            }
            
            // 3. Navegación Absoluta
            _uiState.value = UIState.MainMenu
        }
    }
    
    /**
     * Solicita estadísticas.
     */
    fun requestStats() {
        viewModelScope.launch {
            AppLogger.debug("GameViewModel", "Solicitando estadísticas.")
            gameClient.send(GameMessage.Stats.Request)
        }
    }
    
    /**
     * Navega a la pantalla de records.
     */
    fun navigateToRecords() {
        AppLogger.debug("GameViewModel", "Navegando a Records.")
        _uiState.value = UIState.Records
        requestStats()
    }
    
    /**
     * Navega al menú principal y limpia el estado de la partida anterior.
     */
    fun navigateToMainMenu() {
        AppLogger.debug("GameViewModel", "Navegando al Menú Principal. Ejecutando cleanup de partida.")
        resetGameState()
        _uiState.value = UIState.MainMenu
    }

    fun navigateToOnlineMenu() {
        _uiState.value = UIState.OnlineMenu
    }
    
    /**
     * Resetea el estado interno para permitir una nueva partida limpia.
     */
    private fun resetGameState() {
        _currentGameState.value = null
        _battleEvents.value = emptyList()
        _notification.value = null
        // No reseteamos playerName ni connectionState
    }
    
    /**
     * Limpia las notificaciones.
     */
    fun clearNotification() {
        _notification.value = null
    }
    
    /**
     * Maneja los mensajes del servidor.
     */
    // Variables locales para persistencia de sesión
    private var activeGameId: String = ""
    private var activeOpponentName: String = ""
    
    // ... inside init or just above handleServerMessage
    
    private fun handleServerMessage(message: GameMessage) {
        // ... (check MainMenu logic same as before)
        if (_uiState.value is UIState.MainMenu && 
            message !is GameMessage.Matchmaking.MatchFound && 
            message !is GameMessage.Connection && 
            message !is GameMessage.Stats &&
            message !is GameMessage.Error) {
            AppLogger.debug("GameViewModel", "Ignorando mensaje ${message::class.simpleName} porque estamos en MainMenu")
            return
        }

        when (message) {
            is GameMessage.Matchmaking.MatchFound -> {
                AppLogger.info("GameViewModel", "Match encontrado: ${message.gameId} vs ${message.opponentName}")
                _notification.value = "Oponente encontrado: ${message.opponentName}"
                
                // GUARDAR DATOS DE SESIÓN
                activeGameId = message.gameId
                activeOpponentName = message.opponentName
                
                // ACTUALIZACIÓN DE CONFIGURACIÓN AUTORITATIVA DEL SERVIDOR
                _currentConfig.value = message.config
                AppLogger.info("GameViewModel", "Configuración sincronizada con server: ${message.config.boardSize}x${message.config.boardSize}")
                
                 _battleEvents.value = emptyList()
                
                _uiState.value = UIState.ShipPlacement(
                    gameId = activeGameId, 
                    opponentName = activeOpponentName
                )
            }
            
            // ... (LobbyStatus, Waiting same as before)
            is GameMessage.Matchmaking.LobbyStatus -> {
                 _uiState.value = UIState.Lobby(message)
            }
            
            // --- Nuevos handlers Sala Privada ---
            is GameMessage.Matchmaking.RoomCreated -> {
                AppLogger.info("GameViewModel", "Sala creada: ${message.roomCode}")
                // Usamos el mismo estado de Lobby pero ahora la UI puede mostrar el código
                // Necesitamos que LobbyStatus venga después, normalmente RoomManager lo manda.
                // Si no, podemos poner un estado Loading o esperar al LobbyStatus que RoomManager envía inmediatamente.
            }
            
            is GameMessage.Matchmaking.RoomJoined -> {
                AppLogger.info("GameViewModel", "Unido a sala: ${message.roomCode}")
                // Sync config
                 _currentConfig.value = message.config
            }
            
            is GameMessage.Matchmaking.RoomError -> {
                _notification.value = "Error Sala: ${message.message}"
                _uiState.value = UIState.MainMenu // Regresar si falla
            }
            // ------------------------------------
            
            is GameMessage.Matchmaking.Waiting -> {
                _notification.value = "Buscando... Cola: ${message.playersInQueue}"
            }
            
            is GameMessage.Response.GameState -> {
                _currentGameState.value = message
                // AppLogger.debug("GameViewModel", "GameState recibido. Fase: ${message.phase}")
                
                if (message.phase == GamePhase.PLACEMENT) {
                    if (_uiState.value !is UIState.ShipPlacement) {
                        AppLogger.info("GameViewModel", "Cambiando UI a ShipPlacement")
                        _uiState.value = UIState.ShipPlacement(
                            gameId = activeGameId, 
                            opponentName = activeOpponentName
                        )
                    }
                } else if (message.phase == GamePhase.BATTLE) {
                    if (_uiState.value !is UIState.Battle) {
                        AppLogger.info("GameViewModel", "Cambiando UI a Battle")
                         _battleEvents.value = emptyList() // Limpiar eventos si es primera vez
                        _uiState.value = UIState.Battle(
                            gameId = activeGameId
                        )
                    }
                }
            }
            
            is GameMessage.Response.AttackResult -> {
                val resultText = when (message.result) {
                    ShotResult.MISS -> "Agua"
                    ShotResult.HIT -> "¡Tocado!"
                    ShotResult.SUNK -> "¡Hundido! ${message.shipSunkType?.name}"
                }
                AppLogger.info("GameViewModel", "Resultado de mi ataque en (${message.pos.x}, ${message.pos.y}): $resultText")
                addBattleEvent("Tú disparas a (${message.pos.x}, ${message.pos.y}): $resultText", if(message.result != ShotResult.MISS) CyberColors.NeonBlue else Color.Gray)
            }
            
            is GameMessage.Response.OpponentAttackResult -> {
                val resultText = when (message.result) {
                    ShotResult.MISS -> "falló"
                    ShotResult.HIT -> "te dio"
                    ShotResult.SUNK -> "hundió tu ${message.shipSunkType?.name}"
                }
                AppLogger.info("GameViewModel", "Ataque enemigo en (${message.pos.x}, ${message.pos.y}): $resultText")
                addBattleEvent("Enemigo dispara a (${message.pos.x}, ${message.pos.y}) y $resultText", if(message.result != ShotResult.MISS) CyberColors.NeonRed else Color.Gray)
            }
            
            is GameMessage.Response.GameOver -> {
                val isWinner = message.winnerId == myPlayerId
                AppLogger.info("GameViewModel", "GameOver. Ganador: ${message.winnerId}. Soy ganador: $isWinner")
                addBattleEvent(if (isWinner) "¡MISIÓN CUMPLIDA! VICTORIA" else "ERROR CRÍTICO: DERROTA", if(isWinner) CyberColors.NeonGreen else CyberColors.NeonRed)
                _uiState.value = UIState.GameOver(message)
            }
            
            is GameMessage.Response.PlacementConfirmed -> {
                AppLogger.debug("GameViewModel", "Colocación confirmada: ${message.shipType.name}")
                _notification.value = "${message.shipType.name} colocado correctamente"
            }
            
            is GameMessage.Response.TurnUpdate -> {
                 // Update local state turn
                 val current = _currentGameState.value
                 if (current != null) {
                     _currentGameState.value = current.copy(
                         currentTurn = message.playerId,
                         turnTimeSeconds = message.secondsLeft,
                         status = if (message.playerId == myPlayerId) "Tu Turno (${message.secondsLeft}s)" else "Turno del Oponente (${message.secondsLeft}s)"
                     )
                 }
            }
            
            is GameMessage.Error.InvalidAction -> {
                AppLogger.info("GameViewModel", "Error del servidor (InvalidAction): ${message.message}")
                _notification.value = "Error: ${message.message}"
            }
            
            is GameMessage.Error.ServerError -> {
                AppLogger.error("GameViewModel", "Error crítico del servidor: ${message.message}", null)
                _notification.value = "Error del servidor: ${message.message}"
            }

            is GameMessage.Stats.Response -> {
                AppLogger.info("GameViewModel", "Estadísticas recibidas. Actualizando Leaderboard y PlayerStats.")
                _playerStats.value = message.playerStats
                _leaderboard.value = message.leaderboard
            }

            is GameMessage.Connection.Welcome -> {
                myPlayerId = message.playerId
                AppLogger.info("GameViewModel", "Welcome recibido. MyPlayerId establecido a: '$myPlayerId'")
            }
            
            else -> {}
        }
    }
    
    private fun addBattleEvent(message: String, color: Color) {
        val event = BattleEvent(message, color, System.currentTimeMillis())
        _battleEvents.value = (_battleEvents.value + event).takeLast(50)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            gameClient.disconnect()
        }
    }
}

data class BattleEvent(val message: String, val color: Color, val timestamp: Long)

/**
 * Estados de la UI.
 */
sealed class UIState {
    data object MainMenu : UIState()
    data object OnlineMenu : UIState() // Nuevo estado
    data object Settings : UIState()
    data object Matchmaking : UIState()
    data class ShipPlacement(val gameId: String, val opponentName: String) : UIState()
    data class Lobby(val status: GameMessage.Matchmaking.LobbyStatus) : UIState()
    data class Battle(val gameId: String) : UIState()
    data object Records : UIState()
    data class GameOver(val result: GameMessage.Response.GameOver) : UIState()
}
