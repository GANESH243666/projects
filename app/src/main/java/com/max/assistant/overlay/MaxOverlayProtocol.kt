package com.max.assistant.overlay

object MaxOverlayProtocol {
    const val ACTION_STATE = "com.max.assistant.overlay.STATE"
    const val EXTRA_STATE = "state"
    const val EXTRA_COMMAND = "command"
    const val EXTRA_REPLY = "reply"

    enum class State { IDLE, LISTENING, THINKING, SPEAKING, DONE }
}