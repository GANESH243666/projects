package com.max.assistant.agent

import com.max.assistant.BuildConfig
import com.max.assistant.brain.NetworkState
import com.max.assistant.brain.OfflineBrain
import com.max.assistant.brain.OnlineBrain
import com.max.assistant.memory.MemoryStore
import org.json.JSONArray
import org.json.JSONObject

class Planner(private val context: android.content.Context, private val registry: AgentToolRegistry) {
    private val network = NetworkState(context)
    private val online = OnlineBrain()
    private val offline = OfflineBrain(context)
    private val memory = MemoryStore(context)

    suspend fun plan(command: String): AgentDecision = parseDecision(request(buildPlanPrompt(command), command))

    suspend fun next(command: String, history: String): AgentDecision = parseDecision(request(buildNextPrompt(command, history), command))

    private suspend fun request(prompt: String, query: String): String {
        val memoryContext = memory.contextFor(query)
        return if (network.online() && BuildConfig.LLM_API_KEY.isNotBlank()) {
            runCatching { online.ask(prompt, memoryContext) }.getOrElse { offline.ask(prompt, memoryContext) }
        } else offline.ask(prompt, memoryContext)
    }

    private fun buildPlanPrompt(command: String) = """
        You are MAX Agent Planner. Plan the user's request using only these tools:
        ${registry.descriptions()}
        User command: $command
        Return ONLY valid JSON with this shape:
        {"done":false,"reply":"","steps":[{"tool":"tool.name","params":{},"reason":"short status"}]}
        Use at most one or two immediate steps. Never invent tools. Sensitive tools must remain in steps; the executor handles confirmation.
    """.trimIndent()

    private fun buildNextPrompt(command: String, history: String) = """
        You are MAX Agent Controller. Decide whether the task is complete after these tool results.
        Available tools:
        ${registry.descriptions()}
        Original command: $command
        Execution history:
        $history
        Return ONLY valid JSON:
        {"done":true,"reply":"final concise reply","steps":[]}
        If another action is needed, set done=false and return one next step. Never repeat a successful step.
    """.trimIndent()

    private fun parseDecision(raw: String): AgentDecision {
        val jsonText = raw.substringAfter('{', "").substringBeforeLast('}', "")
        if (jsonText.isBlank()) return AgentDecision(true, emptyList(), raw.trim())
        return runCatching {
            val root = JSONObject("{$jsonText}")
            val steps = root.optJSONArray("steps").toSteps()
            AgentDecision(root.optBoolean("done", steps.isEmpty()), steps, root.optString("reply"))
        }.getOrElse { AgentDecision(true, emptyList(), raw.trim()) }
    }

    private fun JSONArray?.toSteps(): List<AgentStep> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val tool = item.optString("tool").trim()
                if (tool.isNotBlank()) add(AgentStep(tool, item.optJSONObject("params") ?: JSONObject(), item.optString("reason", tool)))
            }
        }
    }
}
