package com.aifusion.app.research

data class ResearchEvidence(
    val title: String,
    val url: String,
    val agent: String,
    val relevance: Int = 0
)
