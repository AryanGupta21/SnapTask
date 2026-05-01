package com.snaptask.ocr

import com.snaptask.network.models.ExtractedEntity

class EntityExtractor {
    // ML Kit entity-extraction was removed from the standalone SDK.
    // Raw text is sufficient for OpenClaw intent classification.
    suspend fun annotate(text: String): List<ExtractedEntity> = emptyList()
}
