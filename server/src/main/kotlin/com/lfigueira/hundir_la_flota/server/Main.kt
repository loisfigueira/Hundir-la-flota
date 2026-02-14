package com.lfigueira.hundir_la_flota.server

import com.lfigueira.hundir_la_flota.common.AppLogger
import kotlinx.coroutines.runBlocking

/**
 * Punto de entrada del servidor.
 */
fun main() = runBlocking {
    // Inicializar Logger
    AppLogger.init("server.log")
    
    val server = GameServer()
    
    // Agregar shutdown hook para detener el servidor correctamente
    Runtime.getRuntime().addShutdownHook(Thread {
        AppLogger.info("Main", "Señal de apagado recibida")
        server.stop()
    })
    
    server.start()
    
    // Mantener el servidor corriendo
    AppLogger.info("Main", "Servidor en ejecución. Presiona Ctrl+C para detener.")
    
    // Loop para comandos del servidor
    while (true) {
        print("> ")
        when (readlnOrNull()?.trim()?.lowercase()) {
            "stats" -> AppLogger.info("Main", "Stats request:\n${server.getStats()}")
            "stop", "exit", "quit" -> {
                server.stop()
                break
            }
            "help" -> {
                println("""
                    Comandos disponibles:
                      stats - Muestra estadísticas del servidor
                      stop  - Detiene el servidor
                      help  - Muestra esta ayuda
                """.trimIndent())
            }
            null -> break
            else -> println("Comando desconocido. Escribe 'help' para ver los comandos disponibles.")
        }
    }
}
