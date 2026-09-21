package com.max.assistant.control

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MaxAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    fun back() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun home() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun readScreen(): String = rootInActiveWindow?.let { node -> collect(node).trim() }.orEmpty()
    fun tap(x: Float, y: Float): Boolean { if (Build.VERSION.SDK_INT < 24) return false; val path = Path().apply { moveTo(x, y) }; return dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 80)).build(), null, null) }
    fun scroll(forward: Boolean): Boolean { val node = rootInActiveWindow ?: return false; return node.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) }
    private fun collect(node: AccessibilityNodeInfo): String { val text = node.text?.toString().orEmpty(); return buildString { if (text.isNotBlank()) append(text).append(' '); for (index in 0 until node.childCount) node.getChild(index)?.let { append(collect(it)); it.recycle() } } }
    companion object { var instance: MaxAccessibilityService? = null }
    override fun onServiceConnected() { instance = this }
    override fun onDestroy() { instance = null; super.onDestroy() }
}
