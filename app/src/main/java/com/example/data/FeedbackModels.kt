package com.example.data

data class FeedbackDraft(
    val type: String = "suggestion",
    val title: String = "",
    val message: String = "",
    val aiSummary: String = "",
    val aiCategory: String = "",
    val priority: String = "normal"
)

data class FeedbackItem(
    val id: String = "",
    val type: String = "suggestion",
    val title: String = "",
    val message: String = "",
    val aiSummary: String = "",
    val status: String = "new",
    val adminReply: String = "",
    val createdAt: Long = 0L
)
