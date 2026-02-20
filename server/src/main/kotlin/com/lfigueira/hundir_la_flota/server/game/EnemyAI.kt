package com.lfigueira.hundir_la_flota.server.game

import com.lfigueira.hundir_la_flota.common.protocol.Coordinate
import com.lfigueira.hundir_la_flota.common.protocol.ShotResult
import com.lfigueira.hundir_la_flota.common.protocol.AIDifficulty
import java.util.*

/**
 * Inteligencia Artificial para Hundir la Flota.
 * Soporta dificultades EASY, MEDIUM y HARD.
 */
class EnemyAI(val difficulty: AIDifficulty = AIDifficulty.MEDIUM) {
    
    private val random = Random()
    private val pendingTargets = Stack<Coordinate>() 
    
    /**
     * Calcula la siguiente coordenada a la que disparará la IA.
     * @param alreadyFired Lista de coordenadas a las que ya se ha disparado en este tablero.
     * @return La coordenada elegida para el siguiente ataque.
     */
    fun getNextShot(alreadyFired: List<Coordinate>): Coordinate {
        // En dificultad EASY, ignoramos la caza de adyacentes y disparamos al azar
        if (difficulty == AIDifficulty.EASY) {
            return generateRandomShot(alreadyFired)
        }

        var target: Coordinate? = null
        
        while (!pendingTargets.isEmpty()) {
            val candidate = pendingTargets.pop()
            if (!alreadyFired.contains(candidate)) {
                target = candidate
                break
            }
        }
        
        if (target == null) {
            target = generateSmartShot(alreadyFired)
        }
        
        return target
    }

    private fun generateRandomShot(alreadyFired: List<Coordinate>): Coordinate {
        var target: Coordinate
        do {
            val x = random.nextInt(10)
            val y = random.nextInt(10)
            target = Coordinate(x, y)
        } while (alreadyFired.contains(target))
        return target
    }

    private fun generateSmartShot(alreadyFired: List<Coordinate>): Coordinate {
        // HARD intenta disparar en patrón de damero (parity)
        if (difficulty == AIDifficulty.HARD) {
            val parityCells = mutableListOf<Coordinate>()
            for (x in 0 until 10) {
                for (y in 0 until 10) {
                    if ((x + y) % 2 == 0 && !alreadyFired.contains(Coordinate(x, y))) {
                        parityCells.add(Coordinate(x, y))
                    }
                }
            }
            if (parityCells.isNotEmpty()) return parityCells.random()
        }
        
        return generateRandomShot(alreadyFired)
    }
    
    /**
     * Informa a la IA del resultado del último disparo para que pueda aprender/ajustar su estrategia.
     * @param result Resultado del disparo (AGUA, TOCADO, HUNDIDO).
     * @param coordinate Coordenada del disparo.
     * @param isSunk Indica si el disparo ha terminado de hundir un barco.
     */
    fun notifyShotResult(result: ShotResult, coordinate: Coordinate, isSunk: Boolean) {
        if (difficulty == AIDifficulty.EASY) return // No aprende en fácil

        if (result == ShotResult.HIT && !isSunk) {
            addAdjacents(coordinate)
        } else if (result == ShotResult.SUNK) {
            // En MEDIUM y HARD, vaciamos los objetivos adyacentes al hundir
            // (Para no perder tiros alrededor de un barco ya muerto)
            pendingTargets.clear() 
        }
    }
    
    private fun addAdjacents(center: Coordinate) {
        val targets = mutableListOf<Coordinate>()
        if (center.y > 0) targets.add(Coordinate(center.x, center.y - 1))
        if (center.y < 9) targets.add(Coordinate(center.x, center.y + 1))
        if (center.x > 0) targets.add(Coordinate(center.x - 1, center.y))
        if (center.x < 9) targets.add(Coordinate(center.x + 1, center.y))
        
        targets.shuffle()
        targets.forEach { pendingTargets.push(it) }
    }
}
