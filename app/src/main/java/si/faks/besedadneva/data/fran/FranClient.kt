package si.faks.besedadneva.data.fran

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

data class FranNoun(val word: String, val gender: Char)

class FranClient {
    private val http = OkHttpClient()

    fun fetchNouns(startsWith: Char, page: Int): List<FranNoun> {
        val url =
            "https://fran.si/iskanje?page=$page&FilteredDictionaryIds=130&View=1&Query=${startsWith}????"

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

            // normalizacija: odstrani presledke in uporabi uppercase
            val word = rawWord.replace(" ", "").uppercase()

            if (word.length == 5) {
                out.add(FranNoun(word, g))
            }
        }

        return out
    }
}
