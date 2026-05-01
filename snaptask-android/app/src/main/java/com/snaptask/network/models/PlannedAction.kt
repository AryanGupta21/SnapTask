package com.snaptask.network.models

data class PlannedAction(
    val type: String,
    val params: Map<String, Any>
)
