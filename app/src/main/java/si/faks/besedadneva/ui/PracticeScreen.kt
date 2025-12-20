package si.faks.besedadneva.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // <--- POMEMBEN IMPORT
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.fran.WordService
import si.faks.besedadneva.ui.viewmodel.GameMode
import si.faks.besedadneva.ui.viewmodel.GameViewModel
import si.faks.besedadneva.ui.viewmodel.GameViewModelFactory

@Composable
fun PracticeScreen(repo: GameRepository) {
    // SPREMEMBA: Uporabimo rememberSaveable namesto remember
    var requestId by rememberSaveable { mutableIntStateOf(0) }

    var picked by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(requestId) {
        picked = null
        error = null
        try {
            val word = withContext(Dispatchers.IO) {
                WordService().pickRandomNoun().word
            }
            picked = word
        } catch (t: Throwable) {
            error = t.message ?: "Napaka pri pridobivanju besede."
        }
    }

    when {
        error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Napaka: $error")
            }
        }
        picked == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            val factory = GameViewModelFactory(
                repo = repo,
                solution = picked!!,
                mode = GameMode.PRACTICE
            )

            val vm: GameViewModel = viewModel(
                key = "practice_$requestId",
                factory = factory
            )

            GameScreen(
                vm = vm,
                onEndOk = {
                    // Ko kliknemo OK ali Nova vaja, povečamo ID -> nov ViewModel -> nova beseda
                    requestId++
                }
            )
        }
    }
}