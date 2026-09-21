package com.max.assistant.voice

sealed interface MaxCommand { data class Ask(val text: String): MaxCommand; data object AcceptCall: MaxCommand; data object RejectCall: MaxCommand; data object ReadScreen: MaxCommand; data object TakePhoto: MaxCommand; data object RecordVideo: MaxCommand; data object TorchOn: MaxCommand; data object TorchOff: MaxCommand }
object CommandParser {
    fun parse(text: String): MaxCommand? { val value = text.lowercase()
        if (value.contains("accept") || value.contains("answer") || value.contains("उठा")) return MaxCommand.AcceptCall
        if (value.contains("reject") || value.contains("decline") || value.contains("काट")) return MaxCommand.RejectCall
        if (value.contains("photo") || value.contains("तस्वीर") || value.contains("फोटो")) return MaxCommand.TakePhoto
        if (value.contains("video") || value.contains("वीडियो")) return MaxCommand.RecordVideo
        if (value.contains("screen") || value.contains("स्क्रीन")) return MaxCommand.ReadScreen
        if (value.contains("torch on") || value.contains("flash on") || value.contains("टॉर्च चालू")) return MaxCommand.TorchOn
        if (value.contains("torch off") || value.contains("flash off") || value.contains("टॉर्च बंद")) return MaxCommand.TorchOff
        return text.takeIf { it.isNotBlank() }?.let(MaxCommand::Ask)
    }
}
