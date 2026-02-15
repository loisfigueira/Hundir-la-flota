package com.lfigueira.hundir_la_flota.common

actual object PlatformUtils {
    actual fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
