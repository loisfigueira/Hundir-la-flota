package com.lfigueira.hundir_la_flota.common

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Utilidad de log personalizada para Hundir la Flota.
 * Soporta salida a consola y a archivo.
 */
object AppLogger {
    private var logFile: File? = null
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Inicializa el logger con un archivo específico.
     * Trunca el archivo si ya existe para empezar limpio en cada ejecución.
     */
    fun init(fileName: String) {
        val file = File(fileName)
        try {
            // Truncar el archivo al inicio
            FileWriter(file, false).use { it.write("") }
            logFile = file
            info("AppLogger", "Logger inicializado (reseteado) en archivo: ${file.absolutePath}")
        } catch (e: Exception) {
            System.err.println("Error inicializando archivo de log: ${e.message}")
        }
    }

    fun info(tag: String, message: String) = log("INFO", tag, message)
    fun debug(tag: String, message: String) = log("DEBUG", tag, message)
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message)
        throwable?.printStackTrace()
        logFile?.let { file ->
            try {
                FileWriter(file, true).use { fw ->
                    PrintWriter(fw).use { out ->
                        throwable?.printStackTrace(out)
                    }
                }
            } catch (e: Exception) {
                System.err.println("Error escribiendo stacktrace en log: ${e.message}")
            }
        }
    }

    private fun log(level: String, tag: String, message: String) {
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val formattedMessage = "[$timestamp] [$level] [$tag] $message"
        
        // Salida a consola
        if (level == "ERROR") {
            System.err.println(formattedMessage)
        } else {
            println(formattedMessage)
        }

        // Salida a archivo
        logFile?.let { file ->
            try {
                FileWriter(file, true).use { fw ->
                    PrintWriter(fw).use { out ->
                        out.println(formattedMessage)
                    }
                }
            } catch (e: Exception) {
                System.err.println("Error escribiendo en el archivo de log: ${e.message}")
            }
        }
    }
}
