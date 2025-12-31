package si.faks.besedadneva.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.data.fran.WordService
import si.faks.besedadneva.ui.viewmodel.*

@Composable
fun DailyMenuScreen(
    repo: GameRepository,
    // Uporabimo nov ViewModel
    menuViewModel: DailyMenuViewModel = viewModel(factory = DailyMenuViewModelFactory(repo))
) {
    // 1. Avtomatsko poslušanje stanja (to reši problem osveževanja!)
    val playedStatus by menuViewModel.playedStatus.collectAsState()
    val todayDate = menuViewModel.todayDate

    // Stanje za navigacijo v igro
    var selectedLength by remember { mutableStateOf<Int?>(null) }
    var pickedWord by remember { mutableStateOf<String?>(null) }
    var gameRowId by remember { mutableStateOf<Long?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun startGame(length: Int, existingGame: GameEntity?) {
        isLoading = true
        selectedLength = length
        scope.launch {
            try {
                if (existingGame != null) {
                    pickedWord = existingGame.solution
                    // Rešimo lokalni ID (za stabilnost)
                    val realLocalId = withContext(Dispatchers.IO) {
                        repo.resolveLocalGameId(existingGame)
                    }
                    gameRowId = realLocalId
                } else {
                    val word = withContext(Dispatchers.IO) {
                        WordService().pickDailyNoun(todayDate, length).word
                    }
                    pickedWord = word

                    val newGame = GameEntity(
                        date = todayDate,
                        mode = "DAILY_$length",
                        solution = word,
                        won = false,
                        attemptsUsed = 0,
                        finishedAtMillis = System.currentTimeMillis()
                    )
                    val id = withContext(Dispatchers.IO) { repo.startDailyGame(newGame) }
                    gameRowId = id
                }
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
                selectedLength = null
            }
        }
    }

    if (selectedLength != null && pickedWord != null && gameRowId != null) {
        val factory = GameViewModelFactory(
            repo = repo,
            solution = pickedWord!!,
            mode = GameMode.DAILY,
            existingGameId = gameRowId
        )
        // Key zagotovi, da se VM resetira ob menjavi igre
        val vm: GameViewModel = viewModel(key = "daily_${todayDate}_$selectedLength", factory = factory)

        GameScreen(
            vm = vm,
            onEndOk = {
                selectedLength = null
                pickedWord = null
                gameRowId = null
            }
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Dnevni izziv", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
            Text(todayDate, fontSize = 16.sp, color = Color.Gray)

            Spacer(Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                // Status se zdaj bere direktno iz "playedStatus", ki ga ViewModel drži svežega
                DailyButton(length = 4, game = playedStatus[4]) { startGame(4, playedStatus[4]) }
                Spacer(Modifier.height(16.dp))
                DailyButton(length = 5, game = playedStatus[5]) { startGame(5, playedStatus[5]) }
                Spacer(Modifier.height(16.dp))
                DailyButton(length = 6, game = playedStatus[6]) { startGame(6, playedStatus[6]) }
            }

            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text("Napaka: $error", color = Color.Red)
            }
        }
    }
}

// DailyButton ostane enak kot prej...
@Composable
fun DailyButton(length: Int, game: GameEntity?, onClick: () -> Unit) {
    val maxAttempts = length + 1

    val isPlayed = game != null
    val isWon = game?.won == true
    val isLost = isPlayed && !isWon && (game!!.attemptsUsed >= maxAttempts)
    val isInProgress = isPlayed && !isWon && !isLost

    val btnColor = when {
        isWon -> Color(0xFF66BB6A)
        isLost -> Color(0xFFEF5350)
        isInProgress -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primary
    }

    val isEnabled = !isWon && !isLost

    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = btnColor,
            disabledContainerColor = btnColor.copy(alpha = 0.7f),
            disabledContentColor = Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val suffix = if (length == 4) "črke" else "črk"
            Text("$length $suffix", fontSize = 18.sp)

            when {
                isWon -> Text("REŠENO ✓", fontSize = 18.sp)
                isLost -> Text("X", fontSize = 18.sp)
                isInProgress -> Text("NADALJUJ", fontSize = 18.sp)
                else -> Text("IGRAJ", fontSize = 18.sp)
            }
        }
    }
}