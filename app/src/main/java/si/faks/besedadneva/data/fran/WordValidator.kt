package si.faks.besedadneva.data.fran

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer

class WordValidator(
    private val http: OkHttpClient = OkHttpClient()
) {
    /**
     * True če beseda obstaja v SSKJ (Fran, dictId=130).
     * Primer URL: https://fran.si/iskanje?FilteredDictionaryIds=130&View=1&Query=lopar
     */
    fun existsInSSKJ(word: String): Boolean {
        val target = normalizeForCompare(word)

        val q = URLEncoder.encode(word.trim().lowercase(), StandardCharsets.UTF_8.toString())
        val url = "https://fran.si/iskanje?FilteredDictionaryIds=130&View=1&Query=$q"

        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) BesedaDneva/1.0")
            .header("Referer", "https://fran.si/")
            .build()

        val html = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return false
            resp.body?.string() ?: return false
        }

        val doc = Jsoup.parse(html)

        // Zadetek: <span class="font_xlarge"><a>lopár</a></span> ...
        val hitLinks = doc.select("span.font_xlarge > a")
        if (hitLinks.isEmpty()) return false

        for (a in hitLinks) {
            val hitText = normalizeForCompare(a.text())

            if (hitText == target) {
                // ✅ opcijsko: preveri da je samostalnik (m/ž/s)
                // v tvojem HTML je spol v bližini istega "header" bloka
                val header = a.parent()?.parent() // a -> span.font_xlarge -> (nek parent)
                val genderSpan = header?.selectFirst("span.font_small")
                val g = genderSpan?.text()?.trim()?.firstOrNull()

                // Če hočeš NUJNO samo samostalnike: odkomentiraj spodaj:
                // if (g !in listOf('m','ž','s')) continue

                return true
            }

            // Fallback: včasih je bolj zanesljiv href (zadnji del url)
            val href = a.attr("href") ?: ""
            val last = href.substringAfterLast("/").substringBefore("?")
            val hitHref = normalizeForCompare(last)
            if (hitHref == target) return true
        }

        return false
    }

    private fun normalizeForCompare(input: String): String {
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
        val noAccents = nfd.replace("\\p{Mn}+".toRegex(), "")   // odstrani naglase
        val lettersOnly = noAccents.replace("[^\\p{L}]".toRegex(), "") // pusti črke (tudi ČŠŽ)
        return lettersOnly.uppercase()
    }
}
