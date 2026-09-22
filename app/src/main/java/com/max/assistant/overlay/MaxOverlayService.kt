package com.max.assistant.overlay

import android.animation.ValueAnimator
import android.app.Service
import android.content.*
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

class MaxOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var chrome: OverlayChromeView
    private lateinit var card: OverlayCardView
    private var chromeAdded = false
    private var cardAdded = false
    private val handler = Handler(Looper.getMainLooper())
    private val hide = Runnable { stopSelf() }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { apply(intent) }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        val reducedMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        chrome = OverlayChromeView(this, reducedMotion)
        card = OverlayCardView(this, reducedMotion)
        ContextCompat.registerReceiver(this, receiver, IntentFilter(MaxOverlayProtocol.ACTION_STATE), ContextCompat.RECEIVER_NOT_EXPORTED)
        addWindows()
    }

    private fun addWindows() {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val base = WindowManager.LayoutParams.MATCH_PARENT
        val chromeParams = WindowManager.LayoutParams(base, base, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START }
        val cardParams = WindowManager.LayoutParams(base, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (resources.displayMetrics.density * 34).toInt()
            width = (resources.displayMetrics.widthPixels * 0.88f).toInt()
        }
        runCatching { windowManager.addView(chrome, chromeParams); chromeAdded = true; windowManager.addView(card, cardParams); cardAdded = true }
            .onFailure { stopSelf() }
    }

    private fun apply(intent: Intent) {
        val state = intent.getStringExtra(MaxOverlayProtocol.EXTRA_STATE)?.let { runCatching { MaxOverlayProtocol.State.valueOf(it) }.getOrNull() } ?: return
        if (state == MaxOverlayProtocol.State.DONE) {
            chrome.setState(MaxOverlayProtocol.State.IDLE)
            handler.removeCallbacks(hide)
            handler.postDelayed(hide, 3000L)
            return
        }
        chrome.setState(state)
        card.update(state, intent.getStringExtra(MaxOverlayProtocol.EXTRA_COMMAND), intent.getStringExtra(MaxOverlayProtocol.EXTRA_REPLY))
        handler.removeCallbacks(hide)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { intent?.let(::apply); return START_NOT_STICKY }
    override fun onDestroy() {
        handler.removeCallbacks(hide)
        runCatching { unregisterReceiver(receiver) }
        if (cardAdded) runCatching { windowManager.removeView(card) }
        if (chromeAdded) runCatching { windowManager.removeView(chrome) }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun emit(context: Context, state: MaxOverlayProtocol.State, command: String? = null, reply: String? = null) {
            val intent = Intent(context, MaxOverlayService::class.java).setAction(MaxOverlayProtocol.ACTION_STATE)
                .putExtra(MaxOverlayProtocol.EXTRA_STATE, state.name).setPackage(context.packageName)
            command?.let { intent.putExtra(MaxOverlayProtocol.EXTRA_COMMAND, it) }
            reply?.let { intent.putExtra(MaxOverlayProtocol.EXTRA_REPLY, it) }
            context.startService(intent)
            context.sendBroadcast(intent)
        }
    }
}