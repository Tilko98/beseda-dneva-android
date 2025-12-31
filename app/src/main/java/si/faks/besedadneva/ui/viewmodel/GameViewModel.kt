package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.faks.besedadneva.data.db.GameRepository
import si.faks.besedadneva.data.db.entities.GameEntity
import si.faks.besedadneva.data.db.entities.GuessEntity
import si.faks.besedadneva.data.fran.WordValidator
import si.faks.besedadneva.wordle.LetterState
import si.faks.besedadneva.wordle.evaluateGuessStates
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
    val isDialogShown: Boolean
)

class GameViewModel(
    private val repo: GameRepository,
    private val solution: String,
    private val mode: GameMode,
    private val existingGameId: Long? = null
) : ViewModel() {

    private val gameId: String = if (mode == GameMode.DAILY) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.format(Date())
    } else {
        "practice_${System.currentTimeMillis()}"
    }

    private val wordLength = solution.length
    private val maxRows = wordLength + 1

    private val _state = MutableStateFlow(
        GameUiState(
            date = gameId,
            mode = mode,
            solution = solution,
            rows = List(maxRows) { GuessRowUi("", null) },
            currentRowIndex = 0,
            currentColIndex = 0,
            isFinished = false,
            isWin = false,
            message = null,
            isDialogShown = false
        )
    )
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var currentGameId: Long? = existingGameId

    private val _shakeEvent = Channel<Unit>()
    val shakeEvent = _shakeEvent.receiveAsFlow()

    init {
        // NOVO: Če igra že obstaja (resume), naloži prejšnje ugibe
        if (existingGameId != null) {
            viewModelScope.launch {
                val oldGuesses = repo.getGuessesForGame(existingGameId)
                if (oldGuesses.isNotEmpty()) {
                    restoreGameState(oldGuesses)
                }
            }
        }
    }

    // Funkcija za obnovo stanja
    private fun restoreGameState(guesses: List<GuessEntity>) {
        val currentRows = _state.value.rows.toMutableList()
        var winFound = false

        guesses.forEachIndexed { index, guess ->
            if (index < maxRows) {
                currentRows[index] = GuessRowUi(
                    letters = guess.guessWord,
                    pattern = guess.pattern
                )
                if (guess.pattern.all { it == 'G' }) {
                    winFound = true
                }
            }
        }

        val nextIndex = guesses.size.coerceAtMost(maxRows - 1)
        // Če je igra že končana (zmaga ali poraz po vseh poskusih)
        val isFinished = winFound || guesses.size >= maxRows

        _state.update {
            it.copy(
                rows = currentRows,
                currentRowIndex = if (isFinished) nextIndex else guesses.size, // Nastavimo na naslednjo prosto vrstico
                isFinished = isFinished,
                isWin = winFound,
                isDialogShown = isFinished // Pokaži dialog samo, če je dejansko konec
            )
        }
    }

    // ... (onLetter, onBackspace, onEnter ostanejo ENAKI kot prej) ...

    fun onLetter(char: Char) {
        val s = _state.value
        if (s.isFinished) return
        if (s.currentColIndex >= wordLength) return

        val rows = s.rows.toMutableList()
        val currentLetters = rows[s.currentRowIndex].letters
        if (currentLetters.length < wordLength) {
            rows[s.currentRowIndex] = rows[s.currentRowIndex].copy(letters = currentLetters + char)
            _state.update {
                it.copy(
                    rows = rows,
                    currentColIndex = it.currentColIndex + 1
                )
            }
        }
    }

    fun onBackspace() {
        val s = _state.value
        if (s.isFinished) return
        if (s.currentColIndex <= 0) return

        val rows = s.rows.toMutableList()
        val currentLetters = rows[s.currentRowIndex].letters
        if (currentLetters.isNotEmpty()) {
            rows[s.currentRowIndex] = rows[s.currentRowIndex].copy(letters = currentLetters.dropLast(1))
            _state.update {
                it.copy(
                    rows = rows,
                    currentColIndex = it.currentColIndex - 1
                )
            }
        }
    }

    fun onEnter() {
        val s = _state.value
        if (s.isFinished) return

        val currentRow = s.rows[s.currentRowIndex]
        val guessWord = currentRow.letters.trim()

        if (guessWord.length < wordLength) {
            viewModelScope.launch { _shakeEvent.send(Unit) }
            showMessage("Premalo črk")
            return
        }

        viewModelScope.launch {
            val isValid = withContext(Dispatchers.IO) {
                WordValidator().existsInSSKJ(guessWord)
            }

            if (!isValid) {
                _shakeEvent.send(Unit)
                showMessage("Beseda ne obstaja!")
                return@launch
            }

            val states = evaluateGuessStates(guessWord, s.solution)
            val patternString = states.joinToString("") { state ->
                when (state) {
                    LetterState.CORRECT -> "G"
                    LetterState.PRESENT -> "Y"
                    LetterState.ABSENT -> "X"
                }
            }

            val newRows = s.rows.toMutableList()
            newRows[s.currentRowIndex] = currentRow.copy(pattern = patternString)

            val isWin = (guessWord.uppercase() == s.solution.uppercase())
            val isLastTry = (s.currentRowIndex == maxRows - 1)
            val isFinished = isWin || isLastTry

            _state.update {
                it.copy(
                    rows = newRows,
                    isFinished = isFinished,
                    isWin = isWin,
                    currentRowIndex = if (isFinished) it.currentRowIndex else it.currentRowIndex + 1,
                    currentColIndex = if (isFinished) it.currentColIndex else 0,
                    isDialogShown = isFinished
                )
            }
            if (isFinished) {
                saveGameResult()
            } else {
                saveProgress()
            }
        }
    }
    private fun saveProgress() {
        saveGameResult()
    }
    fun dismissDialog() { _state.update { it.copy(isDialogShown = false) } }
    fun clearMessage() { _state.update { it.copy(message = null) } }
    private fun showMessage(msg: String) { _state.update { it.copy(message = msg) } }

    private fun saveGameResult() {
        val s = _state.value
        val submitted = s.rows.mapIndexedNotNull { idx, r ->
            val pat = r.pattern ?: return@mapIndexedNotNull null
            Triple(idx, r.letters.replace(" ", ""), pat)
        }
        val attemptsUsed = submitted.size

        val dbMode = if (s.mode == GameMode.DAILY) "DAILY_${s.solution.length}" else s.mode.name

        // Pazi: Uporabi currentGameId ali 0, če ga še ni
        val game = GameEntity(
            id = currentGameId ?: 0,
            date = gameId,
            mode = dbMode,
            solution = s.solution,
            won = s.isWin,
            attemptsUsed = attemptsUsed,
            finishedAtMillis = System.currentTimeMillis()
        )

        val guesses = submitted.map { (idx, word, pat) ->
            GuessEntity(gameId = 0, guessIndex = idx, guessWord = word, pattern = pat)
        }

        viewModelScope.launch {
            if (currentGameId != null) {
                // Če ID imamo, naredimo UPDATE (za Daily IN Practice)
                repo.updateGame(game, guesses)
            } else {
                // Če ID-ja nimamo (Prvi insert za Practice)
                val newId = repo.savePracticeGame(game, guesses)

                // SHRANIMO NOV ID, da bo naslednji save uporabil update!
                currentGameId = newId
            }
        }
    }
}
// Factory ostane enak
class GameViewModelFactory(
    private val repo: GameRepository,
    private val solution: String,
    private val mode: GameMode,
    private val existingGameId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(repo, solution, mode, existingGameId) as T
    }
}