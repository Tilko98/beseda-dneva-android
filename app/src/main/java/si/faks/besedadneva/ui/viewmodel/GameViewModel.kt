package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.data.db.entities.GuessEntity
import si.faks.besedadneva.wordle.evaluateGuessPattern
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun todayIsoDate(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(Date())
}

enum class GameMode { DAILY, PRACTICE }

data class GuessRowUi(
    val letters: String,   // dolžine 5 (lahko vsebuje ' ')
    val pattern: String?   // npr. "GYXGX" ali null dokler ni oddano
)

data class GameUiState(
    val date: String,
    val mode: GameMode,
    val solution: String,             // za zdaj je tu (kasneje lahko skriješ)
    val rows: List<GuessRowUi>,       // 6 vrstic
    val currentRowIndex: Int,
    val currentColIndex: Int,
    val isFinished: Boolean,
    val isWin: Boolean,
    val message: String? = null
)

class GameViewModel(
    private val repo: GameRepository,
    solution: String,
    mode: GameMode = GameMode.DAILY,
    date: String = todayIsoDate()
) : ViewModel() {

    private val normalizedSolution = solution.trim().uppercase()

    private val _state = MutableStateFlow(
        GameUiState(
            date = date,
            mode = mode,
            solution = normalizedSolution,
            rows = List(6) { GuessRowUi(letters = "     ", pattern = null) },
            currentRowIndex = 0,
            currentColIndex = 0,
            isFinished = false,
            isWin = false,
            message = null
        )
    )
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    fun onLetter(ch: Char) {
        val s = _state.value
        if (s.isFinished) return
        if (s.currentRowIndex !in 0..5) return
        if (s.currentColIndex !in 0..4) return

        val upper = ch.uppercaseChar()

        val row = s.rows[s.currentRowIndex]
        val updatedLetters = row.letters.toCharArray().apply {
            this[s.currentColIndex] = upper
        }.concatToString()

        val updatedRows = s.rows.toMutableList().apply {
            this[s.currentRowIndex] = row.copy(letters = updatedLetters)
        }

        _state.value = s.copy(
            rows = updatedRows,
            currentColIndex = s.currentColIndex + 1,
            message = null
        )
    }

    fun onBackspace() {
        val s = _state.value
        if (s.isFinished) return
        if (s.currentRowIndex !in 0..5) return
        if (s.currentColIndex <= 0) return

        val newCol = s.currentColIndex - 1
        val row = s.rows[s.currentRowIndex]
        val updatedLetters = row.letters.toCharArray().apply {
            this[newCol] = ' '
        }.concatToString()

        val updatedRows = s.rows.toMutableList().apply {
            this[s.currentRowIndex] = row.copy(letters = updatedLetters)
        }

        _state.value = s.copy(
            rows = updatedRows,
            currentColIndex = newCol,
            message = null
        )
    }

    fun onEnter() {
        val s = _state.value
        if (s.isFinished) return
        if (s.currentRowIndex !in 0..5) return

        val row = s.rows[s.currentRowIndex]
        val guess = row.letters.replace(" ", "")

        if (guess.length != 5) {
            _state.value = s.copy(message = "Vnesi 5 črk.")
            return
        }

        val pattern = evaluateGuessPattern(guess, s.solution)

        val updatedRows = s.rows.toMutableList().apply {
            this[s.currentRowIndex] = row.copy(pattern = pattern)
        }

        val win = pattern.all { it == 'G' }
        val lastRow = s.currentRowIndex == 5
        val finished = win || lastRow

        val nextState = s.copy(
            rows = updatedRows,
            currentRowIndex = if (!finished) s.currentRowIndex + 1 else s.currentRowIndex,
            currentColIndex = if (!finished) 0 else s.currentColIndex,
            isFinished = finished,
            isWin = win,
            message = if (finished) (if (win) "Bravo! 🎉" else "Konec igre.") else null
        )

        _state.value = nextState

        if (finished) saveGameToDb(nextState)
    }


    private fun updateRow(index: Int, newRow: GuessRowUi) {
        val s = _state.value
        val updated = s.rows.toMutableList()
        updated[index] = newRow
        _state.value = s.copy(rows = updated)
    }

    private fun saveGameToDb(s: GameUiState) {
        // zberemo samo oddane ugibe (kjer pattern != null)
        val submitted = s.rows
            .mapIndexedNotNull { idx, r ->
                if (r.pattern == null) null
                else Triple(idx, r.letters.replace(" ", ""), r.pattern)
            }

        val attemptsUsed = submitted.size.coerceIn(0, 6)

        val game = GameEntity(
            date = s.date,
            mode = s.mode.name, // "DAILY" / "PRACTICE"
            solution = s.solution,
            won = s.isWin,
            attemptsUsed = attemptsUsed,
            finishedAtMillis = System.currentTimeMillis()
        )

        val guesses = submitted.map { (idx, word, pattern) ->
            GuessEntity(
                gameId = 0, // repo bo nastavil pravi gameId po insertu
                guessIndex = idx,
                guessWord = word,
                pattern = pattern
            )
        }

        viewModelScope.launch {
            repo.saveFinishedGame(game, guesses)
        }
    }
}

class GameViewModelFactory(
    private val repo: GameRepository,
    private val solution: String,
    private val mode: GameMode = GameMode.DAILY,
    private val date: String = todayIsoDate()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(repo, solution, mode, date) as T
    }
}
