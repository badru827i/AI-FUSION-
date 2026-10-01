package com.aifusion.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ResearchSource(
    val title: String,
    val url: String
)

data class ResearchResult(
    val query: String,
    val agentName: String,
    val summary: String,
    val sources: List<ResearchSource>
)

object ResearchCore {
    suspend fun research(query: String): List<ResearchResult> = coroutineScope {
        val clean = query.trim()
        if (clean.isBlank()) return@coroutineScope emptyList()

        listOf(
            "Discovery Agent" to clean,
            "Verification Agent" to (clean + " facts evidence"),
            "Current-Change Agent" to (clean + " latest updates")
        ).map { pair ->
            async(Dispatchers.IO) {
                runAgent(pair.first, pair.second)
            }
        }.awaitAll()
    }

    private suspend fun runAgent(name: String, query: String): ResearchResult = withContext(Dispatchers.IO) {
        val sources = searchWeb(query).distinctBy { it.url }.take(5)
        val summary = if (sources.isEmpty()) {
            "Tiada sumber web berjaya diambil. Semak sambungan Internet atau cuba query yang lebih khusus."
        } else {
            "Ditemui " + sources.size + " sumber. Semak sumber di bawah sebelum membuat kesimpulan; hasil ini ialah carian web dan bukan pengesahan automatik."
        }
        ResearchResult(query, name, summary, sources)
    }

    private fun searchWeb(query: String): List<ResearchSource> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val connection = (URL("https://lite.duckduckgo.com/lite/?q=" + encoded).openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("User-Agent", "AI-FUSION/4.1 Android")

        return try {
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val regex = Regex(
                "<a[^>]*rel=[\\\"]nofollow[\\\"][^>]*href=[\\\"]([^\\\"]+)[\\\"][^>]*>(.*?)</a>",
                RegexOption.IGNORE_CASE
            )
            regex.findAll(html).mapNotNull { match ->
                val url = match.groupValues.getOrNull(1)?.trim().orEmpty()
                val rawTitle = match.groupValues.getOrNull(2)
                    ?.replace(Regex("<[^>]+>"), "")
                    .orEmpty()
                val title = rawTitle.replace("&amp;", "&").replace("&quot;", "\"").trim()
                if (url.startsWith("http") && title.isNotBlank()) {
                    ResearchSource(title, url)
                } else {
                    null
                }
            }.toList()
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
