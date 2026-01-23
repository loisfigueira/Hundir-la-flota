package com.lfigueira.hundir_la_flota

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hundir la flota",
    ) {
        App()
    }
}