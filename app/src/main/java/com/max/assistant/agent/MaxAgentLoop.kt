package com.max.assistant.agent

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

class MaxAgentLoop(
    context: Context,
    private val confirmationGateway: VoiceConfirmationGateway = VoiceConfirmationGateway()
) {
    private val appContext = context.applicationContext
    private val tools = AgentToolRegistry(BuiltInAgentTools(appContext).all())
    private val planner = Planner(appContext, tools)
    private val executor = Executor(appContext, tools, planner, confirmationGateway)

    val pendingConfirmation: StateFlow<PendingConfirmation?> = confirmationGateway.state

    suspend fun run(command: String): AgentRunResult = executor.run(command)
    fun confirmVoiceAction() = confirmationGateway.confirm()
    fun denyVoiceAction() = confirmationGateway.deny()
    fun availableTools(): String = tools.descriptions()
}
