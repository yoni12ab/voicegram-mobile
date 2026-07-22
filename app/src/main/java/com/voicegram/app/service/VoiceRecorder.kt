package com.voicegram.app.service

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class VoiceRecorder(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    
    fun startRecording(onRecordingStarted: (File) -> Unit) {
        try {
            audioFile = File(context.getExternalFilesDir(null), "voice_recording_${System.currentTimeMillis()}.wav")
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            audioFile?.let { onRecordingStarted(it) }
            
        } catch (e: Exception) {
            e.printStackTrace()
            @Suppress("UNUSED_EXPRESSION")
            stopRecording { null }
        }
    }
    
    fun stopRecording(onRecordingStopped: (File?) -> Unit) {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            
            isRecording = false
            onRecordingStopped(audioFile)
            
        } catch (e: Exception) {
            e.printStackTrace()
            onRecordingStopped(null)
        } finally {
            mediaRecorder = null
        }
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun getAudioFile(): File? = audioFile
}