package com.max.assistant.agent

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

interface ConfirmationGateway {
    suspend fun request(tool: AgentTool, params: JSONObject): Boolean?
}

data class PendingConfirmation(
    val tool: String,
    val description: String,
    val params: JSONObject
)

class VoiceConfirmationGateway(private val timeoutMs: Long = 30_000L) : ConfirmationGateway {
    private val pending = MutableStateFlow<PendingConfirmation?>(null)
    private var answer: CompletableDeferred<Boolean>? = null
    val state: StateFlow<PendingConfirmation?> = pending

    override suspend fun request(tool: AgentTool, params: JSONObject): Boolean? {
        val response = CompletableDeferred<Boolean>()
        answer = response
        pending.value = PendingConfirmation(tool.name, tool.description, params)
        return try { withTimeoutOrNull(timeoutMs) { response.await() } } finally {
            answer = null
            pending.value = null
        }
    }

    fun confirm() { answer?.complete(true) }
    fun deny() { answer?.complete(false) }
}
