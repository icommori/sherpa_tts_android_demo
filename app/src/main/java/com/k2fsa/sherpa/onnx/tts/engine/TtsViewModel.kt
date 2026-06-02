package com.k2fsa.sherpa.onnx.tts.engine

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.ViewModel
import java.util.Locale

class TtsApp : Application() {
    companion object {
        lateinit var instance: TtsApp
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}