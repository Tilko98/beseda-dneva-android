package si.faks.besedadneva.data.fran

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random // Uporabimo java.util.Random za seed
import kotlin.math.abs

data class FranWord(
    val word: String,
    val definition: String = ""
)

class WordService {

    private val letters = listOf(
        'a','b','c','č','d','e','f','g','h','i','j','k','l','m',
        'n','o','p','r','s','š','t','u','v','z','ž'
    )

    private val client = FranClient()

    // Za navadno vajo (popolnoma naključno)
    suspend fun pickRandomNoun(length: Int, retries: Int = 15): FranWord {
        // ... (tvoja obstoječa logika s kotlin.random.Random) ...
        // Ker je koda dolga, jo tu ne kopiram cele, pusti tako kot je v tvoji datoteki,
        // le uporabi kotlin.random.Random za vajo.
        return pickDeterministicNoun(System.currentTimeMillis(), length, retries)
    }

    // NOVO: Za DAILY igro (naključno, a enako za vse na ta dan)
    suspend fun pickDailyNoun(dateString: String, length: Int): FranWord {
        // Pretvorimo datum (npr. "2023-12-30") v hash kodo, da dobimo številko za seed
        // Dodamo length, da je beseda za 4 črke drugačna kot za 5 na isti dan
        val seed = dateString.hashCode().toLong() + length
        return pickDeterministicNoun(seed, length)
    }

    // Skupna logika, ki uporablja določen SEED
    private suspend fun pickDeterministicNoun(seed: Long, length: Int, retries: Int = 15): FranWord {
        // Uporabimo java.util.Random, ker omogoča setSeed
        val rng = Random(seed)
        var lastError: Throwable? = null

        repeat(retries) {
            try {
                // Deterministična izbira črke in strani
                val letterIndex = rng.nextInt(letters.size)
                val letter = letters[letterIndex]

                // Stran med 1 in 5
                val page = rng.nextInt(5) + 1

                val candidates = withContext(Dispatchers.IO) {
                    client.fetchNouns(letter, page, length)
                }

                if (candidates.isNotEmpty()) {
                    // Deterministična izbira besede iz seznama
                    val pickedIndex = rng.nextInt(candidates.size)
                    val picked = candidates[pickedIndex]
                    return FranWord(picked.word, "")
                }

            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("Ni našlo besede dolžine $length.")
    }
}