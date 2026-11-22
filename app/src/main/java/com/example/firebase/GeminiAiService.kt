package com.example.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

data class AiCaptionResult(
    val captions: List<String>,
    val hashtags: List<String>,
    val engagementTip: String
)

sealed interface AiStreamChunk {
    data class Streaming(val text: String) : AiStreamChunk
    data class Completed(val text: String) : AiStreamChunk
}

interface AiAssistantDataSource {
    suspend fun generatePostCaptions(
        topicOrVibe: String,
        location: String?,
        filterName: String
    ): Result<AiCaptionResult>

    fun streamPostCompanion(
        authorUsername: String,
        caption: String,
        location: String?,
        hashtags: List<String>
    ): Flow<AiStreamChunk>
}

class GeminiAiService : AiAssistantDataSource {

    private val captionSchema = Schema.obj(
        properties = mapOf(
            "captions" to Schema.array(
                items = Schema.string(),
                description = "3 distinct, engaging Instagram caption options matching the user's vibe"
            ),
            "hashtags" to Schema.array(
                items = Schema.string(),
                description = "6 to 8 relevant hashtags starting with #"
            ),
            "engagementTip" to Schema.string(
                description = "A concise 1-sentence social photography or engagement tip"
            )
        )
    )

    private val captionConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = captionSchema
    }

    private val structuredCaptionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = captionConfig
        )
    }

    private val generalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME
        )
    }

    override suspend fun generatePostCaptions(
        topicOrVibe: String,
        location: String?,
        filterName: String
    ): Result<AiCaptionResult> {
        return try {
            val prompt = buildString {
                append("You are an expert social media creative director for InstaSnap (an Instagram-style app). ")
                append("Generate 3 distinct, natural, modern Instagram captions (short/punchy, storytelling, and poetic/minimal), ")
                append("6-8 trending hashtags (each starting with #), and 1 concise engagement tip. ")
                append("Topic/vibe: \"${topicOrVibe.ifBlank { "Aesthetic everyday photography moment" }}\". ")
                if (!location.isNullOrBlank()) {
                    append("Location: \"$location\". ")
                }
                append("Visual filter mood: \"$filterName\".")
            }

            val response = withTimeout(AI_TIMEOUT_MS) {
                structuredCaptionModel.generateContent(prompt)
            }

            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            val json = JSONObject(jsonString)
            val captionsArray = json.getJSONArray("captions")
            val captions = (0 until captionsArray.length()).map { captionsArray.getString(it) }

            val hashtagsArray = json.getJSONArray("hashtags")
            val hashtags = (0 until hashtagsArray.length()).map { tag ->
                val raw = hashtagsArray.getString(tag).trim()
                if (raw.startsWith("#")) raw else "#$raw"
            }

            val tip = json.optString("engagementTip", "Post during peak evening hours for higher engagement.")

            if (captions.isEmpty()) {
                Result.failure(IllegalStateException("No captions were generated. Please try another prompt."))
            } else {
                Result.success(AiCaptionResult(captions = captions, hashtags = hashtags, engagementTip = tip))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini caption generation timed out", e)
            Result.failure(Exception("AI generation timed out after 60 seconds. Please try again.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini caption generation failed", e)
            Result.failure(e)
        }
    }

    override fun streamPostCompanion(
        authorUsername: String,
        caption: String,
        location: String?,
        hashtags: List<String>
    ): Flow<AiStreamChunk> = flow {
        val prompt = buildString {
            append("You are InstaSnap's creative AI companion. Analyze this social media post and respond concisely with:\n")
            append("1. **Visual & Story Vibe** (2 sentences capturing the mood)\n")
            append("2. **3 Smart Comment Ideas** the user can copy or post as a thoughtful reply\n\n")
            append("Post by @$authorUsername")
            if (!location.isNullOrBlank()) append(" in $location")
            append(":\n\"$caption\"\n")
            if (hashtags.isNotEmpty()) {
                append("Hashtags: ${hashtags.joinToString(" ")}")
            }
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                generalModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiStreamChunk.Streaming(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
            emit(AiStreamChunk.Completed(accumulated.toString()))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini streaming timed out", e)
            throw Exception("AI companion timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini streaming failed", e)
            throw e
        }
    }
}
