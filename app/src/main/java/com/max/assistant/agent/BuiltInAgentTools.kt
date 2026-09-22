package com.max.assistant.agent

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.max.assistant.calls.CallActions
import com.max.assistant.camera.CameraCaptureService
import com.max.assistant.control.PhoneControl
import com.max.assistant.memory.MemoryStore
import com.max.assistant.messages.SmsReply
import com.max.assistant.tts.PiperTts
import com.max.assistant.wakeword.WakeWordService
import org.json.JSONObject

class BuiltInAgentTools(private val context: Context) {
    private val memory by lazy { MemoryStore(context) }
    private val tts by lazy { PiperTts(context) }

    fun all(): List<AgentTool> = listOf(
        WakeWordTool(context),
        SttTool(),
        TtsTool(tts),
        MemoryReadTool(memory),
        MemoryDeleteTool(memory),
        AcceptCallTool(context),
        EndCallTool(context),
        SendSmsTool(),
        TorchTool(context),
        OpenAppTool(context),
        CameraTool(context)
    )
}

private class WakeWordTool(private val context: Context) : AgentTool {
    override val name = "wake_word.listen"
    override val description = "Start or keep MAX wake-word listening in the background"
    override val requiresConfirmation = false
    override suspend fun execute(params: JSONObject): ToolResult {
        ContextCompat.startForegroundService(context, Intent(context, WakeWordService::class.java))
        return ToolResult(true, "Wake-word listener is active")
    }
}

private class SttTool : AgentTool {
    override val name = "stt.transcript"
    override val description = "Use the speech transcript already captured by MAX"
    override val requiresConfirmation = false
    override suspend fun execute(params: JSONObject): ToolResult {
        val text = params.optString("text").trim()
        return if (text.isBlank()) ToolResult(false, "No speech transcript was supplied") else ToolResult(true, text, JSONObject().put("text", text))
    }
}

private class TtsTool(private val tts: PiperTts) : AgentTool {
    override val name = "tts.speak"
    override val description = "Speak a short response using MAX voice"
    override val requiresConfirmation = false
    override suspend fun execute(params: JSONObject): ToolResult {
        val text = params.optString("text").trim()
        if (text.isBlank()) return ToolResult(false, "Text is required")
        tts.speak(text)
        return ToolResult(true, "MAX is speaking", JSONObject().put("text", text))
    }
}

private class MemoryReadTool(private val memory: MemoryStore) : AgentTool {
    override val name = "memory.read"
    override val description = "Read relevant saved facts and recent conversation context"
    override val requiresConfirmation = false
    override suspend fun execute(params: JSONObject): ToolResult {
        val context = memory.contextFor(params.optString("query"))
        return ToolResult(true, context, JSONObject().put("context", context))
    }
}

private class MemoryDeleteTool(private val memory: MemoryStore) : AgentTool {
    override val name = "memory.delete"
    override val description = "Delete all saved MAX memory"
    override val requiresConfirmation = true
    override suspend fun execute(params: JSONObject): ToolResult {
        memory.deleteAll()
        return ToolResult(true, "All saved memory was deleted")
    }
}

private class AcceptCallTool(private val context: Context) : AgentTool {
    override val name = "calls.accept"
    override val description = "Answer the currently ringing call"
    override val requiresConfirmation = false
    override suspend fun execute(params: JSONObject): ToolResult {
        CallActions.accept(context)
        return ToolResult(true, "Incoming call answered")
    }
}

private class EndCallTool(private val context: Context) : AgentTool {
    override val name = "calls.end"
    override val description = "End the active phone call"
    override val requiresConfirmation = true
    override suspend fun execute(params: JSONObject): ToolResult {
        CallActions.reject(context)
        return ToolResult(true, "Call ended")
    }
}

private class SendSmsTool : AgentTool {
    override val name = "messages.send_sms"
    override val description = "Send an SMS to a phone number"
    override val requiresConfirmation = true
    override suspend fun execute(params: JSONObject): ToolResult {
        val number = params.optString("number").trim()
        val message = params.optString("message").trim()
        if (number.isBlank() || message.isBlank()) return ToolResult(false, "Both number and message are required")
        return if (SmsReply.send(number, message)) ToolResult(true, "SMS sent to $number") else ToolResult(false, "SMS could not be sent")
    }
}

private class TorchTool(private val context: Context) : AgentTool {
    override val name = "control.torch"
    override val description = "Turn the phone torch on or off"
    override val requiresConfirmation = false
    override suspend fun execute(params: JSONObject): ToolResult {
        val enabled = params.optBoolean("enabled", true)
        PhoneControl.torch(context, enabled)
        return ToolResult(true, "Torch ${if (enabled) "on" else "off"}")
    }
}

private class OpenAppTool(private val context: Context) : AgentTool {
    override val name = "control.open_app"
    override val description = "Open an installed app by package name"
    override val requiresConfirmation = false
    override suspend fun execute(params: JSONObject): ToolResult {
        val packageName = params.optString("package").trim()
        if (packageName.isBlank()) return ToolResult(false, "Package name is required")
        return if (PhoneControl.openApp(context, packageName)) ToolResult(true, "Opened $packageName") else ToolResult(false, "App is not installed")
    }
}

private class CameraTool(private val context: Context) : AgentTool {
    override val name = "camera.capture"
    override val description = "Capture a photo or open video capture"
    override val requiresConfirmation = false
    override suspend fun execute(params: JSONObject): ToolResult {
        val video = params.optString("mode").equals("video", true)
        val action = if (video) CameraCaptureService.ACTION_VIDEO else CameraCaptureService.ACTION_PHOTO
        ContextCompat.startForegroundService(context, Intent(context, CameraCaptureService::class.java).setAction(action))
        return ToolResult(true, if (video) "Video capture opened" else "Photo capture started")
    }
}
