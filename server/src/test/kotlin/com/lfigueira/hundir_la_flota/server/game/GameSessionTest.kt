package com.lfigueira.hundir_la_flota.server.game

import com.lfigueira.hundir_la_flota.common.models.GameConfig
import com.lfigueira.hundir_la_flota.common.protocol.*
import com.lfigueira.hundir_la_flota.server.IClientHandler
import com.lfigueira.hundir_la_flota.server.persistence.RecordsManager
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class MockClientHandler(
    override val clientId: String,
    override val playerName: String
) : IClientHandler {
    val sentMessages = mutableListOf<GameMessage>()
    var disconnected = false

    override suspend fun sendMessage(message: GameMessage) {
        sentMessages.add(message)
    }

    override fun disconnect() {
        disconnected = true
    }
}

class GameSessionTest {

    private lateinit var recordsManager: RecordsManager
    private val recordsFile = "test_records.json"

    @BeforeTest
    fun setup() {
        File(recordsFile).delete()
        recordsManager = RecordsManager(recordsFile)
    }

    @AfterTest
    fun tearDown() {
        File(recordsFile).delete()
    }

    @Test
    fun testShipPlacementValidation() = runBlocking {
        val p1 = MockClientHandler("p1", "Player 1")
        val p2 = MockClientHandler("p2", "Player 2")
        val session = GameSession(
            "test_game", p1, p2, 
            recordsManager = recordsManager, 
            onFinished = {}
        )

        // Valid fleet: 1x4, 2x3, 3x2, 4x1 (CLASSIC_FLEET as example or DEFAULT)
        // DEFAULT_FLEET: 1x5, 2x4, 3x3, 4x2
        val validShips = listOf(
            ShipPlacement(ShipType.CARRIER, Coordinate(0, 0), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.BATTLESHIP, Coordinate(0, 1), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.BATTLESHIP, Coordinate(0, 2), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.CRUISER, Coordinate(0, 3), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.CRUISER, Coordinate(0, 4), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.CRUISER, Coordinate(0, 5), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.DESTROYER, Coordinate(0, 6), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.DESTROYER, Coordinate(0, 7), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.DESTROYER, Coordinate(0, 8), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.DESTROYER, Coordinate(0, 9), Orientation.HORIZONTAL)
        )

        session.handleAction(p1, GameMessage.Action.PlaceShips(validShips))
        
        val lastMsg = p1.sentMessages.lastOrNull()
        assertTrue(lastMsg is GameMessage.Response.PlacementConfirmed, "Placement should be confirmed")
    }

    @Test
    fun testOverlappingShipPlacement() = runBlocking {
        val p1 = MockClientHandler("p1", "Player 1")
        val p2 = MockClientHandler("p2", "Player 2")
        val session = GameSession(
            "test_game", p1, p2, 
            recordsManager = recordsManager, 
            onFinished = {}
        )

        // Overlapping ships
        val invalidShips = listOf(
            ShipPlacement(ShipType.CARRIER, Coordinate(0, 0), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.BATTLESHIP, Coordinate(0, 0), Orientation.HORIZONTAL), // Overlap
            ShipPlacement(ShipType.BATTLESHIP, Coordinate(0, 2), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.CRUISER, Coordinate(0, 3), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.CRUISER, Coordinate(0, 4), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.CRUISER, Coordinate(0, 5), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.DESTROYER, Coordinate(0, 6), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.DESTROYER, Coordinate(0, 7), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.DESTROYER, Coordinate(0, 8), Orientation.HORIZONTAL),
            ShipPlacement(ShipType.DESTROYER, Coordinate(0, 9), Orientation.HORIZONTAL)
        )

        session.handleAction(p1, GameMessage.Action.PlaceShips(invalidShips))
        
        val lastMsg = p1.sentMessages.lastOrNull()
        assertTrue(lastMsg is GameMessage.Error.InvalidAction, "Should return invalid action error for overlapping ships")
    }

    @Test
    fun testAttackResolution() = runBlocking {
        val p1 = MockClientHandler("p1", "Player 1")
        val p2 = MockClientHandler("p2", "Player 2")
        val config = GameConfig(boardSize = 10)
        val session = GameSession(
            "test_game", p1, p2, 
            config = config,
            recordsManager = recordsManager, 
            onFinished = {}
        )

        val ships1 = generateValidFleet(config)
        val ships2 = generateValidFleet(config)

        session.handleAction(p1, GameMessage.Action.PlaceShips(ships1))
        session.handleAction(p2, GameMessage.Action.PlaceShips(ships2))

        // Phase should be BATTLE now
        // P1 attacks P2 at (0, 0)
        // In generateValidFleet, (0, 0) is occupied by CARRIER (first ship)
        session.handleAction(p1, GameMessage.Action.Attack(Coordinate(0, 0)))

        val p1AttackRes = p1.sentMessages.filterIsInstance<GameMessage.Response.AttackResult>().lastOrNull()
        assertNotNull(p1AttackRes)
        assertEquals(ShotResult.HIT, p1AttackRes.result)
        
        // P1 attacks again (invalid, not their turn)
        session.handleAction(p1, GameMessage.Action.Attack(Coordinate(1, 0)))
        val errorMsg = p1.sentMessages.lastOrNull()
        assertTrue(errorMsg is GameMessage.Error.InvalidAction, "Should return error for attacking out of turn")
    }

    private fun generateValidFleet(config: GameConfig): List<ShipPlacement> {
        val fleet = config.shipsConfig
        val placements = mutableListOf<ShipPlacement>()
        var y = 0
        for (def in fleet) {
            repeat(def.count) {
                placements.add(ShipPlacement(def.type, Coordinate(0, y), Orientation.HORIZONTAL))
                y++
            }
        }
        return placements
    }
}
