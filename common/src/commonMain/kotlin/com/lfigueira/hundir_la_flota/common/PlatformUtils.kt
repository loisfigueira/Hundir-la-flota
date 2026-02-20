package com.lfigueira.hundir_la_flota.common

/**
 * Utilidades dependientes de la plataforma.
 */
expect object PlatformUtils {
    /**
     * Obtiene el tiempo actual en milisegundos.
     */
    fun getCurrentTimeMillis(): Long
}
