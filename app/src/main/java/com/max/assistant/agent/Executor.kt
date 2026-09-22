package com.max.assistant.agent

import android.content.Context
import com.max.assistant.overlay.MaxOverlayProtocol
import com.max.assistant.overlay.MaxOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class Executor(
    private val context: Context,
    private val registry: AgentToolRegistry,
    private val planner: Planner,
    private val confirmation: ConfirmationGateway,
    private val maxSteps: Int = 6
) {
    suspend fun run(command: String): AgentRunResult = withContext(Dispatchers.IO) {
        var decision = planner.plan(command)
        var history = StringBuilder()
        var completed = 0
        var lastMessage = ""

        while (completed < maxSteps) {
            if (decision.done && decision.steps.isEmpty()) {
                return@withContext finish(decision.reply.ifBlank { lastMessage }, completed)
            }
            val step = decision.steps.firstOrNull()
                ?: return@withContext finish(decision.reply.ifBlank { lastMessage }, completed)
            if (completed >= maxSteps) break

            val tool = registry.get(step.tool)
            if (tool == null) {
                history.append("Tool ${step.tool}: unknown tool\n")
                lastMessage = "I do not have a tool for ${step.tool}"
            } else {
                emitStatus(step.reason)
                if (tool.requiresConfirmation) {
                    emitStatus("Confirmation needed: ${step.reason}")
                    val approved = confirmation.request(tool, step.params)
                    if (approved != true) {
                        return@withContext AgentRunResult(false, "Confirmation was not received.", completed, true, tool.name)
                    }
                }
                val result = runCatching { tool.execute(step.params) }.getOrElse { ToolResult(false, it.message ?: "Tool failed") }
                completed++
                lastMessage = result.message
                history.append("Step $completed ${tool.name}: ${result.message}\n")
                if (!result.success) {
                    return@withContext finish(result.message, completed, false)
                }
            }

            if (completed >= maxSteps) break
            decision = planner.next(command, history.toString())
        }

        emitStatus("Task limit reached")
        AgentRunResult(false, lastMessage.ifBlank { "MAX stopped after six steps." }, completed)
    }

    private fun emitStatus(status: String) {
        MaxOverlayService.emit(context, MaxOverlayProtocol.State.THINKING, command = status)
    }

    private fun finish(reply: String, steps: Int, success: Boolean = true): AgentRunResult {
        val text = reply.ifBlank { if (success) "Task completed." else "Task failed." }
        MaxOverlayService.emit(context, MaxOverlayProtocol.State.SPEAKING, reply = text)
        return AgentRunResult(success, text, steps)
    }
}
