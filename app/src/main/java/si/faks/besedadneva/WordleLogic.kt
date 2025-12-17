package si.faks.besedadneva.wordle

enum class LetterState { CORRECT, PRESENT, ABSENT }

/**
 * Vrne seznam stanj za vsako črko v guessu (Wordle pravila, tudi z dvojnimi črkami).
 * - CORRECT: prava črka na pravem mestu (green)
 * - PRESENT: črka je v besedi, ampak drugje (yellow)
 * - ABSENT: črke ni v besedi (gray)
 */
fun evaluateGuessStates(guessRaw: String, solutionRaw: String): List<LetterState> {
    val guess = guessRaw.trim().uppercase()
    val solution = solutionRaw.trim().uppercase()

    require(guess.length == solution.length) { "Guess and solution must have same length" }
    require(guess.isNotEmpty()) { "Words must not be empty" }

    val n = guess.length
    val result = MutableList(n) { LetterState.ABSENT }

    // koliko posameznih črk še "ostane" za rumene (po tem ko zeleno porabimo)
    val remaining = mutableMapOf<Char, Int>()

    // 1. pass: označi green in preštej ostanke solution črk (ki niso porabljene za green)
    for (i in 0 until n) {
        val g = guess[i]
        val s = solution[i]
        if (g == s) {
            result[i] = LetterState.CORRECT
        } else {
            remaining[s] = (remaining[s] ?: 0) + 1
        }
    }

    // 2. pass: za ne-green pozicije označi yellow, če je črka še na voljo
    for (i in 0 until n) {
        if (result[i] == LetterState.CORRECT) continue
        val g = guess[i]
        val cnt = remaining[g] ?: 0
        if (cnt > 0) {
            result[i] = LetterState.PRESENT
            remaining[g] = cnt - 1
        }
    }

    return result
}

/**
 * Vrne Wordle "pattern" string:
 * G = green (CORRECT), Y = yellow (PRESENT), X = gray (ABSENT)
 */
fun evaluateGuessPattern(guess: String, solution: String): String {
    return evaluateGuessStates(guess, solution).joinToString("") {
        when (it) {
            LetterState.CORRECT -> "G"
            LetterState.PRESENT -> "Y"
            LetterState.ABSENT -> "X"
        }
    }
}
