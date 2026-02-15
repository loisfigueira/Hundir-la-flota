package com.lfigueira.hundir_la_flota.common.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket
import java.net.InetSocketAddress

actual class NetworkClient {
    private var socket: Socket? = null
    private var reader: BufferedReader? = null // back to reader
    private var writer: PrintWriter? = null
    
    private val _isConnected = MutableStateFlow(false)
    actual val isConnected: Flow<Boolean> = _isConnected
    
    actual suspend fun connect(host: String, port: Int) {
        withContext(Dispatchers.IO) {
            println("[NetworkClient] Intentando conectar a $host:$port...")
            try {
                val newSocket = Socket()
                newSocket.keepAlive = true
                newSocket.tcpNoDelay = true
                newSocket.connect(InetSocketAddress(host, port), 10000)
                println("[NetworkClient] Socket conectado.")
                
                socket = newSocket
                reader = newSocket.getInputStream().bufferedReader()
                writer = PrintWriter(newSocket.getOutputStream(), true) // autoFlush = true
                _isConnected.value = true
                
            } catch (e: Exception) {
                println("[NetworkClient] Error CRÍTICO al conectar: ${e.message}")
                throw e
            }
        }
    }
    
    actual suspend fun send(message: String) {
        withContext(Dispatchers.IO) {
            val currentWriter = writer ?: throw IllegalStateException("Socket not connected")
            currentWriter.println(message)
            if (currentWriter.checkError()) {
                println("[NetworkClient] Warning: Error reportado por PrintWriter")
            }
        }
    }
    
    actual fun receive(): Flow<String> = flow {
        val currentReader = reader ?: throw IllegalStateException("Socket not connected")
        try {
            while (true) {
                val line = currentReader.readLine() ?: break // EOF
                emit(line)
            }
        } catch (e: Exception) {
            // Error en lectura o socket cerrado
        } finally {
            _isConnected.value = false
        }
    }.flowOn(Dispatchers.IO)
    
    actual suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                socket?.close()
            } catch (e: Exception) {
                // Ignorar errores al cerrar
            } finally {
                socket = null
                reader = null
                writer = null
                _isConnected.value = false
            }
        }
    }
}
