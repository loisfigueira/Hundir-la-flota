package com.lfigueira.hundir_la_flota.common.models

import com.lfigueira.hundir_la_flota.common.protocol.ShipType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameConfigTest {

    @Test
    fun testRoundsToWin() {
        assertEquals(1, GameConfig(maxRounds = 1).roundsToWin())
        assertEquals(2, GameConfig(maxRounds = 3).roundsToWin())
        assertEquals(3, GameConfig(maxRounds = 5).roundsToWin())
    }

    @Test
    fun testValidateValidConfig() {
        val config = GameConfig(
            boardSize = 10,
            turnTimeSeconds = 60,
            maxRounds = 3,
            shipsConfig = GameConfig.DEFAULT_FLEET
        )
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun testValidateInvalidBoardSize() {
        val configSmall = GameConfig(boardSize = 5)
        assertTrue(configSmall.validate().isFailure)

        val configLarge = GameConfig(boardSize = 20)
        assertTrue(configLarge.validate().isFailure)
    }

    @Test
    fun testGetExpandedFleet() {
        val config = GameConfig(
            shipsConfig = listOf(
                ShipDefinition(ShipType.BATTLESHIP, 1),
                ShipDefinition(ShipType.DESTROYER, 2)
            )
        )
        val expanded = config.getExpandedFleet()
        assertEquals(3, expanded.size)
        assertEquals(1, expanded.count { it == ShipType.BATTLESHIP })
        assertEquals(2, expanded.count { it == ShipType.DESTROYER })
    }
}
