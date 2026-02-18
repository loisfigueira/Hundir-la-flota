package com.lfigueira.hundir_la_flota.server.game

import com.lfigueira.hundir_la_flota.common.AppLogger
import com.lfigueira.hundir_la_flota.common.models.GameConfig
import com.lfigueira.hundir_la_flota.common.protocol.*
import com.lfigueira.hundir_la_flota.server.ClientHandler
import com.lfigueira.hundir_la_flota.server.persistence.RecordsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Gestiona una partida de Hundir la Flota entre dos jugadores (Player vs Player o Player vs Bot).
 * Mantiene el estado autoritativo, valida reglas y controla el flujo de turnos.
 */
class GameSession(
    val gameId: String,
    val player1: ClientHandler, // En PvE, este es el humano
    val player2: ClientHandler? = null, // En PvE es null
    val isPvE: Boolean = false,
    val difficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val config: GameConfig = GameConfig(), // Configuración dinámica del juego
    private val recordsManager: RecordsManager,
    private val onFinished: (String) -> Unit // Callback para limpieza
) {
    private val gameScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val gameStateMutex = Mutex()
    
    // Estado del juego
    private var isGameRunning = false
    private var currentTurnPlayerId: String = player1.clientId // Usar clientId
    private val botId = "BOT_HUNTER_ID"
    private val botName = "Bot Hunter (${difficulty.name})"
    private var turnTimerJob: Job? = null
    
    // Sistema de Rondas (Best of X)
    private var currentRound: Int = 1
    private var player1RoundsWon: Int = 0
    private var player2RoundsWon: Int = 0
    private val boardSize: Int = config.boardSize
    
    // Tableros autoritativos (Barcos reales)
    private val player1Ships = mutableListOf<ShipPlacement>()
    private val player2Ships = mutableListOf<ShipPlacement>()
    
    // Historial de disparos recibidos (para calcular stats y evitar repetidos)
    private val player1ReceivedShots = mutableListOf<Coordinate>() // Disparos que player1 RECIBIÓ
    private val player2ReceivedShots = mutableListOf<Coordinate>() // Disparos que player2 RECIBIÓ (o Bot)
    
    // AI Controller (solo para PvE)
    private val enemyAI = if (isPvE) EnemyAI(difficulty) else null
    
    // Stats tracking en memoria para esta partida
    private val p1Stats = GameSessionStats()
    private val p2Stats = GameSessionStats() // O Stats del Bot
    
    // Estado de preparación
    private var p1Ready = false
    private var p2Ready = isPvE // Bot siempre listo tras colocar barcos internamente
    
    init {
        // En PvE, generamos los barcos del bot inmediatamente
        if (isPvE) {
            setupBotShips()
            AppLogger.info("GameSession", "[GameSession-$gameId] Bot Hunter inicializado con ${player2Ships.size} barcos en tablero ${boardSize}x${boardSize}")
        }
    }

    /**
     * Inicia la fase de colocación.
     */
    suspend fun start() {
        AppLogger.info("GameSession", "[GameSession-$gameId] Iniciando partida. Enviando fase PLACEMENT.")
        // Al iniciar, enviamos el estado de PLACEMENT
        broadcastGameState()
    }

    /**
     * Envía el estado actual del juego a los jugadores.
     */
    suspend fun broadcastGameState() {
        val p1Msg = createGameStateMessage(player1)
        player1.sendMessage(p1Msg)
        
        player2?.let {
            val p2Msg = createGameStateMessage(it)
            it.sendMessage(p2Msg)
        }
    }

    private fun createGameStateMessage(forPlayer: ClientHandler): GameMessage.Response.GameState {
        val isP1 = forPlayer == player1
        val myShips = if (isP1) player1Ships else player2Ships
        val oppShots = if (isP1) player1ReceivedShots else player2ReceivedShots
        val myShots = if (isP1) player2ReceivedShots else player1ReceivedShots
        
        // Crear tableros para el mensaje
        val myBoard = BoardState(
            cells = generateCells(myShips, oppShots, revealShips = true),
            ships = if (isP1) player1Ships.map { ShipState(it.type, getShipCoordinates(it), false) } else player2Ships.map { ShipState(it.type, getShipCoordinates(it), false) }
        )
        val oppBoard = BoardState(
            cells = generateCells(if (isP1) player2Ships else player1Ships, myShots, revealShips = isGameRunning && false) // Ocultar barcos enemigos
        )
        
        return GameMessage.Response.GameState(
            phase = if (!isGameRunning) GamePhase.PLACEMENT else GamePhase.BATTLE,
            currentTurn = currentTurnPlayerId,
            turnTimeSeconds = config.turnTimeSeconds,
            myBoard = myBoard,
            opponentBoard = oppBoard,
            status = if (!isGameRunning) "Coloca tus barcos" else (if (currentTurnPlayerId == forPlayer.clientId) "Tu turno" else "Turno oponente")
        )
    }

    private fun generateCells(ships: List<ShipPlacement>, shots: List<Coordinate>, revealShips: Boolean): List<List<CellState>> {
        val grid = MutableList(boardSize) { MutableList(boardSize) { CellState.EMPTY } }
        
        if (revealShips) {
            for (ship in ships) {
                for (c in getShipCoordinates(ship)) {
                    grid[c.y][c.x] = CellState.SHIP
                }
            }
        }
        
        for (shot in shots) {
            val hit = ships.any { shipOccupies(it, shot) }
            grid[shot.y][shot.x] = if (hit) CellState.HIT else CellState.MISS
        }
        
        return grid
    }
    
    /**
     * Procesa una acción recibida de un jugador.
     */
    suspend fun handleAction(player: ClientHandler, action: GameMessage.Action) {
        // En fase de colocación (!isGameRunning), solo permitimos colocar barcos, confirmar despliegue o rendirse.
        if (!isGameRunning && action !is GameMessage.Action.PlaceShips && 
            action !is GameMessage.Action.ConfirmDeployment && action !is GameMessage.Action.Surrender) {
            
            AppLogger.info("GameSession", "[GameSession-$gameId] Acción de ${player.playerName} rechazada: fase incorrecta.")
            player.sendMessage(GameMessage.Error.InvalidAction("Juego no iniciado o fase incorrecta"))
            return
        }

        when (action) {
            is GameMessage.Action.PlaceShips -> handlePlaceShips(player, action.ships)
            is GameMessage.Action.ConfirmDeployment -> handlePlaceShips(player, action.ships)
            is GameMessage.Action.Attack -> handleAttack(player, action.coordinate)
            is GameMessage.Action.Surrender -> handleSurrender(player)
            is GameMessage.Action.LeaveGame -> handleLeaveGame(player)
            // Ready se maneja implícitamente tras PlaceShips en esta lógica simplificada
            // o podríamos añadir un paso explícito si el cliente lo requiere
             else -> {}
        }
    }
    
    private suspend fun handlePlaceShips(player: ClientHandler, ships: List<ShipPlacement>) {
        AppLogger.debug("GameSession", "[GameSession-$gameId] ${player.playerName} intenta colocar barcos.")
        gameStateMutex.withLock {
            if (player == player1 && p1Ready) {
                AppLogger.info("GameSession", "[GameSession-$gameId] ${player.playerName} (P1) ya está listo.")
                return 
            }
            if (player == player2 && p2Ready) {
                AppLogger.info("GameSession", "[GameSession-$gameId] ${player.playerName} (P2) ya está listo.")
                return
            }
            
            val valid = validateFleet(ships)
            if (!valid) {
                AppLogger.info("GameSession", "[GameSession-$gameId] Flota de ${player.playerName} INVÁLIDA.")
                player.sendMessage(GameMessage.Error.InvalidAction("Colocación de barcos inválida. Reglas: 1x4, 2x3, 3x2, 4x1, sin solapamientos."))
                return
            }
            
            if (player == player1) {
                player1Ships.clear()
                player1Ships.addAll(ships)
                p1Ready = true
                AppLogger.info("GameSession", "[GameSession-$gameId] ${player.playerName} (P1) listo.")
                player.sendMessage(GameMessage.Response.PlacementConfirmed(ShipType.BATTLESHIP))
            } else if (player == player2) {
                player2Ships.clear() // Debería estar vacío
                player2Ships.addAll(ships)
                p2Ready = true
                AppLogger.info("GameSession", "[GameSession-$gameId] ${player.playerName} (P2) listo.")
                player.sendMessage(GameMessage.Response.PlacementConfirmed(ShipType.BATTLESHIP))
            }
            
            checkStartGame()
        }
    }
    
    private suspend fun checkStartGame() {
        if (p1Ready && p2Ready) {
            AppLogger.debug("GameSession", "[GameSession-$gameId] Cambiando fase a BATTLE tras despliegue de ${player1.playerName}")
            AppLogger.info("GameSession", "[GameSession-$gameId] Ambos jugadores listos. Iniciando fase BATTLE.")
            isGameRunning = true
            currentTurnPlayerId = player1.clientId // Empezar con el ID del jugador 1
            
            val opponentName = if (isPvE) botName else player2?.playerName ?: "Unknown"
            
            player1.sendMessage(GameMessage.Response.GameStart(gameId, opponentName, config.turnTimeSeconds))
            player2?.sendMessage(GameMessage.Response.GameStart(gameId, player1.playerName, config.turnTimeSeconds))
            
            // IMPORTANTE: Informar del cambio de fase a BATTLE enviando el GameState actualizado
            broadcastGameState()
            
            startTurnTimer()
        } else {
            AppLogger.debug("GameSession", "[GameSession-$gameId] Esperando a que el otro jugador esté listo. (P1: $p1Ready, P2: $p2Ready)")
        }
    }

    
    private suspend fun handleAttack(player: ClientHandler?, coordinate: Coordinate) {
        gameStateMutex.withLock {
            if (!isGameRunning) return
            
            // Validar turno usando IDs
            val isP1Turn = currentTurnPlayerId == player1.clientId
            val isPlayer1Sender = player == player1
            
            if (player != null) { // Si es humano
                if (isPlayer1Sender && !isP1Turn) {
                    player.sendMessage(GameMessage.Error.InvalidAction("No es tu turno"))
                    return
                }
                if (!isPlayer1Sender && isP1Turn) {
                     player.sendMessage(GameMessage.Error.InvalidAction("No es tu turno"))
                     return
                }
            } else {
                // Es Bot. Verificar que es turno del bot (que es NO P1Turn)
                if (isP1Turn) {
                    AppLogger.debug("GameSession", "Bot Hunter intentó disparar fuera de turno (Le toca a P1)")
                    return
                }
            }
            
            // Determinar objetivo
            val targetShips = if (isP1Turn) player2Ships else player1Ships
            val previousShots = if (isP1Turn) player2ReceivedShots else player1ReceivedShots
            
            // Validar coordenadas y repetidos
            if (coordinate.x < 0 || coordinate.x >= boardSize || coordinate.y < 0 || coordinate.y >= boardSize) return
            if (previousShots.contains(coordinate)) {
                 player?.sendMessage(GameMessage.Error.InvalidAction("Ya has disparado ahí"))
                 // Si es bot, la IA debería evitar esto, pero por seguridad retornamos
                 return
            }
            
            previousShots.add(coordinate)
            val shooterStats = if (isP1Turn) p1Stats else p2Stats
            shooterStats.shotsFired++
            shooterStats.turns++ // Incrementar turnos / intentos de disparo
            
            // Calcular resultado
            var result = ShotResult.MISS
            var sunkShipType: ShipType? = null
            var allSunk = false
            
            val shipHit = targetShips.find { ship ->
                 shipOccupies(ship, coordinate)
            }
            
            if (shipHit != null) {
                result = ShotResult.HIT
                shooterStats.hits++
                
                // Verificar Hundido
                val shipCoords = getShipCoordinates(shipHit)
                val isSunk = shipCoords.all { previousShots.contains(it) }
                
                if (isSunk) {
                    result = ShotResult.SUNK
                    sunkShipType = shipHit.type
                    shooterStats.shipsSunk++
                    
                    // Comprobar Victoria (Todos hundidos)
                    allSunk = targetShips.all { s ->
                         getShipCoordinates(s).all { previousShots.contains(it) }
                    }
                }
            }
            
            // Notificar resultados
            val attackResult = GameMessage.Response.AttackResult(coordinate, result, result == ShotResult.SUNK, sunkShipType)
            val oppAttackResult = GameMessage.Response.OpponentAttackResult(coordinate, result, result == ShotResult.SUNK, sunkShipType)
            
            if (isP1Turn) {
                player1.sendMessage(attackResult)
                player2?.sendMessage(oppAttackResult)
            } else {
                player2?.sendMessage(attackResult)
                player1.sendMessage(oppAttackResult)
                
                if (isPvE && player == null) {
                     // Feedback para la IA
                     enemyAI?.notifyShotResult(result, coordinate, sunkShipType != null)
                }
            }
            
            if (allSunk) {
                finishGame(winner = if (isP1Turn) player1 else player2)
            } else {
                // El cambio de turno ahora se encarga de notificar
                AppLogger.debug("GameSession", "[GameSession-$gameId] Disparo procesado. Cambiando turno. Turno actual era: $currentTurnPlayerId")
                switchTurn()
            }
        }
    }
    
    private suspend fun broadcastTurn() {
        val msg = GameMessage.Response.TurnUpdate(currentTurnPlayerId, 60)
        player1.sendMessage(msg)
        player2?.sendMessage(msg)
    }
    
    private fun switchTurn() {
        turnTimerJob?.cancel()
        
        // Reset del Timer: Volver a config.turnTimeSeconds antes de cualquier otra acción
        val nextTimeLeft = config.turnTimeSeconds
        
        currentTurnPlayerId = if (currentTurnPlayerId == player1.clientId) {
            player2?.clientId ?: botId
        } else {
            player1.clientId
        }
        
        AppLogger.info("GameSession", "[GameSession-$gameId] Cambiando turno a $currentTurnPlayerId. Proyectando disparo de IA: ${isPvE && currentTurnPlayerId == botId}")
        
        // Informar del cambio de turno y del estado completo inmediatamente
        gameScope.launch { 
            broadcastTurnUpdate(nextTimeLeft) 
            broadcastGameState() // Sincronización crucial
        }

        // Si ahora le toca al Bot
        if (isPvE && currentTurnPlayerId == botId) {
             // Disparo Proactivo: No esperar al timer. Delay reducido a 1s.
             gameScope.launch { 
                 delay(1000) 
                 processAITurn()
             }
        } else {
            startTurnTimer()
        }
    }
    
    /**
     * Realiza el disparo automático de la IA con un retraso realista.
     */
    private suspend fun processAITurn() {
        if (!isGameRunning || !isPvE) {
            AppLogger.debug("GameSession", "[GameSession-$gameId] processAITurn abortado: isGameRunning=$isGameRunning, isPvE=$isPvE")
            return
        }
        
        AppLogger.debug("GameSession", "[GameSession-$gameId] IA empezando ciclo de disparo...")
        
        var shot: Coordinate? = null
        try {
            // Intentar obtener disparo de la IA lógica
            shot = gameStateMutex.withLock {
                enemyAI?.getNextShot(player1ReceivedShots)
            }
        } catch (e: Exception) {
            AppLogger.error("GameSession", "[GameSession-$gameId] Error en lógica de IA: ${e.message}", e)
        }
        
        // Fallback Aleatorio si la IA falla o el disparo es nulo
        if (shot == null) {
            AppLogger.info("GameSession", "[GameSession-$gameId] IA disparando de forma aleatoria (fallback).")
            shot = generateRandomValidShot()
        }
        
        if (shot != null) {
            AppLogger.info("GameSession", "[GameSession-$gameId] IA confirma disparo a (${shot.x}, ${shot.y})")
            handleAttack(null, shot)
        } else {
            // Caso extremo: Tablero lleno o error crítico
            AppLogger.info("GameSession", "[GameSession-$gameId] IA sin coordenadas disponibles. Forzando cambio de turno.")
            switchTurn()
        }
    }

    /**
     * Genera una coordenada aleatoria válida (no disparada previamente).
     */
    private fun generateRandomValidShot(): Coordinate? {
        val available = mutableListOf<Coordinate>()
        for (x in 0 until boardSize) {
            for (y in 0 until boardSize) {
                val coord = Coordinate(x, y)
                if (!player1ReceivedShots.contains(coord)) {
                    available.add(coord)
                }
            }
        }
        return if (available.isNotEmpty()) available.random() else null
    }
    
    private fun startTurnTimer() {
        turnTimerJob?.cancel()
        turnTimerJob = gameScope.launch {
            var timeLeft = config.turnTimeSeconds
            while (timeLeft >= 0 && isGameRunning) {
                // Notificar tiempo restante cada segundo de forma explícita
                broadcastTurnUpdate(timeLeft)
                delay(1000)
                timeLeft--
            }
            if (isGameRunning) {
                AppLogger.info("GameSession", "[GameSession-$gameId] Tiempo agotado para $currentTurnPlayerId. Saltando turno.")
                switchTurn()
            }
        }
    }

    private suspend fun broadcastTurnUpdate(seconds: Int) {
        val msg = GameMessage.Response.TurnUpdate(currentTurnPlayerId, seconds)
        player1.sendMessage(msg)
        player2?.sendMessage(msg)
    }

    
    // Método privado, llamado por handleAction
    /**
     * Maneja el abandono voluntario de la partida.
     * El jugador que abandona pierde y el oponente gana automáticamente.
     */
    private fun handleLeaveGame(player: ClientHandler) {
        val leaver = player
        val opponent = if (leaver == player1) player2 else player1
        
        AppLogger.info("GameSession", "[GameSession-$gameId] Jugador ${leaver.playerName} ha abandonado la partida. Procesando derrota por abandono.")
        
        gameScope.launch {
            try {
                // En PvP/PvE, el que abandona pierde la partida completa
                val leaverIsP1 = leaver == player1
                
                // Calcular stats finales (abandonador = perdedor, oponente = ganador)
                val leaverFinalStats = calculateGameStats(if (leaverIsP1) p1Stats else p2Stats, isWinner = false)
                val opponentFinalStats = calculateGameStats(if (leaverIsP1) p2Stats else p1Stats, isWinner = true)
                
                val winnerId = opponent?.clientId ?: botId
                val winnerName = opponent?.playerName ?: botName

                // 1. Notificar GameOver a ambos
                leaver.sendMessage(GameMessage.Response.GameOver(winnerId, leaverFinalStats))
                opponent?.sendMessage(GameMessage.Response.GameOver(winnerId, opponentFinalStats))

                // 2. Persistencia: Actualizar records de ambos
                AppLogger.info("GameSession", "[GameSession-$gameId] Guardando records por abandono de ${leaver.playerName}. Ganador: $winnerName")
                
                // Estadísticas del abandonador
                recordsManager.updatePlayerStats(
                    leaver.playerName,
                    won = false,
                    shots = leaverFinalStats.totalShots,
                    hits = leaverFinalStats.successfulHits,
                    playTimeSeconds = leaverFinalStats.gameDurationSeconds,
                    isPvP = !isPvE,
                    turns = if (leaverIsP1) p1Stats.turns else p2Stats.turns
                )

                // Estadísticas del ganador (oponente humano)
                if (opponent != null) {
                    recordsManager.updatePlayerStats(
                        opponent.playerName,
                        won = true,
                        shots = opponentFinalStats.totalShots,
                        hits = opponentFinalStats.successfulHits,
                        playTimeSeconds = opponentFinalStats.gameDurationSeconds,
                        isPvP = true,
                        turns = if (!leaverIsP1) p1Stats.turns else p2Stats.turns
                    )
                }
            } catch (e: Exception) {
                AppLogger.error("GameSession", "Error procesando abandono: ${e.message}", e)
            } finally {
                // 3. Limpiar sesión
                dispose()
            }
        }
    }

    private suspend fun handleSurrender(player: ClientHandler) {
        val winner = if (player == player1) player2 else player1
        finishGame(winner, GameOverReason.SURRENDER)
    }
    
    private suspend fun finishGame(winner: ClientHandler?, reason: GameOverReason = GameOverReason.ALL_SHIPS_SUNK) {
        val winnerName = winner?.playerName ?: botName
        val winnerId = winner?.clientId ?: botId
        AppLogger.info("GameSession", "[GameSession-$gameId] Ronda $currentRound finalizada. Ganador: $winnerName. Razón: $reason")
        isGameRunning = false
        turnTimerJob?.cancel()
        
        // Actualizar contador de rondas ganadas
        if (winner == player1) {
            player1RoundsWon++
        } else {
            player2RoundsWon++
        }
        
        // Calcular stats finales de esta ronda
        val p1FinalStats = calculateGameStats(p1Stats, isWinner = winner == player1)
        val p2FinalStats = calculateGameStats(p2Stats, isWinner = winner == player2 || (winner == null && isPvE)) 
        
        // Verificar si se alcanzó el objetivo (Best of X)
        val roundsToWin = config.roundsToWin()
        val isMatchOver = player1RoundsWon >= roundsToWin || player2RoundsWon >= roundsToWin
        
        AppLogger.info("GameSession", "[GameSession-$gameId] Puntuación: P1=$player1RoundsWon, P2=$player2RoundsWon. Match over: $isMatchOver")
        
        // Enviar resultado de la ronda
        val roundResult = GameMessage.Response.RoundResult(
            roundNumber = currentRound,
            winnerId = winnerId,
            player1Score = player1RoundsWon,
            player2Score = player2RoundsWon,
            isMatchOver = isMatchOver,
            stats = p1FinalStats
        )
        
        player1.sendMessage(roundResult)
        player2?.sendMessage(roundResult)
        
        if (isMatchOver) {
            // Match completo - Enviar GameOver y limpiar sesión
            AppLogger.info("GameSession", "[GameSession-$gameId] Match finalizado. Ganador final: $winnerName")
            
            player1.sendMessage(GameMessage.Response.GameOver(winnerId, p1FinalStats))
            player2?.sendMessage(GameMessage.Response.GameOver(winnerId, p2FinalStats))
            
            // Persistencia
            AppLogger.debug("GameSession", "[GameSession-$gameId] Actualizando estadísticas en RecordsManager.")
            try {
                recordsManager.updatePlayerStats(
                    player1.playerName, 
                    winner == player1, 
                    p1FinalStats.totalShots,
                    p1FinalStats.successfulHits,
                    p1FinalStats.gameDurationSeconds,
                    isPvP = !isPvE,
                    turns = p1Stats.turns
                )
                if (player2 != null) {
                    recordsManager.updatePlayerStats(
                        player2.playerName, 
                        winner == player2, 
                        p2FinalStats.totalShots,
                        p2FinalStats.successfulHits,
                        p2FinalStats.gameDurationSeconds,
                        isPvP = true,
                        turns = p2Stats.turns
                    )
                }
            } catch (e: Exception) {
                AppLogger.error("GameSession", "[GameSession-$gameId] Error actualizando records: ${e.message}", e)
            }
            
            // Finalizar recursos
            dispose()
        } else {
            // Preparar siguiente ronda
            AppLogger.info("GameSession", "[GameSession-$gameId] Preparando ronda ${currentRound + 1}")
            startNextRound()
        }
    }
    
    /**
     * Prepara la siguiente ronda: resetea tableros, stats y vuelve a fase de colocación.
     */
    private suspend fun startNextRound() {
        currentRound++
        
        // Resetear estado de juego
        player1Ships.clear()
        player2Ships.clear()
        player1ReceivedShots.clear()
        player2ReceivedShots.clear()
        
        // Resetear stats de sesión
        p1Stats.shotsFired = 0
        p1Stats.hits = 0
        p1Stats.shipsSunk = 0
        p1Stats.turns = 0
        
        p2Stats.shotsFired = 0
        p2Stats.hits = 0
        p2Stats.shipsSunk = 0
        p2Stats.turns = 0
        
        // Resetear flags de preparación
        p1Ready = false
        p2Ready = isPvE // Bot siempre listo tras colocar barcos
        
        // En PvE, regenerar barcos del bot
        if (isPvE) {
            setupBotShips()
        }
        
        // Volver a fase de colocación
        isGameRunning = false
        currentTurnPlayerId = player1.clientId
        
        AppLogger.info("GameSession", "[GameSession-$gameId] Ronda $currentRound iniciada. Fase: PLACEMENT")
        
        // Notificar a los jugadores del nuevo estado
        broadcastGameState()
    }
    
    /**
     * Limpia recursos y libera el juego del mapa de sesiones.
     */
    fun dispose() {
        if (!isGameRunning && turnTimerJob == null) return // Evitar doble dispose si es posible
        
        AppLogger.info("GameSession", "[GameSession-$gameId] Limpiando sesión ID: $gameId. Jugadores liberados.")
        isGameRunning = false
        turnTimerJob?.cancel()
        turnTimerJob = null
        gameScope.cancel()
        
        // Notificar a GameLogic para remover esta sesión
        onFinished(gameId)
    }
    
    private fun validateFleet(ships: List<ShipPlacement>): Boolean {
        // Obtener la flota esperada de la configuración
        val expectedFleet = config.getExpandedFleet()
        
        if (ships.size != expectedFleet.size) {
            AppLogger.debug("GameSession", "[GameSession-$gameId] Validación fallida: se esperaban ${expectedFleet.size} barcos, recibidos ${ships.size}")
            return false
        }
        
        // Verificar que los tipos y cantidades coincidan
        val receivedCounts = ships.groupingBy { it.type }.eachCount()
        val expectedCounts = expectedFleet.groupingBy { it }.eachCount()
        
        for ((type, count) in expectedCounts) {
            if (receivedCounts[type] != count) {
                AppLogger.debug("GameSession", "[GameSession-$gameId] Validación fallida: se esperaban $count de $type, recibidos ${receivedCounts[type] ?: 0}")
                return false
            }
        }
        
        // Verificar que no haya solapamientos y que estén dentro del tablero
        val occupied = mutableSetOf<Coordinate>()
        for (ship in ships) {
            val coords = getShipCoordinates(ship)
            for (c in coords) {
                if (c.x !in 0 until boardSize || c.y !in 0 until boardSize) {
                    AppLogger.debug("GameSession", "[GameSession-$gameId] Validación fallida: coordenada fuera del tablero ($c)")
                    return false
                }
                if (!occupied.add(c)) {
                    AppLogger.debug("GameSession", "[GameSession-$gameId] Validación fallida: solapamiento en $c")
                    return false
                }
            }
        }
        return true
    }
    
    private fun getShipCoordinates(ship: ShipPlacement): List<Coordinate> {
        val coords = mutableListOf<Coordinate>()
        for (i in 0 until ship.type.size) {
            val x = if (ship.orientation == Orientation.HORIZONTAL) ship.coordinate.x + i else ship.coordinate.x
            val y = if (ship.orientation == Orientation.VERTICAL) ship.coordinate.y + i else ship.coordinate.y
            coords.add(Coordinate(x, y))
        }
        return coords
    }
    
    private fun shipOccupies(ship: ShipPlacement, coord: Coordinate): Boolean {
        return getShipCoordinates(ship).contains(coord)
    }
    
    private fun calculateGameStats(sessionStats: GameSessionStats, isWinner: Boolean): GameStats {
        val accuracy = if (sessionStats.shotsFired > 0) 
            (sessionStats.hits.toDouble() / sessionStats.shotsFired) * 100 
        else 0.0
            
        return GameStats(
            totalShots = sessionStats.shotsFired,
            successfulHits = sessionStats.hits,
            shipsDestroyed = sessionStats.shipsSunk,
            accuracy = accuracy,
            winStreak = if (isWinner) 1 else 0 // Racha simplificada
        )
    }
    
    private fun setupBotShips() {
        // Usar la flota de la configuración
        val shipsToPlace = config.getExpandedFleet()
        
        val placed = mutableListOf<ShipPlacement>()
        val occupied = mutableSetOf<Coordinate>()
        val random = java.util.Random()
        
        for (type in shipsToPlace) {
            var placedShip: ShipPlacement? = null
            while (placedShip == null) {
                val x = random.nextInt(boardSize)
                val y = random.nextInt(boardSize)
                val isHorizontal = random.nextBoolean()
                val orientation = if (isHorizontal) Orientation.HORIZONTAL else Orientation.VERTICAL
                val candidate = ShipPlacement(type, Coordinate(x, y), orientation)
                val coords = getShipCoordinates(candidate)
                
                var valid = true
                for (c in coords) {
                    if (c.x !in 0 until boardSize || c.y !in 0 until boardSize || occupied.contains(c)) {
                        valid = false
                        break
                    }
                }
                if (valid) {
                    placedShip = candidate
                    occupied.addAll(coords)
                    placed.add(placedShip)
                }
            }
        }
        player2Ships.addAll(placed)
    }
}

class GameSessionStats {
    var shotsFired = 0
    var hits = 0
    var shipsSunk = 0
    var turns = 0
}
