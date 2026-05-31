package game

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import game.ui.MainAppContainer

class GameLauncher  {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = application {
            val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)

            Window(
                onCloseRequest = ::exitApplication,
                title = "Buckshot Roulette -- Admin Tool",
                state = windowState
            ) {
                MainAppContainer()
            }
        }
    }
}