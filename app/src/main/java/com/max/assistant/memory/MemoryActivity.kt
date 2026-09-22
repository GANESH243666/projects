package com.max.assistant.memory

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MemoryActivity : Activity() {
    private lateinit var list: LinearLayout
    private val store by lazy { MemoryStore(this) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        root.addView(TextView(this).apply { text = "MAX memory और voice"; textSize = 24f })
        root.addView(TextView(this).apply { text = "Speaking rate: 0.95x"; tag = "rate_label" })
        val rate = SeekBar(this).apply {
            max = 150
            progress = ((getSharedPreferences("max_voice_settings", MODE_PRIVATE).getFloat("speaking_rate", 0.95f) - 0.5f) * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    val speed = (0.5f + value / 100f).coerceIn(0.5f, 2f)
                    getSharedPreferences("max_voice_settings", MODE_PRIVATE).edit().putFloat("speaking_rate", speed).apply()
                    root.findViewWithTag<TextView>("rate_label")?.text = "Speaking rate: %.2fx".format(speed)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        root.addView(rate)
        root.addView(Button(this).apply {
            text = "Delete all memory"
            setOnClickListener { lifecycleScope.launch { store.deleteAll(); render(emptyList(), emptyList()) } }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        lifecycleScope.launch {
            combine(store.turns(), store.facts()) { turns, facts -> turns to facts }.collect { (turns, facts) -> render(turns, facts) }
        }
    }

    private fun render(turns: List<ConversationTurn>, facts: List<MemoryFact>) {
        list.removeAllViews()
        list.addView(TextView(this).apply { text = "Saved facts"; textSize = 19f; setPadding(0, 18, 0, 8) })
        facts.forEach { fact -> addRow("${fact.category}: ${fact.fact}", Date(fact.createdAt)) { lifecycleScope.launch { store.deleteFact(fact.id) } } }
        list.addView(TextView(this).apply { text = "Conversation turns"; textSize = 19f; setPadding(0, 18, 0, 8) })
        turns.forEach { turn -> addRow("${turn.userText}\nMAX: ${turn.maxReply}", Date(turn.timestamp)) { lifecycleScope.launch { store.deleteTurn(turn.id) } } }
        if (facts.isEmpty() && turns.isEmpty()) list.addView(TextView(this).apply { text = "No saved memory"; gravity = Gravity.CENTER })
    }

    private fun addRow(text: String, date: Date, delete: () -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 8) }
        row.addView(TextView(this).apply { this.text = "$text\n${DateFormat.getDateTimeInstance().format(date)}"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        row.addView(Button(this).apply { text = "Delete"; setOnClickListener { delete() } })
        list.addView(row)
    }
}
