package com.lfigueira.hundir_la_flota.server.persistence

import com.lfigueira.hundir_la_flota.common.protocol.PlayerStats
import com.lfigueira.hundir_la_flota.common.protocol.LeaderboardEntry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

/**
 * Gestor de persistencia de records y estadísticas.
 * Utiliza un Mutex para garantizar escrituras thread-safe cuando múltiples
 * partidas terminan simultáneamente.
 */
class RecordsManager(private val recordsFilePath: String = "records.json") {
    
    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    @Serializable
    private data class RecordsData(
        val players: MutableMap<String, PlayerStats> = mutableMapOf()
    )
    
    private var recordsData: RecordsData = RecordsData()
    
    init {
        loadRecords()
    }
    
    /**
     * Carga los records desde el archivo JSON.
     * Si el archivo no existe, se crea uno nuevo.
     */
    private fun loadRecords() {
        val file = File(recordsFilePath)
        if (file.exists()) {
            try {
                val jsonContent = file.readText()
                recordsData = json.decodeFromString(jsonContent)
                println("[RecordsManager] Cargados ${recordsData.players.size} registros de jugadores")
            } catch (e: Exception) {
                println("[RecordsManager] Error al cargar records: ${e.message}")
                println("[RecordsManager] Creando nuevo archivo de records")
                recordsData = RecordsData()
            }
        } else {
            println("[RecordsManager] Archivo de records no encontrado, creando uno nuevo")
            saveRecords()
        }
    }
    
    /**
     * Guarda los records en el archivo JSON de forma segura (Atomic Write).
     * DEBE ser llamado dentro de un bloque mutex.withLock.
     */
    private fun saveRecords() {
        try {
            val file = File(recordsFilePath)
            val tempFile = File("$recordsFilePath.tmp")
            
            val jsonContent = json.encodeToString(recordsData)
            
            // 1. Escribir a archivo temporal
            tempFile.writeText(jsonContent)
            
            // 2. Renombrar atómicamente (si es posible) o mover
            if (file.exists()) {
                file.delete()
            }
            if (!tempFile.renameTo(file)) {
                // Fallback si renameTo falla (común en Windows si el archivo está bloqueado)
                // Intentar copia y borrado
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
            
            println("[RecordsManager] Records guardados exitosamente (Safe Save)")
        } catch (e: Exception) {
            println("[RecordsManager] Error al guardar records: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Obtiene las estadísticas de un jugador.
     * Thread-safe gracias al Mutex.
     */
    suspend fun getPlayerStats(playerName: String): PlayerStats = mutex.withLock {
        recordsData.players[playerName] ?: PlayerStats(
            playerName = playerName,
            gamesPlayed = 0,
            gamesWon = 0,
            gamesLost = 0,
            pvpWon = 0,
            pvpLost = 0,
            pveWon = 0,
            pveLost = 0,
            totalShots = 0,
            totalHits = 0,
            winStreak = 0,
            bestWinStreak = 0,
            averageAccuracy = 0.0,
            bestAccuracy = 0.0,
            fastestWinTurns = Int.MAX_VALUE,
            totalPlayTimeSeconds = 0
        )
    }

    /**
     * Asegura que existan estadísticas para un jugador y las devuelve.
     * Si no existen, las crea y guarda inmediatamente.
     */
    suspend fun ensurePlayerStats(playerName: String): PlayerStats = mutex.withLock {
        val existing = recordsData.players[playerName]
        if (existing != null) {
            existing
        } else {
             val newStats = PlayerStats(
                playerName = playerName,
                gamesPlayed = 0,
                gamesWon = 0,
                gamesLost = 0,
                pvpWon = 0,
                pvpLost = 0,
                pveWon = 0,
                pveLost = 0,
                totalShots = 0,
                totalHits = 0,
                winStreak = 0,
                bestWinStreak = 0,
                averageAccuracy = 0.0,
                bestAccuracy = 0.0,
                fastestWinTurns = Int.MAX_VALUE,
                totalPlayTimeSeconds = 0
            )
            recordsData.players[playerName] = newStats
            println("[RecordsManager] Nuevas estadísticas creadas para $playerName")
            saveRecords() // Save immediately on creation
            newStats
        }
    }
    
    /**
     * Actualiza las estadísticas de un jugador después de una partida.
     * Thread-safe: múltiples partidas pueden terminar simultáneamente sin corrupción.
     */
    suspend fun updatePlayerStats(
        playerName: String,
        won: Boolean,
        shots: Int,
        hits: Int,
        playTimeSeconds: Long,
        isPvP: Boolean,
        turns: Int = 0
    ) = mutex.withLock {
        val currentStats = recordsData.players[playerName] ?: PlayerStats(
            playerName = playerName,
            gamesPlayed = 0,
            gamesWon = 0,
            gamesLost = 0,
            pvpWon = 0,
            pvpLost = 0,
            pveWon = 0,
            pveLost = 0,
            totalShots = 0,
            totalHits = 0,
            winStreak = 0,
            bestWinStreak = 0,
            averageAccuracy = 0.0,
            bestAccuracy = 0.0,
            fastestWinTurns = Int.MAX_VALUE,
            totalPlayTimeSeconds = 0
        )
        
        val newGamesPlayed = currentStats.gamesPlayed + 1
        val newGamesWon = if (won) currentStats.gamesWon + 1 else currentStats.gamesWon
        val newGamesLost = if (!won) currentStats.gamesLost + 1 else currentStats.gamesLost
        val newTotalShots = currentStats.totalShots + shots
        val newTotalHits = currentStats.totalHits + hits
        val newWinStreak = if (won) currentStats.winStreak + 1 else 0
        val newBestWinStreak = maxOf(currentStats.bestWinStreak, newWinStreak)
        
        val currentAccuracy = if (shots > 0) (hits.toDouble() / shots.toDouble()) * 100.0 else 0.0
        val newBestAccuracy = maxOf(currentStats.bestAccuracy, currentAccuracy)
        
        val newAverageAccuracy = if (newTotalShots > 0) {
            (newTotalHits.toDouble() / newTotalShots.toDouble()) * 100.0
        } else 0.0
        val newTotalPlayTime = currentStats.totalPlayTimeSeconds + playTimeSeconds
        
        val newPvpWon = if (won && isPvP) currentStats.pvpWon + 1 else currentStats.pvpWon
        val newPvpLost = if (!won && isPvP) currentStats.pvpLost + 1 else currentStats.pvpLost
        val newPveWon = if (won && !isPvP) currentStats.pveWon + 1 else currentStats.pveWon
        val newPveLost = if (!won && !isPvP) currentStats.pveLost + 1 else currentStats.pveLost
        
        val newFastestWin = if (won && turns > 0) minOf(currentStats.fastestWinTurns, turns) else currentStats.fastestWinTurns
        
        val updatedStats = PlayerStats(
            playerName = playerName,
            gamesPlayed = newGamesPlayed,
            gamesWon = newGamesWon,
            gamesLost = newGamesLost,
            pvpWon = newPvpWon,
            pvpLost = newPvpLost,
            pveWon = newPveWon,
            pveLost = newPveLost,
            totalShots = newTotalShots,
            totalHits = newTotalHits,
            winStreak = newWinStreak,
            bestWinStreak = newBestWinStreak,
            averageAccuracy = newAverageAccuracy,
            bestAccuracy = newBestAccuracy,
            fastestWinTurns = newFastestWin,
            totalPlayTimeSeconds = newTotalPlayTime
        )
        
        recordsData.players[playerName] = updatedStats
        saveRecords()
        
        println("[RecordsManager] Estadísticas actualizadas para $playerName: " +
                "${newGamesWon}W/${newGamesLost}L, Racha: $newWinStreak")
    }
    
    /**
     * Obtiene el leaderboard ordenado por victorias.
     * Thread-safe.
     */
    suspend fun getLeaderboard(limit: Int = 10): List<LeaderboardEntry> = mutex.withLock {
        recordsData.players.values
            .sortedByDescending { it.gamesWon }
            .take(limit)
            .mapIndexed { index, stats ->
                val winRate = if (stats.gamesPlayed > 0) {
                    (stats.gamesWon.toDouble() / stats.gamesPlayed.toDouble()) * 100.0
                } else 0.0
                
                LeaderboardEntry(
                    rank = index + 1,
                    playerName = stats.playerName,
                    gamesWon = stats.gamesWon,
                    winRate = winRate,
                    bestStreak = stats.bestWinStreak
                )
            }
    }
    
    /**
     * Obtiene todos los jugadores registrados.
     */
    suspend fun getAllPlayers(): List<String> = mutex.withLock {
        recordsData.players.keys.toList()
    }
}
