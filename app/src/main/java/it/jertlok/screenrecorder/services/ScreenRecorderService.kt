package it.jertlok.screenrecorder.services

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

open class ScreenRecorderService : Service() {

    // Activity context
    private lateinit var mContext: Context
    // MediaProjection API
    private var mMediaProjection: MediaProjection? = null
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private var mMediaRecorder: MediaRecorder? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionCallback: MediaProjectionCallback? = null
    // Display metrics
    private lateinit var mDisplayMetrics: DisplayMetrics
    // Output file
    var mOutputFile: File? = null
        private set
    // Whether we are recording or not
    private var mIsRecording = false
    // SharedPreference
    private lateinit var mSharedPreferences: SharedPreferences
    // Service binder
    private var mBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        mContext = applicationContext
        // Get the media projection service
        mMediaProjectionManager = mContext.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // Get windowManager
        val windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // Get display metrics
        mDisplayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(mDisplayMetrics)
        // Instantiate media projection callbacks
        mMediaProjectionCallback = MediaProjectionCallback()
        // Get SharedPreference
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        // If we are requesting a start
        if (action == ACTION_START) {
            // Let's retrieve our parcelable
            val mediaPermission = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            startRecording(Activity.RESULT_OK, mediaPermission)
        } // Otherwise, let's stop.
        else if (action == ACTION_STOP) {
            stopRecording()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = mBinder

    private fun initRecorder() {
        // Conditional audio recording
        val isAudioRecEnabled = mSharedPreferences.getBoolean("audio_recording", false)
        // Initialise MediaRecorder
        mMediaRecorder = MediaRecorder()
        mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        if (isAudioRecEnabled) {
            mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mOutputFile = getOutputMediaFile()
        mMediaRecorder?.setOutputFile(mOutputFile?.path)
        // Set video size
        mMediaRecorder?.setVideoSize(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels)
        mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        val bitRate = mSharedPreferences.getString("bit_rate", "16384000")!!.toInt()
        mMediaRecorder?.setVideoEncodingBitRate(bitRate)
        if (isAudioRecEnabled) {
            mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mMediaRecorder?.setAudioEncodingBitRate(320 * 1000)
            mMediaRecorder?.setAudioSamplingRate(44100)
        }
        // Get user preference for frame rate
        val videoFrameRate = mSharedPreferences.getString("frame_rate", "30")!!.toInt()
        mMediaRecorder?.setVideoFrameRate(videoFrameRate)
        // Prepare MediaRecorder
        try {
            mMediaRecorder?.prepare()
        } catch (e: IOException) {
            Log.e(TAG, "prepare() failed")
        }
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
        mMediaRecorder?.start()
    }

    fun stopRecording() {
        // Stopping the media recorder could lead to crash, let us be safe.
        mIsRecording = false
        mMediaRecorder?.apply {
            stop()
            release()
        }
        mMediaRecorder = null
        // Stop screen sharing
        stopScreenSharing()
        // Destroy media projection session
        destroyMediaProjection()
        // Notify new media file
        notifyNewMedia()
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
        return mMediaProjection?.createVirtualDisplay(TAG, mDisplayMetrics.widthPixels,
                mDisplayMetrics.heightPixels, mDisplayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder?.surface, null, null)
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            mMediaRecorder?.apply {
                stop()
                release()
            }
            mMediaRecorder = null
            mMediaProjection = null
        }
    }

    private fun notifyNewMedia() {
        if (mOutputFile != null) {
            val contentUri = Uri.fromFile(mOutputFile)
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
            sendBroadcast(mediaScanIntent)
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
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())

        return File(mediaStorageDir.path + File.separator +
                "SCR_" + timeStamp + ".mp4")
    }

    fun isRecording(): Boolean {
        return mIsRecording
    }

    inner class LocalBinder : Binder() {
        fun getService(): ScreenRecorderService = this@ScreenRecorderService
    }

    companion object {
        private const val TAG = "ScreenRecorderService"
        // Request code for starting a screen record
        const val REQUEST_CODE_SCREEN_RECORD = 1
        // Intent actions
        const val ACTION_START = "it.jertlok.services.ScreenRecorderService.ACTION_START"
        const val ACTION_STOP = "it.jertlok.services.ScreenRecorderService.ACTION_STOP"
    }
}