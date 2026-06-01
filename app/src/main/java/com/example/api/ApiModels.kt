package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

@JsonClass(generateAdapter = true)
data class StatusResponse(val status: String, val pairingCode: String? = null)

@JsonClass(generateAdapter = true)
data class ConnectRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class GenericResponse(
    val success: Boolean,
    val error: String? = null,
    val code: String? = null,
    val status: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ConfigUpdate(
    val name: String,
    val prefix: String,
    val geminiKey: String,
    val responses: List<QuickReply>
)

@JsonClass(generateAdapter = true)
data class QuickReply(val trigger: String, val reply: String)

@JsonClass(generateAdapter = true)
data class UpdateBotRequest(val code: String)

interface TermuxApi {
    @GET("/status")
    suspend fun getStatus(): StatusResponse

    @POST("/connect")
    suspend fun connect(@Body request: ConnectRequest): GenericResponse

    @POST("/config")
    suspend fun updateConfig(@Body config: ConfigUpdate): GenericResponse

    @GET("/config")
    suspend fun getConfig(): ConfigUpdate

    @POST("/update-bot")
    suspend fun updateBot(@Body request: UpdateBotRequest): GenericResponse

    @Streaming
    @GET("/get-code")
    suspend fun getCode(): ResponseBody
}

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "system_instruction") val systemInstruction: PartWrapper?,
    val contents: List<Content>
)

@JsonClass(generateAdapter = true)
data class Content(val role: String, val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class PartWrapper(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<Candidate>? = null, val error: GeminiError? = null)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GeminiError(val message: String)

interface GeminiApi {
    @POST("/v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") key: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
