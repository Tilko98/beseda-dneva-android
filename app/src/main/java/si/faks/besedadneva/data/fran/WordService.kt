package si.faks.besedadneva.data.fran

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

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

    // SPREMEMBA: parameter length
    suspend fun pickRandomNoun(
        length: Int, // privzeto lahko pustimo npr. 5, če se kje še rabi
        retries: Int = 15
    ): FranWord {
        var lastError: Throwable? = null

        repeat(retries) {
            try {
                val letter = letters.random()
                val page = Random.nextInt(1, 5) // Malo manj strani za hitrost

                val candidates = withContext(Dispatchers.IO) {
                    client.fetchNouns(letter, page, length)
                }

                if (candidates.isNotEmpty()) {
                    val picked = candidates.random()
                    return FranWord(picked.word, "")
                }

            } catch (t: Throwable) {
                lastError = t
            }
        }

        throw lastError ?: IllegalStateException("Ni našlo besede dolžine $length.")
    }
}