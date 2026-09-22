package com.max.assistant.agent

import org.json.JSONObject

interface AgentTool {
    val name: String
    val description: String
    val requiresConfirmation: Boolean
    suspend fun execute(params: JSONObject): ToolResult
}

data class ToolResult(
    val success: Boolean,
    val message: String,
    val data: JSONObject = JSONObject()
)

data class AgentStep(
    val tool: String,
    val params: JSONObject,
    val reason: String
)

data class AgentDecision(
    val done: Boolean,
    val steps: List<AgentStep>,
    val reply: String = ""
)

data class AgentRunResult(
    val success: Boolean,
    val reply: String,
    val steps: Int,
    val awaitingConfirmation: Boolean = false,
    val pendingTool: String? = null
)

class AgentToolRegistry(tools: List<AgentTool>) {
    private val byName = tools.associateBy { it.name }
    fun get(name: String): AgentTool? = byName[name]
    fun descriptions(): String = byName.values.joinToString("\n") {
        "- ${it.name}: ${it.description}${if (it.requiresConfirmation) " [requires voice confirmation]" else ""}"
    }
}
