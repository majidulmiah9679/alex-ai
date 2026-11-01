package com.alex.ai.live

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class BidiClientMessage(
    val setup: BidiSetup? = null,
    val realtimeInput: BidiRealtimeInput? = null,
    val clientContent: BidiClientContent? = null,
    val toolResponse: BidiToolResponse? = null
)

@Serializable
data class BidiSetup(
    val model: String,
    val generationConfig: BidiGenerationConfig? = null,
    val systemInstruction: BidiContent? = null,
    val tools: List<BidiToolDeclaration>? = null
)

@Serializable
data class BidiGenerationConfig(
    val responseModalities: List<String> = listOf("AUDIO"),
    val speechConfig: BidiSpeechConfig? = null
)

@Serializable
data class BidiSpeechConfig(
    val voiceConfig: BidiVoiceConfig? = null
)

@Serializable
data class BidiVoiceConfig(
    val prebuiltVoiceConfig: BidiPrebuiltVoice? = null
)

@Serializable
data class BidiPrebuiltVoice(
    val voiceName: String
)

@Serializable
data class BidiContent(
    val parts: List<BidiPart>
)

@Serializable
data class BidiPart(
    val text: String? = null,
    val inlineData: BidiBlob? = null
)

@Serializable
data class BidiBlob(
    val mimeType: String,
    val data: String // Base64 encoded audio
)

@Serializable
data class BidiToolDeclaration(
    val functionDeclarations: List<BidiFunctionSpec>
)

@Serializable
data class BidiFunctionSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject? = null
)

@Serializable
data class BidiRealtimeInput(
    val mediaChunks: List<BidiBlob>
)

@Serializable
data class BidiClientContent(
    val turns: List<BidiTurn>,
    val turnComplete: Boolean = true
)

@Serializable
data class BidiTurn(
    val role: String,
    val parts: List<BidiPart>
)

@Serializable
data class BidiToolResponse(
    val functionResponses: List<BidiFunctionResponse>
)

@Serializable
data class BidiFunctionResponse(
    val id: String,
    val response: JsonObject
)

// Server message schemas
@Serializable
data class BidiServerMessage(
    val serverContent: BidiServerContent? = null,
    val toolCall: BidiToolCall? = null,
    val toolCallCancellation: BidiToolCallCancellation? = null
)

@Serializable
data class BidiServerContent(
    val modelTurn: BidiTurn? = null,
    val turnComplete: Boolean? = null,
    val interrupted: Boolean? = null
)

@Serializable
data class BidiToolCall(
    val functionCalls: List<BidiFunctionCall>
)

@Serializable
data class BidiFunctionCall(
    val id: String,
    val name: String,
    val args: JsonObject? = null
)

@Serializable
data class BidiToolCallCancellation(
    val ids: List<String>
)
