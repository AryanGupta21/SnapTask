package com.snaptask.network.models

data class SnapTaskResponse(
    val intent: String,
    val confidence: Float,
    val summary: String,
    val actions: List<PlannedAction>
)
