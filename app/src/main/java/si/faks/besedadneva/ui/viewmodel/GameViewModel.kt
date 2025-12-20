package si.faks.besedadneva.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val letters: String,   // dolžina 5, vsebuje ' ' za prazno
    val pattern: String?   // npr. "GYXGX", ali null dokler ni oddano
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
    val keyboard: Map<Char, Char> = emptyMap()
)

private fun todayIsoDate(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(Date())
}

class GameViewModel(
    private val repo: GameRepository,
    private val gameId: String, // <--- NOVO: Unikaten ID igre
    solution: String,
    mode: GameMode = GameMode.DAILY,
    date: String = todayIsoDate() // To je le za prikaz uporabniku
) : ViewModel() {

    private val validator = WordValidator()
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
            message = null,
            keyboard = emptyMap()
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
        val guessCompact = row.letters.replace(" ", "")

        if (guessCompact.length != 5) {
            _state.value = s.copy(message = "Vnesi 5 črk.")
            return
        }

        val guess = guessCompact.uppercase()

        // ✅ VALIDACIJA prek SSKJ/Fran
        viewModelScope.launch {
            _state.value = _state.value.copy(message = "Preverjam v SSKJ...")

            val ok = withContext(Dispatchers.IO) {
                validator.existsInSSKJ(guess)
            }

            if (!ok) {
                // Pobriši trenutni row in dovoli ponovno tipkanje
                val st = _state.value
                val clearedRows = st.rows.toMutableList().apply {
                    this[st.currentRowIndex] = GuessRowUi(letters = "     ", pattern = null)
                }
                _state.value = st.copy(
                    rows = clearedRows,
                    currentColIndex = 0,
                    message = "Ni v SSKJ. Poskusi znova."
                )
                return@launch
            }

            // Če ok → normalna Wordle logika
            applyGuess(guess)
        }
    }

    private fun applyGuess(guess: String) {
        val s = _state.value
        val pattern = evaluateGuessPattern(guess, s.solution)

        val updatedRows = s.rows.toMutableList().apply {
            this[s.currentRowIndex] = this[s.currentRowIndex].copy(pattern = pattern)
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
            message = if (finished) (if (win) "Bravo! 🎉" else "Konec igre.") else null
        )

        _state.value = nextState

        if (finished) saveGameToDb(nextState)
    }

    private fun computeKeyboard(rows: List<GuessRowUi>): Map<Char, Char> {
        val map = mutableMapOf<Char, Char>()

        fun better(newP: Char, oldP: Char?): Boolean {
            if (oldP == null) return true
            if (oldP == 'G') return false
            if (oldP == 'Y') return newP == 'G'
            return newP == 'Y' || newP == 'G' // oldP == 'X'
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

        // TUKAJ JE KLJUČNA SPREMEMBA:
        // Namesto s.date (ki je vedno enak za današnji dan), uporabimo gameId.
        // Če je mode == PRACTICE, bo gameId vseboval timestamp in bo unikaten.
        val game = GameEntity(
            date = gameId,  // <--- Uporabimo unikaten ID
            mode = s.mode.name,
            solution = s.solution,
            won = s.isWin,
            attemptsUsed = attemptsUsed,
            finishedAtMillis = System.currentTimeMillis()
        )

        val guesses = submitted.map { (idx, word, pat) ->
            GuessEntity(
                gameId = 0, // repo bo to uredil (če uporabljaš @Transaction insert)
                guessIndex = idx,
                guessWord = word,
                pattern = pat
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            // ✅ če imaš to metodo v repo
            repo.saveFinishedGame(game, guesses)

            // Če NIMAŠ saveFinishedGame, uporabi tole (odkomentiraj):
            // val id = repo.insertGame(game)
            // guesses.forEach { g -> repo.insertGuess(g.copy(gameId = id)) }
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

        // Logika za generiranje ID-ja
        val gameId = if (mode == GameMode.DAILY) {
            // Daily: Uporabimo datum, da se ohranja 1 igra na dan
            java.time.LocalDate.now().toString()
        } else {
            // Practice: Generiramo unikaten ID s časovnim žigom
            "practice_${System.currentTimeMillis()}"
        }

        return GameViewModel(
            repo = repo,
            gameId = gameId, // <--- Zdaj pravilno podamo ID
            solution = solution,
            mode = mode
        ) as T
    }
}