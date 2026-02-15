package com.lfigueira.hundir_la_flota.common.models

import com.lfigueira.hundir_la_flota.common.protocol.ShipType
import kotlinx.serialization.Serializable

/**
 * Configuración dinámica del juego.
 * 
 * Esta clase define todos los parámetros configurables de una partida,
 * permitiendo personalizar el tamaño del tablero, el tiempo de turno,
 * el número de rondas, y la composición de la flota.
 */
@Serializable
data class GameConfig(
    /**
     * Tamaño del tablero (NxN).
     * Valores típicos: 10 (clásico), 12, 15 (máximo)
     */
    val boardSize: Int = 10,
    
    /**
     * Tiempo máximo por turno en segundos.
     * Rango recomendado: 30-120 segundos
     */
    val turnTimeSeconds: Int = 60,
    
    /**
     * Número máximo de rondas a jugar.
     * - 1: Partida única
     * - 3: Mejor de 3 (primero en ganar 2)
     * - 5: Mejor de 5 (primero en ganar 3)
     */
    val maxRounds: Int = 1,
    
    /**
     * Configuración de la flota.
     * Define qué barcos y cuántos de cada tipo se deben colocar.
     * 
     * Configuración por defecto (nueva):
     * - 1x Portaaviones (5 casillas)
     * - 2x Acorazado (4 casillas)
     * - 3x Crucero (3 casillas)
     * - 4x Destructor (2 casillas)
     */
    val shipsConfig: List<ShipDefinition> = DEFAULT_FLEET
) {
    companion object {
        /**
         * Flota por defecto: 1x5, 2x4, 3x3, 4x2
         */
        val DEFAULT_FLEET = listOf(
            ShipDefinition(ShipType.CARRIER, 1),
            ShipDefinition(ShipType.BATTLESHIP, 2),
            ShipDefinition(ShipType.CRUISER, 3),
            ShipDefinition(ShipType.DESTROYER, 4)
        )
        
        /**
         * Flota clásica antigua: 1x4, 2x3, 3x2, 4x1
         */
        val CLASSIC_FLEET = listOf(
            ShipDefinition(ShipType.BATTLESHIP, 1),
            ShipDefinition(ShipType.CRUISER, 2),
            ShipDefinition(ShipType.DESTROYER, 3),
            ShipDefinition(ShipType.PATROL_BOAT, 4)
        )
    }
    
    /**
     * Calcula el número de rondas necesarias para ganar.
     * Ejemplo: Best of 3 requiere 2 victorias, Best of 5 requiere 3.
     */
    fun roundsToWin(): Int = (maxRounds / 2) + 1
    
    /**
     * Valida que la configuración sea coherente.
     */
    fun validate(): Result<Unit> {
        if (boardSize < 8 || boardSize > 15) {
            return Result.failure(IllegalArgumentException("boardSize debe estar entre 8 y 15"))
        }
        
        if (turnTimeSeconds < 10 || turnTimeSeconds > 300) {
            return Result.failure(IllegalArgumentException("turnTimeSeconds debe estar entre 10 y 300"))
        }
        
        if (maxRounds !in listOf(1, 3, 5)) {
            return Result.failure(IllegalArgumentException("maxRounds debe ser 1, 3 o 5"))
        }
        
        if (shipsConfig.isEmpty()) {
            return Result.failure(IllegalArgumentException("shipsConfig no puede estar vacía"))
        }
        
        // Validar que todos los barcos caben en el tablero
        val largestShip = shipsConfig.maxOfOrNull { it.type.size } ?: 0
        if (largestShip > boardSize) {
            return Result.failure(IllegalArgumentException("El barco más grande ($largestShip) no cabe en el tablero ($boardSize x $boardSize)"))
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Obtiene la lista completa de barcos a colocar (expandida).
     * Ejemplo: ShipDefinition(BATTLESHIP, 2) -> [BATTLESHIP, BATTLESHIP]
     */
    fun getExpandedFleet(): List<ShipType> {
        return shipsConfig.flatMap { definition ->
            List(definition.count) { definition.type }
        }
    }
}

/**
 * Define un tipo de barco y cuántos de ese tipo deben colocarse.
 */
@Serializable
data class ShipDefinition(
    val type: ShipType,
    val count: Int
)
