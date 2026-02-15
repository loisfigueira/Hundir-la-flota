package com.lfigueira.hundir_la_flota

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lfigueira.hundir_la_flota.common.AppLogger

fun main() = application {
    AppLogger.init("client.log")
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hundir la flota",
    ) {
        App()
    }
}