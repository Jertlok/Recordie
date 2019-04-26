package it.jertlok.screenrecorder.common

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.util.Log
import java.io.File
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*

open class ScreenRecorder (context: Context) {

    companion object {
        const val TAG = "ScreenRecorder"
    }

    // Activity context
    private var mContext: Context = context
    // MediaProjection API
    private var mMediaProjection: MediaProjection? = null
    private var mMediaProjectionManager: MediaProjectionManager
    private var mMediaRecorder: MediaRecorder? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionCallback: MediaProjectionCallback
    // Display metrics
    private var mDisplayMetrics: DisplayMetrics
    // Output file
    var mOutputFile: File? = null
        private set
    // Whether we are recording or not
    private var mIsRecording = false

    // SharedPreference
    private var mSharedPreferences: SharedPreferences

    // User preferences
    private var mUserHeight: Int = 1
    private var mUserWidth: Int = -1

    init {
        // Get the media projection service
        mMediaProjectionManager = mContext.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // Get display metrics
        mDisplayMetrics = mContext.resources.displayMetrics
        // Instantiate media projection callbacks
        mMediaProjectionCallback = MediaProjectionCallback()
        // Get SharedPreference
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
    }

    private fun initRecorder() {
        // TODO: This will probably have to check for the shared preferences.
        mMediaRecorder = MediaRecorder()
        mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mOutputFile = getOutputMediaFile()
        mMediaRecorder?.setOutputFile(mOutputFile?.path)

        // Get user preference for video resolution
        val videoResolution = mSharedPreferences.getString("video_resolution",
                "1920x1080")?.split("x".toRegex())
        mUserWidth = videoResolution?.get(1)?.toInt() as Int
        mUserHeight = videoResolution[0].toInt()
        // Set video resolution with user preferences
        mMediaRecorder?.setVideoSize(mUserWidth, mUserHeight)
        mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder?.setVideoEncodingBitRate(16384 * 1000)
        // Get user preference for frame rate
        val videoFrameRate = mSharedPreferences.getString("frame_rate",
                "30")?.toInt() as Int
        mMediaRecorder?.setVideoFrameRate(videoFrameRate)
        // Conditional audio recording
        //val audioRecording
        //if ()
        mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        // Prepare MediaRecorder
        mMediaRecorder?.prepare()
    }

    fun startRecording(resultCode: Int, data: Intent?) {
        // TODO: Improve user experience
        if (mIsRecording) {
            return
        }
        Log.d(TAG, "startRecording()")
        mIsRecording = true
        // TODO: try to figure the warning on data
        // Initialise MediaProjection
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data)
        mMediaProjection?.registerCallback(mMediaProjectionCallback, null)
        // Init recorder
        initRecorder()
        // Create virtual display
        mVirtualDisplay = createVirtualDisplay()
        // Start recording
        try {
            mMediaRecorder?.start()
        } catch (e: RuntimeException) {
            Log.e(TAG, e.toString())
        }
    }

    fun stopRecording() {
        // Stopping the media recorder could lead to crash, let us be safe.
        mIsRecording = false
        try {
            mMediaRecorder?.stop()
        } catch (e: RuntimeException) {
            Log.d(TAG, e.toString())
        }
        mMediaRecorder?.reset()
        // Stop screen sharing
        stopScreenSharing()
        // Destroy media projection session
        destroyMediaProjection()
    }

    private fun stopScreenSharing() {
        // We don't have a virtual display anymore
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay?.release()
    }

    private fun destroyMediaProjection() {
        if (mMediaProjection != null) {
            Log.d(TAG, "destroyMediaProjection()")
            mMediaProjection?.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection?.stop()
            mMediaProjection = null
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mMediaProjection?.createVirtualDisplay(TAG, mUserWidth, mUserHeight,
                mDisplayMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder?.surface, null, null)
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            try {
                mMediaRecorder?.stop()
            } catch (e: RuntimeException) {
                Log.d(TAG, e.toString())
            }
            mMediaRecorder?.reset()
            mMediaProjection = null
        }
    }

    private fun getOutputMediaFile(): File? {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED,
                        ignoreCase = true)) {
            return null
        }
        // Create folder app
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "Screen Recorder")

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory")
                return null
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())

        return File(mediaStorageDir.path + File.separator +
                "VID_" + timeStamp + ".mp4")
    }

    fun isRecording(): Boolean {
        return mIsRecording
    }

    fun getOutputFileName(): String? {
        return mOutputFile?.path
    }
}