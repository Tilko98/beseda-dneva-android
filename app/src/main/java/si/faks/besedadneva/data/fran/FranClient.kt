package si.faks.besedadneva.data.fran

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.text.Normalizer

data class FranNoun(val word: String, val gender: Char)

class FranClient {
    private val http = OkHttpClient()

    fun fetchNouns(startsWith: Char, page: Int, wordLength: Int): List<FranNoun> {
        // Pripravimo query z wildcardi
        val wildcards = "?".repeat(wordLength - 1)

        val url =
            "https://fran.si/iskanje?page=$page&FilteredDictionaryIds=130&View=1&Query=${startsWith}${wildcards}"

        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        val html = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            resp.body?.string() ?: ""
        }

        val doc = Jsoup.parse(html)
        val out = mutableListOf<FranNoun>()

        val wordSpans = doc.select("span.font_xlarge")
        wordSpans.forEach { span ->
            val rawWord = span.selectFirst("a")?.text()?.trim() ?: return@forEach
            val genderSpan = span.parent()?.selectFirst("span.font_small") ?: return@forEach
            val g = genderSpan.text().trim().firstOrNull() ?: return@forEach
            if (g !in listOf('m', 'ž', 's')) return@forEach

            // 1. Očistimo besedo (odstranimo naglase, ohranimo ČŠŽ)
            val cleanWord = removeAccentsPreservingSumniki(rawWord.uppercase())

            // 2. Preverimo dolžino in če so samo črke
            if (cleanWord.length == wordLength && cleanWord.all { it.isLetter() }) {
                out.add(FranNoun(cleanWord, g))
            }
        }
        return out
    }

    /**
     * Funkcija, ki odstrani naglase (á -> a), a ohrani šumnike (č -> č).
     */
    private fun removeAccentsPreservingSumniki(input: String): String {
        var text = input

        // 1. Zaščitimo šumnike s "placeholdery" (da jih normalizacija ne spremeni v C, S, Z)
        text = text.replace("Č", "@@C@@")
        text = text.replace("Š", "@@S@@")
        text = text.replace("Ž", "@@Z@@")
        text = text.replace("č", "@@c@@")
        text = text.replace("š", "@@s@@")
        text = text.replace("ž", "@@z@@")

        // 2. Normalizacija (razbije znake na osnovo + diakritični znak)
        text = Normalizer.normalize(text, Normalizer.Form.NFD)

        // 3. Odstranimo vse "ne-prostorske" znake (diakritične znake, naglase...)
        // Regex \p{Mn} ujame vse "Marks, non-spacing"
        text = text.replace(Regex("\\p{Mn}+"), "")

        // 4. Vrnemo šumnike nazaj
        text = text.replace("@@C@@", "Č")
        text = text.replace("@@S@@", "Š")
        text = text.replace("@@Z@@", "Ž")
        text = text.replace("@@c@@", "Č") // Vrnemo v uppercase, ker igramo z velikimi
        text = text.replace("@@s@@", "Š")
        text = text.replace("@@z@@", "Ž")

        return text.uppercase()
    }
}