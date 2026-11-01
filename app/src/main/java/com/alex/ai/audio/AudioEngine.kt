package com.alex.ai.audio

import android.annotation.SuppressLint
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sqrt

interface AudioEngineListener {
    fun onMicData(chunk: ByteArray)
    fun onMicAmplitude(amplitude: Float)
    fun onSpeakerAmplitude(amplitude: Float)
}

class AudioEngine(
    private val listener: AudioEngineListener
) {
    private val tag = "AudioEngine"

    // 16kHz 16-bit Mono PCM for input to Gemini
    private val inputSampleRate = 16000
    private val inputChannelConfig = AudioFormat.CHANNEL_IN_MONO
    private val inputAudioFormat = AudioFormat.ENCODING_PCM_16BIT

    // 24kHz 16-bit Mono PCM for output from Gemini
    private val outputSampleRate = 24000
    private val outputChannelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val outputAudioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    private var isRecording = false
    private var isPlaying = false

    private val playbackQueue = ConcurrentLinkedQueue<ByteArray>()
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Start the microphone capture pipeline with hardware Echo Guard
     */
    @SuppressLint("MissingPermission")
    fun startRecording(enableEchoGuard: Boolean = true, enableNoiseSuppressor: Boolean = true) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            inputSampleRate,
            inputChannelConfig,
            inputAudioFormat
        )
        // 100ms chunk size: (16000 samples/sec * 2 bytes/sample * 0.1 sec) = 3200 bytes
        val chunkSize = 3200
        val bufferSize = maxOf(minBufferSize, chunkSize * 2)

            var record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                inputSampleRate,
                inputChannelConfig,
                inputAudioFormat,
                bufferSize
            )

            // Fallback to standard MIC if VOICE_COMMUNICATION is not supported on device
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(tag, "VOICE_COMMUNICATION not supported, falling back to MIC source")
                record.release()
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    inputSampleRate,
                    inputChannelConfig,
                    inputAudioFormat,
                    bufferSize
                )
            }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord failed to initialize")
                record.release()
                return
            }

            audioRecord = record
            val audioSessionId = record.audioSessionId

            // Hardware Acoustic Echo Guard
            try {
                if (enableEchoGuard && AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply {
                        enabled = true
                        Log.d(tag, "Acoustic Echo Canceler enabled")
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "AcousticEchoCanceler init warning: ${e.message}")
            }

            // Hardware Noise Suppressor
            try {
                if (enableNoiseSuppressor && NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply {
                        enabled = true
                        Log.d(tag, "Noise Suppressor enabled")
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "NoiseSuppressor init warning: ${e.message}")
            }

            record.startRecording()
            isRecording = true

            recordingJob = audioScope.launch {
                val buffer = ByteArray(chunkSize)
                while (isRecording && isActive) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        val chunk = buffer.copyOf(readBytes)
                        listener.onMicData(chunk)

                        // Calculate RMS Amplitude
                        val amplitude = calculateRms(chunk)
                        listener.onMicAmplitude(amplitude)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start AudioRecord", e)
            stopRecording()
        }
    }

    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping AudioRecord", e)
        }
        audioRecord = null

        echoCanceler?.release()
        echoCanceler = null
        noiseSuppressor?.release()
        noiseSuppressor = null
    }

    /**
     * Start Low-Latency AudioTrack playback pipeline
     */
    fun startPlayback() {
        if (audioTrack != null) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            outputSampleRate,
            outputChannelConfig,
            outputAudioFormat
        )

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(outputSampleRate)
            .setChannelMask(outputChannelConfig)
            .setEncoding(outputAudioFormat)
            .build()

        audioTrack = AudioTrack(
            audioAttributes,
            format,
            minBufferSize * 2,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack?.play()
        isPlaying = true

        playbackJob = audioScope.launch {
            while (isPlaying && isActive) {
                val chunk = playbackQueue.poll()
                if (chunk != null) {
                    audioTrack?.write(chunk, 0, chunk.size)
                    val amplitude = calculateRms(chunk)
                    listener.onSpeakerAmplitude(amplitude)
                } else {
                    delay(10)
                }
            }
        }
    }

    /**
     * Enqueue incoming 24kHz PCM chunk from Gemini
     */
    fun enqueuePlaybackChunk(pcmBytes: ByteArray) {
        playbackQueue.offer(pcmBytes)
        if (!isPlaying || audioTrack == null) {
            startPlayback()
        }
    }

    /**
     * Instant Barge-In: Flushes audio buffer and drops queued chunks immediately
     */
    fun stopPlayback() {
        playbackQueue.clear()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.e(tag, "Error flushing AudioTrack", e)
        }
        listener.onSpeakerAmplitude(0f)
    }

    fun release() {
        stopRecording()
        stopPlayback()
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing AudioTrack", e)
        }
        audioTrack = null
    }

    private fun calculateRms(pcmBytes: ByteArray): Float {
        var sum = 0.0
        val sampleCount = pcmBytes.size / 2
        if (sampleCount == 0) return 0f

        for (i in 0 until sampleCount) {
            val low = pcmBytes[i * 2].toInt()
            val high = pcmBytes[i * 2 + 1].toInt()
            val sample = (high shl 8) or (low and 0xFF)
            sum += sample * sample
        }

        val rms = sqrt(sum / sampleCount)
        // Normalize 16-bit PCM (max 32767) to 0.0 - 1.0 range
        return (rms / 32767.0).toFloat().coerceIn(0f, 1f)
    }
}
