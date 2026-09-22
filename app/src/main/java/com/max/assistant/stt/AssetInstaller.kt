package com.max.assistant.stt

import android.content.Context
import java.io.File

object AssetInstaller {
    fun installModels(context: Context) {
        val root = File(context.filesDir, "models").apply { mkdirs() }
        context.assets.list("models").orEmpty().filter { it.startsWith("vosk-model-") }.forEach { model -> copyTree(context, "models/$model", File(root, model)) }
        installPiper(context)
    }

    fun installPiper(context: Context) {
        val piperRoot = File(context.filesDir, "piper").apply { mkdirs() }
        context.assets.list("piper").orEmpty().filter { !it.equals("README.md", ignoreCase = true) }.forEach { asset -> copyTree(context, "piper/$asset", File(piperRoot, asset)) }
    }
    private fun copyTree(context: Context, assetPath: String, destination: File) {
        if (destination.exists()) return
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) { destination.parentFile?.mkdirs(); context.assets.open(assetPath).use { input -> destination.outputStream().use(input::copyTo) }; return }
        destination.mkdirs(); children.forEach { copyTree(context, "$assetPath/$it", File(destination, it)) }
    }
}
