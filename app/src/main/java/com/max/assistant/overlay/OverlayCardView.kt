package com.max.assistant.overlay

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView

class OverlayCardView(context: Context, reducedMotion: Boolean) : LinearLayout(context) {
    private val status = label(13f, 0xffb4c9da.toInt())
    private val command = label(16f, Color.WHITE)
    private val reply = label(14f, 0xffd8e5ef.toInt())
    private val density = resources.displayMetrics.density

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(20), dp(14), dp(20), dp(14))
        background = GradientDrawable().apply { setColor(0xe616222d.toInt()); cornerRadius = dp(22).toFloat(); setStroke(dp(1), 0x6685b9d1) }
        elevation = dp(18).toFloat()
        alpha = if (reducedMotion) 1f else 0f
        translationY = if (reducedMotion) 0f else dp(28).toFloat()
        if (!reducedMotion) animate().alpha(1f).translationY(0f).setDuration(360L).setInterpolator(DecelerateInterpolator()).start()
        addView(status)
        addView(command)
        addView(reply)
        contentDescription = "MAX voice assistant"
    }

    fun update(state: MaxOverlayProtocol.State, commandText: String?, replyText: String?) {
        status.text = when (state) {
            MaxOverlayProtocol.State.LISTENING -> "सुन रहा हूँ"
            MaxOverlayProtocol.State.THINKING -> "सोच रहा हूँ"
            MaxOverlayProtocol.State.SPEAKING -> "जवाब दे रहा हूँ"
            else -> "MAX"
        }
        if (!commandText.isNullOrBlank()) command.text = commandText
        if (!replyText.isNullOrBlank()) reply.text = replyText
        visibility = VISIBLE
    }

    private fun label(size: Float, color: Int) = TextView(context).apply {
        textSize = size
        setTextColor(color)
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    private fun dp(value: Int) = (value * density).toInt()
}