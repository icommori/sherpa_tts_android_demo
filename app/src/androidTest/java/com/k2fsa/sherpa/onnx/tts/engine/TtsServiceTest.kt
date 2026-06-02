package com.k2fsa.sherpa.onnx.tts.engine

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TtsServiceTest {

    private fun testTtsForLanguage(locale: Locale, textToSpeak: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val initLatch = CountDownLatch(1)
        var ttsInstance: TextToSpeech? = null

        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                initLatch.countDown()
            }
        }

        // Initialize TTS specifying our engine package
        ttsInstance = TextToSpeech(context, listener, context.packageName)

        // Wait for initialization to complete (timeout after 15 seconds)
        assertTrue("TTS failed to initialize", initLatch.await(15, TimeUnit.SECONDS))

        // Set Language
        val result = ttsInstance.setLanguage(locale)
        assertTrue(
            "Language not supported or missing data: $result",
            result >= TextToSpeech.LANG_AVAILABLE
        )

        // Latch to wait for speaking to finish
        val speakLatch = CountDownLatch(1)
        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                speakLatch.countDown()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                fail("TTS encountered an error during synthesis")
            }
        })

        // Speak
        val utteranceId = "test_utterance_${System.currentTimeMillis()}"
        
        // Use a bundle to set audio attributes (optional, but good practice)
        val speakResult = ttsInstance.speak(
            textToSpeak,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )

        assertEquals("Failed to enqueue text for speaking", TextToSpeech.SUCCESS, speakResult)

        // Wait for speech to finish playing (timeout after 40 seconds to account for synthesis time)
        assertTrue("Speech playback timed out", speakLatch.await(40, TimeUnit.SECONDS))

        // Cleanup
        ttsInstance.shutdown()
    }

    @Test
    fun testChineseEnglishTts() {
        // Test Kokoro Model (Chinese / English)
        testTtsForLanguage(Locale.CHINESE, "Good morning, 開元通訊你好！這是一個測試。")
    }

    @Test
    fun testJapaneseTts() {
        // Test Supertonic Model (Japanese)
        testTtsForLanguage(Locale.JAPANESE, "今日は調子はどうですか？これは次世代Kaldiを使用したテキスト読み上げエンジンです。")
    }
}
