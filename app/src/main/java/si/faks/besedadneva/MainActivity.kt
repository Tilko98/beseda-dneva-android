package si.faks.besedadneva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import si.faks.besedadneva.data.db.DatabaseProvider
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.ui.GameScreen
import si.faks.besedadneva.ui.theme.BesedaDnevaTheme
import si.faks.besedadneva.ui.viewmodel.GameMode
import si.faks.besedadneva.ui.viewmodel.GameViewModel
import si.faks.besedadneva.ui.viewmodel.GameViewModelFactory
import androidx.compose.foundation.layout.padding


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Room + Repository
        val db = DatabaseProvider.get(applicationContext)
        val repo = GameRepository(db.gameDao(), db.guessDao())

        setContent {
            BesedaDnevaTheme {
                // GameViewModel (za test ima solution hardcoded)
                val gameFactory = GameViewModelFactory(
                    repo = repo,
                    solution = "LOPAR",
                    mode = GameMode.DAILY
                )
                val gameVm: GameViewModel = viewModel(factory = gameFactory)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        vm = gameVm,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

            }
        }
    }
}
