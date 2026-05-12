import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*

class GameLauncher
{
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = application {
            val players = remember {
                listOf(
                    Player(name = "Bastard", initialHealth = 4),
                    Player(name = "Freak", initialHealth = 4),
                    Player(name = "Clown", initialHealth = 4),
                    Player(name = "God Maksim", initialHealth = 4),
                )
            }

            val session = remember { GameSession(players) }

            val viewModel = remember { ViewModel(session, players) }

            LaunchedEffect(Unit) {
                session.startRound(live = 2, blank = 2)
            }

            Window(
                onCloseRequest = ::exitApplication,
                title = "Buckshot Roulette -- Admin Tool",
                state = rememberWindowState(width = 1200.dp, height = 800.dp)
            ) {
                TableScreen(viewModel = viewModel)
            }
        }
    }
}