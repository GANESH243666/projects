package com.max.assistant.wakeword

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig

class KwsRecognizer(context: Context) {
    companion object {
        const val SAMPLE_RATE = 16000
        const val KEYWORDS_SCORE = 1.5f
        const val KEYWORDS_THRESHOLD = 0.25f
        private const val MODEL_DIR = "kws"
    }

    private val spotter = KeywordSpotter(
        assetManager = context.assets,
        config = KeywordSpotterConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = "$MODEL_DIR/encoder-epoch-12-avg-2-chunk-16-left-64.onnx",
                    decoder = "$MODEL_DIR/decoder-epoch-12-avg-2-chunk-16-left-64.onnx",
                    joiner = "$MODEL_DIR/joiner-epoch-12-avg-2-chunk-16-left-64.onnx",
                ),
                tokens = "$MODEL_DIR/tokens.txt",
                modelType = "zipformer2",
            ),
            keywordsFile = "$MODEL_DIR/keywords.txt",
            keywordsScore = KEYWORDS_SCORE,
            keywordsThreshold = KEYWORDS_THRESHOLD,
        ),
    )
    private val stream: OnlineStream = spotter.createStream()

    fun accept(pcm: ByteArray, length: Int): Boolean {
        val samples = FloatArray(length / 2)
        for (index in samples.indices) {
            val low = pcm[index * 2].toInt() and 0xff
            val high = pcm[index * 2 + 1].toInt()
            samples[index] = ((high shl 8) or low).toShort() / 32768.0f
        }
        stream.acceptWaveform(samples, SAMPLE_RATE)
        while (spotter.isReady(stream)) {
            spotter.decode(stream)
            if (spotter.getResult(stream).keyword.isNotEmpty()) {
                spotter.reset(stream)
                return true
            }
        }
        return false
    }

    fun close() {
        spotter.release()
    }
}