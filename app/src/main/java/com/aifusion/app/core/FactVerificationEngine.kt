package com.aifusion.app.core

data class VerificationReport(
    val verdict: String,
    val score: Int,
    val uniqueSources: Int,
    val uniqueDomains: Int,
    val overlapSources: Int,
    val explanation: String
)

object FactVerificationEngine {
    fun evaluate(results: List<ResearchResult>): VerificationReport {
        val sources = results.flatMap { it.sources }.distinctBy { it.url }
        val domains = sources.mapNotNull { source ->
            runCatching { java.net.URI(source.url).host?.removePrefix("www.") }.getOrNull()
        }.filter { it.isNotBlank() }.toSet()

        val allUrls = results.flatMap { it.sources }.map { it.url }
        val overlap = allUrls.groupingBy { it }.eachCount().count { it.value > 1 }

        var score = (sources.size * 9 + domains.size * 13 + overlap * 8).coerceAtMost(100)
        if (sources.any { it.url.contains(".gov.", true) || it.url.endsWith(".gov", true) }) score = (score + 12).coerceAtMost(100)
        if (sources.any { it.url.contains(".edu.", true) || it.url.endsWith(".edu", true) }) score = (score + 5).coerceAtMost(100)

        val verdict = when {
            score >= 75 && domains.size >= 3 -> "Evidence supports claim"
            score >= 45 && domains.size >= 2 -> "Mixed / needs review"
            else -> "Insufficient evidence"
        }

        val explanation = when (verdict) {
            "Evidence supports claim" ->
                "Several independent source domains were found and the evidence pattern is reasonably consistent. This remains evidence-based verification, not mathematical proof."
            "Mixed / needs review" ->
                "Multiple sources were found, but independence or agreement is limited. Check the primary sources before treating the claim as verified."
            else ->
                "Too little independent evidence was found to support an automatic verification verdict."
        }

        return VerificationReport(
            verdict = verdict,
            score = score,
            uniqueSources = sources.size,
            uniqueDomains = domains.size,
            overlapSources = overlap,
            explanation = explanation
        )
    }
}
