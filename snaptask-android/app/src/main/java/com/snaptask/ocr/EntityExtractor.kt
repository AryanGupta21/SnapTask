package com.snaptask.ocr

import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.snaptask.network.models.ExtractedEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class EntityExtractor {

    private val extractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )

    suspend fun annotate(text: String): List<ExtractedEntity> =
        suspendCancellableCoroutine { continuation ->
            val params = EntityExtractionParams.Builder(text).build()
            extractor.annotate(params)
                .addOnSuccessListener { annotations ->
                    val entities = annotations.flatMap { annotation ->
                        annotation.entities.map { entity ->
                            ExtractedEntity(
                                type = entityTypeName(entity.type),
                                text = annotation.annotatedText
                            )
                        }
                    }
                    continuation.resume(entities)
                }
                .addOnFailureListener {
                    // Model may not be downloaded yet — proceed with raw text only
                    continuation.resume(emptyList())
                }
        }

    private fun entityTypeName(type: Int): String = when (type) {
        Entity.TYPE_DATE_TIME -> "DATE_TIME"
        Entity.TYPE_PHONE     -> "PHONE"
        Entity.TYPE_EMAIL     -> "EMAIL"
        Entity.TYPE_ADDRESS   -> "ADDRESS"
        Entity.TYPE_MONEY     -> "MONEY"
        Entity.TYPE_URL       -> "URL"
        else                  -> "UNKNOWN"
    }
}
