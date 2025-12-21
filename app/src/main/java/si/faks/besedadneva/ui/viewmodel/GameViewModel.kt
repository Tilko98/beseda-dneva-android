package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.data.db.entities.GuessEntity
import si.faks.besedadneva.data.fran.WordValidator
import si.faks.besedadneva.wordle.evaluateGuessPattern
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GameMode { DAILY, PRACTICE }

data class GuessRowUi(
    val letters: String,
    val pattern: String?
)

data class GameUiState(
    val date: String,
    val mode: GameMode,
    val solution: String,
    val rows: List<GuessRowUi>,
    val currentRowIndex: Int,
    val currentColIndex: Int,
    val isFinished: Boolean,
    val isWin: Boolean,
    val message: String? = null,
    val isDialogShown: Boolean = false,
    val keyboard: Map<Char, Char> = emptyMap(),
    val isLoading: Boolean = false // Novo: za indikator nalaganja med preverjanjem na Fran.si
)

private fun todayIsoDate(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(Date())
}

class GameViewModel(
    private val repo: GameRepository,
    private val gameId: String,
    solution: String,
    mode: GameMode = GameMode.DAILY,
    date: String = todayIsoDate()
) : ViewModel() {

    private val normalizedSolution = solution.trim().uppercase()
    private val wordValidator = WordValidator() // Instanca za preverjanje SSKJ

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
            message = null,
            isDialogShown = false
        )
    )
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    fun dismissDialog() {
        _state.value = _state.value.copy(isDialogShown = true)
    }

    fun onLetter(ch: Char) {
        val s = _state.value
        if (s.isFinished || s.isLoading) return
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
        if (s.isFinished || s.isLoading) return
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
        if (s.isFinished || s.isLoading) return
        if (s.currentRowIndex !in 0..5) return

        val row = s.rows[s.currentRowIndex]
        val guess = row.letters.replace(" ", "")

        if (guess.length != 5) {
            _state.value = s.copy(message = "Vnesi 5 črk.")
            return
        }

        // --- NOVO: Preverjanje validnosti besede ---
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, message = "Preverjam...") }

            val isValid = wordValidator.existsInSSKJ(guess)

            withContext(Dispatchers.Main) {
                if (!isValid) {
                    _state.update { it.copy(isLoading = false, message = "Besede '$guess' ni v slovarju!") }
                    return@withContext
                }

                // Beseda je validna, nadaljujemo z Wordle logiko
                val pattern = evaluateGuessPattern(guess, s.solution)
                val updatedRows = s.rows.toMutableList().apply {
                    this[s.currentRowIndex] = row.copy(pattern = pattern)
                }
                val keyboard = computeKeyboard(updatedRows)
                val win = pattern.all { it == 'G' }
                val lastRow = s.currentRowIndex == 5
                val finished = win || lastRow

                val nextState = s.copy(
                    rows = updatedRows,
                    keyboard = keyboard,
                    currentRowIndex = if (!finished) s.currentRowIndex + 1 else s.currentRowIndex,
                    currentColIndex = if (!finished) 0 else s.currentColIndex,
                    isFinished = finished,
                    isWin = win,
                    message = if (finished) (if (win) "Bravo! 🎉" else "Konec igre. Rešitev: ${s.solution}") else null,
                    isDialogShown = false,
                    isLoading = false
                )

                _state.value = nextState
                if (finished) saveGameToDb(nextState)
            }
        }
    }

    private fun computeKeyboard(rows: List<GuessRowUi>): Map<Char, Char> {
        val map = mutableMapOf<Char, Char>()
        fun better(newP: Char, oldP: Char?): Boolean {
            if (oldP == null) return true
            if (oldP == 'G') return false
            if (oldP == 'Y') return newP == 'G'
            return newP == 'Y' || newP == 'G'
        }
        rows.forEach { r ->
            val pat = r.pattern ?: return@forEach
            for (i in 0 until 5) {
                val ch = r.letters[i]
                if (ch == ' ') continue
                val p = pat[i]
                val prev = map[ch]
                if (better(p, prev)) map[ch] = p
            }
        }
        return map
    }

    private fun saveGameToDb(s: GameUiState) {
        val submitted = s.rows.mapIndexedNotNull { idx, r ->
            val pat = r.pattern ?: return@mapIndexedNotNull null
            Triple(idx, r.letters.replace(" ", ""), pat)
        }

        val attemptsUsed = submitted.size.coerceIn(0, 6)

        val game = GameEntity(
            date = gameId,
            mode = s.mode.name,
            solution = s.solution,
            won = s.isWin,
            attemptsUsed = attemptsUsed,
            finishedAtMillis = System.currentTimeMillis()
        )

        val guesses = submitted.map { (idx, word, pat) ->
            GuessEntity(
                gameId = 0,
                guessIndex = idx,
                guessWord = word,
                pattern = pat
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
    private val mode: GameMode
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val gameId = if (mode == GameMode.DAILY) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.format(Date())
        } else {
            "practice_${System.currentTimeMillis()}"
        }

        return GameViewModel(
            repo = repo,
            gameId = gameId,
            solution = solution,
            mode = mode
        ) as T
    }
}