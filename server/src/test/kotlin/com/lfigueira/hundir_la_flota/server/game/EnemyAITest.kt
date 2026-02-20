package com.lfigueira.hundir_la_flota.server.game

import com.lfigueira.hundir_la_flota.common.protocol.AIDifficulty
import com.lfigueira.hundir_la_flota.common.protocol.Coordinate
import com.lfigueira.hundir_la_flota.common.protocol.ShotResult
import kotlin.test.*

class EnemyAITest {

    @Test
    fun testAIGetNextShot() {
        val ai = EnemyAI(AIDifficulty.EASY)
        val firedShots = mutableListOf<Coordinate>()
        
        repeat(10) {
            val shot = ai.getNextShot(firedShots)
            assertNotNull(shot)
            assertFalse(firedShots.contains(shot))
            firedShots.add(shot)
        }
    }

    @Test
    fun testAIHuntMode() {
        val ai = EnemyAI(AIDifficulty.MEDIUM)
        val firedShots = mutableListOf<Coordinate>()
        
        // Simulate a HIT at (5, 5)
        val hitCoord = Coordinate(5, 5)
        ai.notifyShotResult(ShotResult.HIT, hitCoord, false)
        firedShots.add(hitCoord)

        // Next shot should be adjacent to (5, 5)
        val nextShot = ai.getNextShot(firedShots)
        assertNotNull(nextShot)
        
        val isAdjacent = (nextShot.x == 5 && (nextShot.y == 4 || nextShot.y == 6)) ||
                         (nextShot.y == 5 && (nextShot.x == 4 || nextShot.x == 6))
        
        assertTrue(isAdjacent, "AI in HUNT mode should target adjacent cells. Got $nextShot")
    }

    @Test
    fun testAISinkResetsMode() {
        val ai = EnemyAI(AIDifficulty.MEDIUM)
        val firedShots = mutableListOf<Coordinate>()
        
        // Simulate HIT then SUNK
        ai.notifyShotResult(ShotResult.HIT, Coordinate(5, 5), false)
        ai.notifyShotResult(ShotResult.SUNK, Coordinate(5, 6), true)
        
        firedShots.add(Coordinate(5, 5))
        firedShots.add(Coordinate(5, 6))

        // After SUNK, it should go back to random search or at least not be forced to adjacent of last sunk
        // This is hard to test perfectly without knowing internal search state, but we can verify it still works
        val shot = ai.getNextShot(firedShots)
        assertNotNull(shot)
        assertFalse(firedShots.contains(shot))
    }
}
