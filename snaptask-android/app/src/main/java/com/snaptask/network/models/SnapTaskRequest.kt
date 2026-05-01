package com.snaptask.network.models

data class SnapTaskRequest(
    val rawText: String,
    val entities: List<ExtractedEntity>,
    val deviceInfo: String = "Samsung Galaxy"
)

data class ExtractedEntity(
    val type: String,
    val text: String
)
