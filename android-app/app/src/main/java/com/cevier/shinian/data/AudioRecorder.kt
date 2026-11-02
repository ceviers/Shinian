package com.cevier.shinian.data

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    fun start() {
        cancel()
        val directory = context.cacheDir.resolve("recordings").apply { mkdirs() }
        val file = File.createTempFile("recording-", ".m4a", directory)
        val mediaRecorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        output = file
        recorder = mediaRecorder
    }

    fun stop(): File? {
        val file = output
        val stopped = runCatching { recorder?.stop() }.isSuccess
        recorder?.release()
        recorder = null
        output = null
        if (!stopped) file?.delete()
        return file?.takeIf { stopped && it.exists() && it.length() > 0L }
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        output?.delete()
        output = null
    }
}
