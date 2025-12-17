package si.faks.besedadneva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import si.faks.besedadneva.data.db.DatabaseProvider
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.ui.theme.BesedaDnevaTheme
import si.faks.besedadneva.ui.viewmodel.HistoryViewModel
import si.faks.besedadneva.ui.viewmodel.HistoryViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Room + Repository + ViewModel factory
        val db = DatabaseProvider.get(applicationContext)
        val repo = GameRepository(db.gameDao(), db.guessDao())
        val factory = HistoryViewModelFactory(repo)

        // TEST: enkrat vpišemo eno igro, da vidiš, da se Room zapisuje
//        lifecycleScope.launch {
//            repo.insertGame(
//                GameEntity(
//                    date = "2025-12-17",
//                    mode = "DAILY",
//                    solution = "MIZA?",
//                    won = true,
//                    attemptsUsed = 3,
//                    finishedAtMillis = System.currentTimeMillis()
//                )
//            )
//        }

        setContent {
            BesedaDnevaTheme {
                val vm: HistoryViewModel = viewModel(factory = factory)
                val games by vm.games.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Text("Games: ${games.size}")
                        games.forEach { g ->
                            Text("${g.date} ${g.mode} won=${g.won} attempts=${g.attemptsUsed}")
                        }
                    }
                }
            }
        }
    }
}
