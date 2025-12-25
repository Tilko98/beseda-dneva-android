package si.faks.besedadneva.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var requestId by rememberSaveable { mutableIntStateOf(0) }
    var selectedLength by rememberSaveable { mutableIntStateOf(5) }

    var picked by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(requestId, selectedLength) {
        // Tole se zgodi, ampak včasih z malim zamikom.
        // Zato picked=null nastavimo tudi v onClick gumba (spodaj).
        picked = null
        error = null
        try {
            val word = withContext(Dispatchers.IO) {
                WordService().pickRandomNoun(length = selectedLength).word
            }
            picked = word
        } catch (t: Throwable) {
            error = t.message ?: "Napaka pri pridobivanju besede."
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Text(
            "Izberi težavnost:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top=8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // TU JE GLAVNI TRIK ZA HITROST:
            // Ob kliku takoj nastavimo picked = null, da se pokaže loader
            DifficultyButton(4, "4 (Lahko)", selectedLength) {
                selectedLength = 4
                picked = null
                requestId++
            }
            DifficultyButton(5, "5 (Normal)", selectedLength) {
                selectedLength = 5
                picked = null
                requestId++
            }
            DifficultyButton(6, "6 (Težko)", selectedLength) {
                selectedLength = 6
                picked = null
                requestId++
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Napaka: $error")
                            Button(onClick = { requestId++ }) { Text("Poskusi znova") }
                        }
                    }
                }
                picked == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    val solution = picked!!
                    val factory = GameViewModelFactory(
                        repo = repo,
                        solution = solution,
                        mode = GameMode.PRACTICE
                    )

                    val vm: GameViewModel = viewModel(
                        key = "practice_${selectedLength}_$requestId",
                        factory = factory
                    )

                    GameScreen(
                        vm = vm,
                        onEndOk = { requestId++ }
                    )
                }
            }
        }
    }
}

@Composable
fun DifficultyButton(len: Int, label: String, current: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (current == len) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (current == len) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(label)
    }
}