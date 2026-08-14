package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler

class AudioRecorder(private val onAudioReady: (ByteArray, Int) -> Unit) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordThread: Thread? = null
    private var aec: AcousticEchoCanceler? = null

    @SuppressLint("MissingPermission")
    fun start() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 4
        )

        if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
            val sessionId = audioRecord?.audioSessionId ?: -1
            if (sessionId != -1 && AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(sessionId)
                aec?.enabled = true
            }

            audioRecord?.startRecording()
            isRecording = true

            recordThread = Thread {
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        onAudioReady(buffer, read)
                    }
                }
            }
            recordThread?.start()
        }
    }

    fun stop() {
        isRecording = false
        try {
            recordThread?.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            aec?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        aec = null
        try {
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }
}
