package si.faks.besedadneva.data.fran

import org.jsoup.Jsoup
import java.text.Normalizer
import kotlin.random.Random

data class FranWord(
    val word: String,
    val definition: String
)

class WordService {

    suspend fun pickRandomNoun(): FranWord {
        val letters = listOf(
            'a','b','c','č','d','e','f','g','h','i','j','k','l','m',
            'n','o','p','r','s','š','t','u','v','z','ž'
        )

        val letter = letters.random()
        val page = Random.nextInt(1, 8)

        val url =
            "https://fran.si/iskanje?page=$page&FilteredDictionaryIds=130&View=1&Query=$letter????"

        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(10_000)
            .get()

        val candidates = mutableListOf<FranWord>()
        val entries = doc.select("span.font_xlarge")

        for (entry in entries) {
            val link = entry.selectFirst("a") ?: continue
            val rawWord = link.text().trim()

            val header = entry.parent() ?: continue

            // vzamemo samo samostalnike (m/ž/s)
            val genderSpan = header.selectFirst("span.font_small") ?: continue
            val genderChar = genderSpan.text().trim().firstOrNull() ?: continue
            if (genderChar !in listOf('m', 'ž', 's')) continue

            val explanation = header
                .selectFirst("span[title=Razlaga]")
                ?.text()
                ?.trim()
                ?: ""

            val clean = normalizeForGame(rawWord)

            if (clean.length == 5) {
                candidates += FranWord(word = clean, definition = explanation)
            }
        }

        if (candidates.isEmpty()) throw IllegalStateException("Ni primernih besed iz Frana")

        return candidates.random()
    }

    // ✅ odstrani naglase, pusti ČŠŽ
    private fun normalizeForGame(input: String): String {
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)

        // 1) odstrani samo diakritiko (naglas)
        val noAccents = nfd.replace("\\p{Mn}+".toRegex(), "")

        // 2) odstrani vse kar ni črka (pusti ČŠŽ ker so črke)
        val lettersOnly = noAccents.replace("[^\\p{L}]".toRegex(), "")

        return lettersOnly.uppercase()
    }
}
