package com.aifusion.app.research

import java.util.UUID

data class ResearchTask(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val agent: String,
    val status: ResearchTaskStatus = ResearchTaskStatus.PENDING
)

enum class ResearchTaskStatus { PENDING, RUNNING, COMPLETE, ERROR }
