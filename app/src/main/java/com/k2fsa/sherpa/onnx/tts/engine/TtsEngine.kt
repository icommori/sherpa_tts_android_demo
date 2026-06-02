package com.k2fsa.sherpa.onnx.tts.engine

import PreferenceHelper
import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

const val MIN_TTS_SPEED = 0.1f
const val MAX_TTS_SPEED = 5.0f

enum class TtsMode { CHINESE_ENGLISH, JAPANESE, CHINESE_XIAO_YA, ENGLISH_LESSAC }

object TtsEngine {
    val TAG = "TtsEngine"
    @Volatile
    var ttsChinese: OfflineTts? = null

    @Volatile
    var ttsJapanese: OfflineTts? = null

    @Volatile
    var ttsChineseXiaoYa: OfflineTts? = null

    @Volatile
    var ttsEnglishLessac: OfflineTts? = null

    var currentMode: TtsMode? = null

    val tts: OfflineTts?
        get() = when (currentMode) {
            TtsMode.CHINESE_ENGLISH -> ttsChinese
            TtsMode.JAPANESE -> ttsJapanese
            TtsMode.CHINESE_XIAO_YA -> ttsChineseXiaoYa
            TtsMode.ENGLISH_LESSAC -> ttsEnglishLessac
            null -> null
        }

    val isSupertonic: Boolean
        get() = currentMode == TtsMode.JAPANESE

    val supertonicLang: String = "ja"

    val lang: String?
        get() = when (currentMode) {
            TtsMode.CHINESE_ENGLISH -> "en"
            TtsMode.JAPANESE -> "ja"
            TtsMode.CHINESE_XIAO_YA -> "zh"
            TtsMode.ENGLISH_LESSAC -> "en"
            null -> null
        }

    val lang2: String?
        get() = when (currentMode) {
            TtsMode.CHINESE_ENGLISH -> "zh"
            TtsMode.JAPANESE -> null
            TtsMode.CHINESE_XIAO_YA -> null
            TtsMode.ENGLISH_LESSAC -> null
            null -> null
        }

    val speedState: MutableState<Float> = mutableFloatStateOf(1.0F)
    val speakerIdState: MutableState<Int> = mutableIntStateOf(0)

    var speed: Float
        get() = speedState.value
        set(value) {
            speedState.value = value
        }

    var speakerId: Int
        get() = speakerIdState.value
        set(value) {
            speakerIdState.value = value
        }

    private var assets: AssetManager? = null

    private fun initChineseEngine(context: Context): OfflineTts {
        Log.i(TAG, "initChineseEngine...")
        val modelDir = "kokoro-multi-lang-v1_1"
        var dataDir = "kokoro-multi-lang-v1_1/espeak-ng-data"
        val newDir = copyDataDir(context, dataDir)
        dataDir = "$newDir/$dataDir"

        val config = getOfflineTtsConfig(
            modelDir = modelDir,
            modelName = "model.onnx",
            acousticModelName = "",
            vocoder = "",
            voices = "voices.bin",
            lexicon = "kokoro-multi-lang-v1_1/lexicon-us-en.txt,kokoro-multi-lang-v1_1/lexicon-zh.txt",
            dataDir = dataDir,
            dictDir = "",
            ruleFsts = "$modelDir/phone-zh.fst,$modelDir/date-zh.fst,$modelDir/number-zh.fst",
            ruleFars = "",
            isKitten = false,
            isSupertonic = false,
            durationPredictor = "",
            textEncoder = "",
            vectorEstimator = "",
            supertonicVocoder = "",
            ttsJson = "",
            unicodeIndexer = "",
            voiceStyle = "",
            lang = "en",
        )
        return OfflineTts(assetManager = context.assets, config = config)
    }

    private fun initJapaneseEngine(context: Context): OfflineTts {
        Log.i(TAG, "initJapaneseEngine...")
        val modelDir = "sherpa-onnx-supertonic-3-tts-int8-2026-05-11"
        var dataDir = "kokoro-multi-lang-v1_1/espeak-ng-data"
        val newDir = copyDataDir(context, dataDir)
        dataDir = "$newDir/$dataDir"

        val config = getOfflineTtsConfig(
            modelDir = modelDir,
            modelName = "",
            acousticModelName = "",
            vocoder = "",
            voices = "",
            lexicon = "",
            dataDir = dataDir,
            dictDir = "",
            ruleFsts = "",
            ruleFars = "",
            isKitten = false,
            isSupertonic = true,
            durationPredictor = "duration_predictor.int8.onnx",
            textEncoder = "text_encoder.int8.onnx",
            vectorEstimator = "vector_estimator.int8.onnx",
            supertonicVocoder = "vocoder.int8.onnx",
            ttsJson = "tts.json",
            unicodeIndexer = "unicode_indexer.bin",
            voiceStyle = "voice.bin",
            lang = "",
        )
        return OfflineTts(assetManager = context.assets, config = config)
    }

    private fun initChineseXiaoYaEngine(context: Context): OfflineTts {
        Log.i(TAG, "initChineseXiaoYaEngine...")
        val modelDir = "vits-piper-zh_CN-xiao_ya-medium-int8"

        val config = getOfflineTtsConfig(
            modelDir = modelDir,
            modelName = "zh_CN-xiao_ya-medium.onnx",
            acousticModelName = "",
            vocoder = "",
            voices = "",
            lexicon = "lexicon.txt",
            dataDir = "",
            dictDir = "",
            ruleFsts = "$modelDir/phone.fst,$modelDir/date.fst,$modelDir/number.fst",
            ruleFars = "",
            isKitten = false,
            isSupertonic = false,
            durationPredictor = "",
            textEncoder = "",
            vectorEstimator = "",
            supertonicVocoder = "",
            ttsJson = "",
            unicodeIndexer = "",
            voiceStyle = "",
            lang = "",
        )
        return OfflineTts(assetManager = context.assets, config = config)
    }

    private fun initEnglishLessacEngine(context: Context): OfflineTts {
        Log.i(TAG, "initEnglishLessacEngine...")
        val modelDir = "vits-piper-en_US-lessac-low-int8"
        var dataDir = "$modelDir/espeak-ng-data"
        val newDir = copyDataDir(context, dataDir)
        dataDir = "$newDir/$dataDir"

        val config = getOfflineTtsConfig(
            modelDir = modelDir,
            modelName = "en_US-lessac-low.onnx",
            acousticModelName = "",
            vocoder = "",
            voices = "",
            lexicon = "",
            dataDir = dataDir,
            dictDir = "",
            ruleFsts = "",
            ruleFars = "",
            isKitten = false,
            isSupertonic = false,
            durationPredictor = "",
            textEncoder = "",
            vectorEstimator = "",
            supertonicVocoder = "",
            ttsJson = "",
            unicodeIndexer = "",
            voiceStyle = "",
            lang = "en",
        )
        return OfflineTts(assetManager = context.assets, config = config)
    }

    fun needsReinit(mode: TtsMode): Boolean {
        if (ttsChinese == null || ttsJapanese == null || ttsChineseXiaoYa == null || ttsEnglishLessac == null) return true
        return false
    }

    fun initAllEngines(context: Context, mode: TtsMode) {
        cleanOldKokoroData(context)
        if (ttsChinese == null) {
            ttsChinese = initChineseEngine(context)
        }
        if (ttsJapanese == null) {
            ttsJapanese = initJapaneseEngine(context)
        }
        if (ttsChineseXiaoYa == null) {
            ttsChineseXiaoYa = initChineseXiaoYaEngine(context)
        }
        if (ttsEnglishLessac == null) {
            ttsEnglishLessac = initEnglishLessacEngine(context)
        }
        currentMode = mode
        speed = PreferenceHelper(context).getSpeed()
        speakerId = PreferenceHelper(context).getSid()
    }

    /** Delete any extracted kokoro-multi-lang-v* directories that are not the current version. */
    private fun cleanOldKokoroData(context: Context) {
        val currentVersion = "kokoro-multi-lang-v1_1"
        val externalDir = context.getExternalFilesDir(null) ?: return
        externalDir.listFiles { f ->
            f.isDirectory && f.name.startsWith("kokoro-multi-lang-v") && f.name != currentVersion
        }?.forEach {
            Log.i(TAG, "Removing stale kokoro data: ${it.name}")
            it.deleteRecursively()
        }
    }

    fun releaseAll() {
        Log.i(TAG, "releaseAll TTS engine!")
        ttsChinese?.release()
        ttsChinese = null
        ttsJapanese?.release()
        ttsJapanese = null
        ttsChineseXiaoYa?.release()
        ttsChineseXiaoYa = null
        ttsEnglishLessac?.release()
        ttsEnglishLessac = null
    }

    /** Legacy: called by TtsService. */
    fun createTts(context: Context) {
        initAllEngines(context, currentMode ?: TtsMode.CHINESE_ENGLISH)
    }


    private fun copyDataDir(context: Context, dataDir: String): String {
        Log.i(TAG, "data dir is $dataDir")
        copyAssets(context, dataDir)

        val newDataDir = context.getExternalFilesDir(null)!!.absolutePath
        Log.i(TAG, "newDataDir: $newDataDir")
        return newDataDir
    }

    private fun copyAssets(context: Context, path: String) {
        val assets: Array<String>?
        try {
            assets = context.assets.list(path)
            if (assets!!.isEmpty()) {
                copyFile(context, path)
            } else {
                val fullPath = "${context.getExternalFilesDir(null)}/$path"
                val dir = File(fullPath)
                dir.mkdirs()
                for (asset in assets.iterator()) {
                    val p: String = if (path == "") "" else "$path/"
                    copyAssets(context, p + asset)
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to copy $path. $ex")
        }
    }

    private fun copyFile(context: Context, filename: String) {
        try {
            val istream = context.assets.open(filename)
            val newFilename = context.getExternalFilesDir(null).toString() + "/" + filename
            val ostream = FileOutputStream(newFilename)
            // Log.i(TAG, "Copying $filename to $newFilename")
            val buffer = ByteArray(1024)
            var read = 0
            while (read != -1) {
                ostream.write(buffer, 0, read)
                read = istream.read(buffer)
            }
            istream.close()
            ostream.flush()
            ostream.close()
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to copy $filename, $ex")
        }
    }
}
