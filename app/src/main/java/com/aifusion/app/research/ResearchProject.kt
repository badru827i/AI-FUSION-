package com.aifusion.app.research

import java.util.UUID

data class ResearchProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val query: String,
    val status: ResearchProjectStatus = ResearchProjectStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

enum class ResearchProjectStatus { DRAFT, RUNNING, PAUSED, COMPLETE, ERROR }
