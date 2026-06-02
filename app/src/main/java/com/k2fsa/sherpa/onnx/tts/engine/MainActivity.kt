@file:OptIn(ExperimentalMaterial3Api::class)

package com.k2fsa.sherpa.onnx.tts.engine

import PreferenceHelper
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import com.k2fsa.sherpa.onnx.tts.engine.R
import androidx.core.content.FileProvider
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.tts.engine.ui.theme.SherpaOnnxTtsEngineTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.TimeSource



class MainActivity : ComponentActivity() {
    val TAG = "MainActivity"
    private var mediaPlayer: MediaPlayer? = null
    private var track: AudioTrack? = null
    private var stopped: Boolean = false
    private var samplesChannel = Channel<FloatArray>(capacity = 128)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferenceHelper = PreferenceHelper(this)
        setContent {
            val uiScope = rememberCoroutineScope()
            var selectedMode by remember { mutableStateOf(TtsMode.CHINESE_ENGLISH) }
            var isIniting by remember { mutableStateOf(false) }
            var testText by remember { mutableStateOf(sampleTextForMode(TtsMode.CHINESE_ENGLISH)) }
            val context = LocalContext.current
            val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
            var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()) }
            val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() }

            /** Reinit TTS+AudioTrack on background thread, show progress during init */
            fun reinitEngine(mode: TtsMode, onDone: () -> Unit = {}) {
                testText = sampleTextForMode(mode)
                uiScope.launch {
                    isIniting = true
                    withContext(Dispatchers.IO) {
                        TtsEngine.initAllEngines(context as MainActivity, mode)
                        initAudioTrack()
                    }
                    isIniting = false
                    onDone()
                }
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (TtsEngine.needsReinit(selectedMode)) {
                    reinitEngine(selectedMode)
                }
            }

            SherpaOnnxTtsEngineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(topBar = {
                            TopAppBar(title = { Text("Next-gen Kaldi: TTS Engine") })
                        }) { paddingValues ->
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                            ) {
                                // ── Bottom footer: logo + branding + QR ──────────────
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .alpha(0.9f)
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Divider(
                                        thickness = 0.5.dp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Logo
                                        Image(
                                            painter = painterResource(id = R.drawable.logo),
                                            contentDescription = "Innocomm Logo",
                                            modifier = Modifier.size(width = 200.dp, height = 32.dp)
                                        )
                                        // Center text
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Powered by sherpa-onnx",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = "© 2026 Innocomm",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        // QR code
                                        Image(
                                            painter = painterResource(id = R.drawable.innocomm),
                                            contentDescription = "QR Code",
                                            modifier = Modifier.size(100.dp)
                                        )
                                    }
                                }
                                 // ── UI content ────────────────────────────────────
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(bottom = 60.dp), // Leave space for footer
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    
                                    // ── Mode Selection Card ──────────────────────────────────
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Language Model", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(selected = selectedMode == TtsMode.CHINESE_ENGLISH, onClick = { if (selectedMode != TtsMode.CHINESE_ENGLISH) { selectedMode = TtsMode.CHINESE_ENGLISH; reinitEngine(TtsMode.CHINESE_ENGLISH) } })
                                                    Text("中＋英", fontSize = 14.sp)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(selected = selectedMode == TtsMode.JAPANESE, onClick = { if (selectedMode != TtsMode.JAPANESE) { selectedMode = TtsMode.JAPANESE; reinitEngine(TtsMode.JAPANESE) } })
                                                    Text("日文", fontSize = 14.sp)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(selected = selectedMode == TtsMode.CHINESE_XIAO_YA, onClick = { if (selectedMode != TtsMode.CHINESE_XIAO_YA) { selectedMode = TtsMode.CHINESE_XIAO_YA; reinitEngine(TtsMode.CHINESE_XIAO_YA) } })
                                                    Text("中文-小雅", fontSize = 14.sp)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(selected = selectedMode == TtsMode.ENGLISH_LESSAC, onClick = { if (selectedMode != TtsMode.ENGLISH_LESSAC) { selectedMode = TtsMode.ENGLISH_LESSAC; reinitEngine(TtsMode.ENGLISH_LESSAC) } })
                                                    Text("English", fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }

                                    // ── Settings Card ───────────────────────────────────────
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Audio Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text("Speed: " + String.format("%.1f", TtsEngine.speed), fontSize = 14.sp)
                                            Slider(
                                                value = TtsEngine.speedState.value,
                                                onValueChange = { TtsEngine.speed = it; preferenceHelper.setSpeed(it) },
                                                valueRange = MIN_TTS_SPEED..MAX_TTS_SPEED,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            
                                            Text("Volume: ${currentVolume.toInt()}", fontSize = 14.sp)
                                            Slider(
                                                value = currentVolume,
                                                onValueChange = { currentVolume = it; audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, it.toInt(), 0) },
                                                valueRange = 0f..maxVolume,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            val numSpeakers = TtsEngine.tts?.numSpeakers() ?: 0
                                            if (numSpeakers > 1) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedTextField(
                                                    value = TtsEngine.speakerIdState.value.toString(),
                                                    onValueChange = { TtsEngine.speakerId = it.toIntOrNull() ?: 0; preferenceHelper.setSid(TtsEngine.speakerId) },
                                                    label = { Text("Speaker ID (0-${numSpeakers - 1})") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }

                                    var startEnabled by remember { mutableStateOf(true) }
                                    var playEnabled by remember { mutableStateOf(false) }
                                    var saveEnabled by remember { mutableStateOf(false) }
                                    var shareEnabled by remember { mutableStateOf(false) }
                                    var rtfText by remember { mutableStateOf("") }

                                    val saveLauncher = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.CreateDocument("audio/wav")
                                    ) { uri ->
                                        if (uri != null) {
                                            try {
                                                val srcFile = File(application.filesDir.absolutePath + "/generated.wav")
                                                contentResolver.openOutputStream(uri)?.use { output ->
                                                    srcFile.inputStream().use { input -> input.copyTo(output) }
                                                }
                                                Toast.makeText(applicationContext, "Audio saved", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Failed to save audio: $e")
                                                Toast.makeText(applicationContext, "Failed to save audio", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }

                                    // ── Input & Action Card ─────────────────────────────────
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Text Input", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            OutlinedTextField(
                                                value = testText,
                                                onValueChange = { testText = it },
                                                label = { Text("Type here to synthesize...") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp),
                                                singleLine = false
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // ── Action Buttons ─────────────────────────────────────────
                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    enabled = startEnabled && !isIniting,
                                                    onClick = {
                                                        if (testText.isBlank()) {
                                                            Toast.makeText(applicationContext, "Please input some text", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }
                                                        startEnabled = false
                                                        playEnabled = false
                                                        saveEnabled = false
                                                        shareEnabled = false
                                                        stopped = false
                                                        rtfText = ""

                                                        fun doGenerate() {
                                                            track?.pause(); track?.flush(); track?.play()
                                                            scope.launch {
                                                                for (samples in samplesChannel) {
                                                                    if (samples.isEmpty()) break
                                                                    track?.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                                                                    if (stopped) break
                                                                }
                                                                while (!samplesChannel.isEmpty) samplesChannel.tryReceive().getOrNull()
                                                            }
                                                            CoroutineScope(Dispatchers.Default).launch {
                                                                val timeSource = TimeSource.Monotonic
                                                                val startTime = timeSource.markNow()
                                                                val genConfig = GenerationConfig(sid = TtsEngine.speakerId, speed = TtsEngine.speed)
                                                                if (TtsEngine.isSupertonic) genConfig.extra = mapOf("lang" to TtsEngine.supertonicLang)

                                                                val audio = TtsEngine.tts!!.generateWithConfigAndCallback(
                                                                    text = testText, config = genConfig, callback = ::callback
                                                                )
                                                                val elapsed = startTime.elapsedNow().inWholeMilliseconds.toFloat() / 1000
                                                                val audioDuration = audio.samples.size / TtsEngine.tts!!.sampleRate().toFloat()
                                                                val RTF = String.format(
                                                                    "Threads: %d | Elapsed: %.3fs | Audio: %.3fs | RTF: %.3f",
                                                                    TtsEngine.tts!!.config.model.numThreads, elapsed, audioDuration, elapsed / audioDuration
                                                                )
                                                                scope.launch { samplesChannel.send(FloatArray(0)) }
                                                                val filename = application.filesDir.absolutePath + "/generated.wav"
                                                                val ok = audio.samples.isNotEmpty() && audio.save(filename)
                                                                if (ok) {
                                                                    withContext(Dispatchers.Main) {
                                                                        startEnabled = true; playEnabled = true
                                                                        saveEnabled = true; shareEnabled = true
                                                                        rtfText = RTF
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (TtsEngine.needsReinit(selectedMode)) {
                                                            reinitEngine(selectedMode) {
                                                                startEnabled = false
                                                                doGenerate()
                                                            }
                                                        } else {
                                                            doGenerate()
                                                        }
                                                    }
                                                ) { Text("Synthesize") }
                                                
                                                Button(
                                                    enabled = playEnabled,
                                                    onClick = { stopped = true; track?.pause(); track?.flush(); onClickPlay() }
                                                ) { Text("Play") }
                                                
                                                Button(
                                                    onClick = { onClickStop(); startEnabled = true }
                                                ) { Text("Stop") }
                                                
                                                Button(
                                                    enabled = saveEnabled,
                                                    onClick = { saveLauncher.launch("generated.wav") }
                                                ) { Text("Save") }
                                                
                                                Button(
                                                    enabled = shareEnabled,
                                                    onClick = {
                                                        val file = File(application.filesDir.absolutePath + "/generated.wav")
                                                        if (!file.exists()) {
                                                            Toast.makeText(applicationContext, "No audio to share", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            val uri = FileProvider.getUriForFile(context, "com.k2fsa.sherpa.onnx.tts.engine.fileprovider", file)
                                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                                type = "audio/wav"
                                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            startActivity(Intent.createChooser(intent, "Share audio"))
                                                        }
                                                    }
                                                ) { Text("Share") }
                                            }

                                            if (rtfText.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Performance Metrics", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                                                Text(rtfText, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                } // end UI content Column
                            } // end inner Box
                        }

                        // ── Progress overlay ───────────────────────────────────────────
                        if (isIniting) {
                            Dialog(
                                onDismissRequest = { /* Prevent dismissing while initializing */ },
                                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Column {
                                            Text(
                                                "Initializing Engine",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "Please wait...",
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        stopMediaPlayer()
        TtsEngine.releaseAll()
        super.onDestroy()
        kotlin.system.exitProcess(0)
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
    }

    private fun onClickPlay() {
        val filename = application.filesDir.absolutePath + "/generated.wav"
        stopMediaPlayer()
        mediaPlayer = MediaPlayer.create(applicationContext, Uri.fromFile(File(filename)))
        mediaPlayer?.start()
    }

    private fun onClickStop() {
        stopped = true
        track?.pause(); track?.flush()
        stopMediaPlayer()
    }

    private fun callback(samples: FloatArray): Int {
        return if (!stopped) {
            val copy = samples.copyOf()
            scope.launch { samplesChannel.trySend(copy) }
            1
        } else {
            track?.stop()
            0
        }
    }

    internal fun initAudioTrack() {
        track?.stop(); track?.release()
        val sampleRate = TtsEngine.tts!!.sampleRate()
        val bufLength = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val attr = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA).build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate).build()
        track = AudioTrack(attr, format, bufLength, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        track?.play()
    }
}
