package com.aifusion.app.research

import com.aifusion.app.core.ResearchCore

class ResearchOrchestrator {
    suspend fun execute(query: String): ResearchSession {
        val results = ResearchCore.research(query)
        val evidence = results.flatMap { result ->
            result.sources.map { source ->
                ResearchEvidence(source.title, source.url, result.agentName)
            }
        }.distinctBy { it.url }
        return ResearchSession(query = query, resultsCount = results.size, evidence = evidence)
    }
}

data class ResearchSession(
    val query: String,
    val resultsCount: Int,
    val evidence: List<ResearchEvidence>
)
