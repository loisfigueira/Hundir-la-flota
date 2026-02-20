package com.lfigueira.hundir_la_flota.ui

import com.lfigueira.hundir_la_flota.common.client.ConnectionState
import com.lfigueira.hundir_la_flota.common.client.IGameClient
import com.lfigueira.hundir_la_flota.common.models.GameConfig
import com.lfigueira.hundir_la_flota.common.protocol.GameMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.*

class MockGameClient : IGameClient {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<GameMessage>()
    override val incomingMessages: SharedFlow<GameMessage> = _incomingMessages.asSharedFlow()

    val sentMessages = mutableListOf<GameMessage>()

    override suspend fun connectAndInitialize(host: String, port: Int, playerName: String): Boolean {
        _connectionState.value = ConnectionState.Connected
        return true
    }

    override suspend fun send(message: GameMessage) {
        sentMessages.add(message)
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
    }

    suspend fun emitMessage(message: GameMessage) {
        _incomingMessages.emit(message)
    }
}

class GameViewModelTest {

    @Test
    fun testInitialState() {
        val mockClient = MockGameClient()
        val viewModel = GameViewModel(mockClient)
        assertEquals(UIState.MainMenu, viewModel.uiState.value)
    }

    @Test
    fun testNavigationToSettings() {
        val mockClient = MockGameClient()
        val viewModel = GameViewModel(mockClient)
        viewModel.navigateToSettings()
        assertEquals(UIState.Settings, viewModel.uiState.value)
    }

    @Test
    fun testMatchFoundNavigation() = runBlocking {
        val mockClient = MockGameClient()
        val viewModel = GameViewModel(mockClient)
        
        val config = GameConfig(boardSize = 10)
        mockClient.emitMessage(GameMessage.Matchmaking.MatchFound("game1", "Opponent", false, config))
        delay(200)
        
        assertTrue(viewModel.uiState.value is UIState.ShipPlacement)
        val state = viewModel.uiState.value as UIState.ShipPlacement
        assertEquals("Opponent", state.opponentName)
    }

    @Test
    fun testAttackAction() = runBlocking {
        val mockClient = MockGameClient()
        val viewModel = GameViewModel(mockClient)
        delay(200) // Esperar a que el colector de mensajes en el init del ViewModel se active
        
        // Simular conexión y bienvenida para tener myPlayerId
        mockClient.emitMessage(GameMessage.Connection.Welcome("p1", "1.0.0", "Welcome"))
        delay(200)
        
        // Simular inicio de partida
        mockClient.emitMessage(GameMessage.Matchmaking.MatchFound("game1", "Opponent", false, GameConfig()))
        delay(200)
        
        val emptyBoard = com.lfigueira.hundir_la_flota.common.protocol.BoardState(
            size = 10,
            cells = List(10) { List(10) { com.lfigueira.hundir_la_flota.common.protocol.CellState.EMPTY } }
        )

        // Simular mensaje de GameState indicando que es mi turno
        mockClient.emitMessage(GameMessage.Response.GameState(
            phase = com.lfigueira.hundir_la_flota.common.protocol.GamePhase.BATTLE,
            currentTurn = "p1",
            turnTimeSeconds = 30,
            myBoard = emptyBoard,
            opponentBoard = emptyBoard,
            status = "Tu Turno"
        ))
        delay(200)

        viewModel.sendAttack(com.lfigueira.hundir_la_flota.common.protocol.Coordinate(5, 5))
        delay(200)
        
        val lastSent = mockClient.sentMessages.lastOrNull()
        assertTrue(lastSent is GameMessage.Action.Attack, "Should have sent an Attack message but got $lastSent")
        val attack = lastSent as GameMessage.Action.Attack
        assertEquals(5, attack.coordinate.x)
        assertEquals(5, attack.coordinate.y)
    }
}
