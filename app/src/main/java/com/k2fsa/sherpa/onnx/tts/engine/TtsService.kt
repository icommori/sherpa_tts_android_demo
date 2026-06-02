package com.k2fsa.sherpa.onnx.tts.engine

import android.media.AudioFormat
import com.k2fsa.sherpa.onnx.GenerationConfig
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log

/*
https://developer.android.com/reference/java/util/Locale#getISO3Language()
https://developer.android.com/reference/java/util/Locale#getISO3Country()

eng, USA,
eng, USA, POSIX
eng,
eng, GBR
afr,
afr, NAM
afr, ZAF
agq
agq, CMR
aka,
aka, GHA
amh,
amh, ETH
ara,
ara, 001
ara, ARE
ara, BHR,
deu
deu, AUT
deu, BEL
deu, CHE
deu, ITA
deu, ITA
deu, LIE
deu, LUX
spa,
spa, 419
spa, ARG,
spa, BRA
fra,
fra, BEL,
fra, FRA,

E  Failed to check TTS data, no activity found for Intent
{ act=android.speech.tts.engine.CHECK_TTS_DATA pkg=com.k2fsa.sherpa.chapter5 })

E Failed to get default language from engine com.k2fsa.sherpa.chapter5
Engine failed voice data integrity check (null return)com.k2fsa.sherpa.chapter5
Failed to get default language from engine com.k2fsa.sherpa.chapter5

*/

class TtsService : TextToSpeechService() {
    val TAG = "TtsService"
    override fun onCreate() {
        Log.i(TAG, "onCreate tts service")
        super.onCreate()

        // see https://github.com/Miserlou/Android-SDK-Samples/blob/master/TtsEngine/src/com/example/android/ttsengine/RobotSpeakTtsService.java#L68
        onLoadLanguage(TtsEngine.lang, "", "")
        if (TtsEngine.lang2 != null) {
            onLoadLanguage(TtsEngine.lang2, "", "")
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy tts service")
        // TtsEngine.releaseAll() // 註解掉：保留在記憶體中，下次使用就能「秒發音」
        super.onDestroy()
    }

    override fun onIsLanguageAvailable(_lang: String?, _country: String?, _variant: String?): Int {
        val lang = _lang ?: ""
        val supportedLangs = listOf("en", "eng", "zh", "zho", "cmn", "ja", "jpn")
        if (lang in supportedLangs) {
            return TextToSpeech.LANG_AVAILABLE
        }
        return TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf(TtsEngine.lang!!, "", "")
    }

    override fun onLoadLanguage(_lang: String?, _country: String?, _variant: String?): Int {
        Log.i(TAG, "onLoadLanguage: $_lang, $_country")
        val lang = _lang ?: ""

        if (lang == "ja" || lang == "jpn") {
            Log.i(TAG, "creating tts, switching to Japanese")
            TtsEngine.initAllEngines(application, TtsMode.JAPANESE)
            return TextToSpeech.LANG_AVAILABLE
        } else if (lang == "en" || lang == "eng" || lang == "zh" || lang == "zho" || lang == "cmn") {
            Log.i(TAG, "creating tts, switching to Chinese/English")
            TtsEngine.initAllEngines(application, TtsMode.CHINESE_ENGLISH)
            return TextToSpeech.LANG_AVAILABLE
        }
        
        Log.i(TAG, "lang $lang not supported")
        return TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onStop() {}

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) {
            return
        }
        val language = request.language
        val country = request.country
        val variant = request.variant
        val text = request.charSequenceText.toString()
        // Map Android TTS speech rate (where 100 == normal) to engine speed (1.0 == normal)
        // Allow per-request override from external apps; fallback to engine default if absent.
        val rate = runCatching { request.speechRate }.getOrDefault(-1)
        val engineSpeed = if (rate > 0) {
            // Map 100 -> 1.0f
            val mapped = rate / 100.0f
            mapped.coerceIn(MIN_TTS_SPEED, MAX_TTS_SPEED)
        } else {
            // Fallback to current engine/global setting
            TtsEngine.speed
        }

        val ret = onIsLanguageAvailable(language, country, variant)
        if (ret == TextToSpeech.LANG_NOT_SUPPORTED) {
            callback.error()
            return
        }
        
        if (language == "ja" || language == "jpn") {
            TtsEngine.currentMode = TtsMode.JAPANESE
        } else {
            TtsEngine.currentMode = TtsMode.CHINESE_ENGLISH
        }
        
        Log.i(TAG, "text: $text, engineSpeed: $engineSpeed")
        val tts = TtsEngine.tts!!

        // Note that AudioFormat.ENCODING_PCM_FLOAT requires API level >= 24
        // callback.start(tts.sampleRate(), AudioFormat.ENCODING_PCM_FLOAT, 1)

        callback.start(tts.sampleRate(), AudioFormat.ENCODING_PCM_16BIT, 1)

        if (text.isBlank() || text.isEmpty()) {
            callback.done()
            return
        }

        val ttsCallback: (FloatArray) -> Int = fun(floatSamples): Int {
            // convert FloatArray to ByteArray
            val samples = floatArrayToByteArray(floatSamples)
            val maxBufferSize: Int = callback.maxBufferSize
            var offset = 0
            while (offset < samples.size) {
                val bytesToWrite = Math.min(maxBufferSize, samples.size - offset)
                callback.audioAvailable(samples, offset, bytesToWrite)
                offset += bytesToWrite
            }

            // 1 means to continue
            // 0 means to stop
            return 1
        }

        Log.i(TAG, "text: $text")
        tts.generateWithConfigAndCallback(
            text = text,
            config = GenerationConfig(sid = TtsEngine.speakerId, speed = engineSpeed),
            callback = ttsCallback,
        )

        callback.done()
    }

    private fun floatArrayToByteArray(audio: FloatArray): ByteArray {
        // byteArray is actually a ShortArray
        val byteArray = ByteArray(audio.size * 2)
        for (i in audio.indices) {
            val sample = (audio[i] * 32767).toInt()
            byteArray[2 * i] = sample.toByte()
            byteArray[2 * i + 1] = (sample shr 8).toByte()
        }
        return byteArray
    }
}
