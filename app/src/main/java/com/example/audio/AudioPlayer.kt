package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class AudioPlayer {
    private var audioTrack: AudioTrack? = null
    private val queue = LinkedBlockingQueue<ByteArray>()
    private var isPlaying = false
    private var playThread: Thread? = null

    fun start() {
        val bufferSize = AudioTrack.getMinBufferSize(
            24000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(24000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(bufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        isPlaying = true

        playThread = Thread {
            while (isPlaying) {
                try {
                    val chunk = queue.poll(100, TimeUnit.MILLISECONDS)
                    if (chunk != null && isPlaying) {
                        audioTrack?.write(chunk, 0, chunk.size)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        playThread?.start()
    }

    fun playChunk(data: ByteArray) {
        queue.offer(data)
    }

    fun clear() {
        queue.clear()
        audioTrack?.flush()
    }

    fun stop() {
        isPlaying = false
        try {
            playThread?.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack?.stop()
            }
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
        queue.clear()
    }
}
