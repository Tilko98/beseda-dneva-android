package si.faks.besedadneva.data.fran

import org.jsoup.Connection
import org.jsoup.Jsoup
import java.text.Normalizer
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

    /**
     * Najde random 5-črkovni samostalnik iz SSKJ (Fran).
     * - retries: kolikokrat poskusimo z drugo črko/stranjo
     * - timeoutMs: timeout za en request
     */
    suspend fun pickRandomNoun(
        retries: Int = 15,
        timeoutMs: Int = 15_000
    ): FranWord {

        var lastError: Throwable? = null

        repeat(retries) {
            try {
                val letter = letters.random()
                val page = Random.nextInt(1, 12) // povečano, da najde več
                val url =
                    "https://fran.si/iskanje?page=$page&FilteredDictionaryIds=130&View=1&Query=$letter????"

                val resp: Connection.Response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Android) BesedaDneva/1.0")
                    .referrer("https://fran.si/")
                    .timeout(timeoutMs)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute()

                if (resp.statusCode() != 200) {
                    lastError = IllegalStateException("HTTP ${resp.statusCode()}")
                    return@repeat
                }

                val doc = resp.parse()
                val candidates = mutableListOf<FranWord>()

                val entries = doc.select("span.font_xlarge")

                for (entry in entries) {
                    val link = entry.selectFirst("a") ?: continue
                    val rawWord = link.text().trim()

                    val header = entry.parent() ?: continue

                    // spol (m/ž/s) -> samostalnik
                    val genderSpan = header.selectFirst("span.font_small") ?: continue
                    val g = genderSpan.text().trim().firstOrNull() ?: continue
                    if (g !in listOf('m', 'ž', 's')) continue

                    // razlaga (ni nujna)
                    val explanation = header
                        .selectFirst("span[title=Razlaga]")
                        ?.text()
                        ?.trim()
                        ?: ""

                    val clean = normalizeSlovenian(rawWord)

                    // samo 5 črk
                    if (clean.length == 5) {
                        candidates.add(FranWord(clean, explanation))
                    }
                }

                if (candidates.isNotEmpty()) {
                    return candidates.random()
                }

            } catch (t: Throwable) {
                lastError = t
            }
        }

        throw lastError ?: IllegalStateException("Ni našlo primerne besede po $retries poskusih.")
    }

    // odstrani naglase, pusti ČŠŽ
    private fun normalizeSlovenian(input: String): String {
        return input
            // najprej odstranimo tipične naglase (NE šumnikov)
            .replace("á", "a").replace("à", "a").replace("â", "a").replace("ä", "a")
            .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
            .replace("í", "i").replace("ì", "i").replace("î", "i").replace("ï", "i")
            .replace("ó", "o").replace("ò", "o").replace("ô", "o").replace("ö", "o")
            .replace("ú", "u").replace("ù", "u").replace("û", "u").replace("ü", "u")

            .replace("Á", "A").replace("À", "A").replace("Â", "A").replace("Ä", "A")
            .replace("É", "E").replace("È", "E").replace("Ê", "E").replace("Ë", "E")
            .replace("Í", "I").replace("Ì", "I").replace("Î", "I").replace("Ï", "I")
            .replace("Ó", "O").replace("Ò", "O").replace("Ô", "O").replace("Ö", "O")
            .replace("Ú", "U").replace("Ù", "U").replace("Û", "U").replace("Ü", "U")

            // odstrani vse kar ni črka (šumniki ostanejo!)
            .replace(Regex("[^\\p{L}]"), "")
            .uppercase()
    }

}
