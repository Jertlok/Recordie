package it.jertlok.screenrecorder.services

import android.app.*
import android.content.*
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.squareup.seismic.ShakeDetector
import it.jertlok.screenrecorder.BuildConfig
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.activities.MainActivity
import it.jertlok.screenrecorder.tasks.DeleteVideoTask
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

open class ScreenRecorderService : Service(), ShakeDetector.Listener {

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
    private lateinit var mSharedPrefListener: SharedPreferences.OnSharedPreferenceChangeListener
    // Service binder
    private var mBinder = LocalBinder()
    // Notification
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mRecNotificationChannel: NotificationChannel
    private lateinit var mFinalNotificationChannel: NotificationChannel
    // Shake detector
    private lateinit var mSensorManager: SensorManager
    private lateinit var mShakeDetector: ShakeDetector
    private var mIsShakeActive = false
    // Broadcast receiver
    private var mBroadcastReceiver = LocalBroadcastReceiver()
    // Screen off stop
    private var mIsScreenStopActive = false
    // Some way to handle a scheduled recording
    private var mRecDelay = 2
    var mRecScheduled = false
        private set
    private var mHandler = Handler()
    // Vibration
    private lateinit var mVibrator: Vibrator

    override fun onCreate() {
        super.onCreate()
        mContext = applicationContext
        // Get vibrator service
        mVibrator = mContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // Get the media projection service
        mMediaProjectionManager = mContext.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
        // Get windowManager
        val windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Get display metrics
        mDisplayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(mDisplayMetrics)
        // Instantiate media projection callbacks
        mMediaProjectionCallback = MediaProjectionCallback()
        // Get SharedPreference
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        // Notification
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Notification channel for the recording progress
            mRecNotificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_REC_ID,
                NOTIFICATION_CHANNEL_PROGRESS_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            mNotificationManager.createNotificationChannel(mRecNotificationChannel)
            // Notification channel for completed screen records
            mFinalNotificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_FINAL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            // Default ringtone
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            mFinalNotificationChannel.vibrationPattern = longArrayOf(300, 300, 300)
            mFinalNotificationChannel.setSound(defaultSoundUri, audioAttributes)
            // Create notification channels
            mNotificationManager.createNotificationChannel(mRecNotificationChannel)
            mNotificationManager.createNotificationChannel(mFinalNotificationChannel)
        }
        // Set shared preference listener
        mSharedPrefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "shake_stop" -> {
                    // Get the new value for the preference
                    mIsShakeActive = mSharedPreferences.getBoolean("shake_stop", false)
                    // Our preference has changed, we also need to either start / stop the service
                    if (mIsShakeActive) mShakeDetector.start(mSensorManager) else mShakeDetector.stop()
                }
                "screen_off_stop" -> mIsScreenStopActive = mSharedPreferences.getBoolean("screen_off_stop", false)
                "rec_delay" -> mRecDelay = mSharedPreferences.getInt("rec_delay", 2)
            }
        }
        // Register shared preference listener
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefListener)
        // Initialise shake detector
        mIsShakeActive = mSharedPreferences.getBoolean("shake_stop", false)
        mShakeDetector = ShakeDetector(this)
        // Initialisation for screen off feature
        mIsScreenStopActive = mSharedPreferences.getBoolean("screen_off_stop", false)
        // Broadcast receiver
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(ACTION_DELETE)
        registerReceiver(mBroadcastReceiver, intentFilter)
        // Delay for recording
        mRecDelay = mSharedPreferences.getInt("rec_delay", 2)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If we are requesting a start
        when (intent?.action) {
            ACTION_START -> {
                mRecScheduled = true
                // Let's retrieve our parcelable
                val mediaPermission = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                // Start ShakeDetector if active
                if (mIsShakeActive) mShakeDetector.start(mSensorManager)
                // Start recording after user preference
                mHandler.postDelayed({
                    startRecording(mediaPermission)
                    createNotification()
                }, (mRecDelay * 1000).toLong())
                return START_STICKY
            }
            ACTION_STOP -> {
                stopRecording()
                // Stop shake detector
                mShakeDetector.stop()
            }
            ACTION_STOP_DELAYED -> {
                // Block everything
                mHandler.removeCallbacksAndMessages(null)
                mRecScheduled = false
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = mBinder

    override fun onDestroy() {
        super.onDestroy()
        // Unregister shared preference listener
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener)
        // Stop shaking service if it's active
        mShakeDetector.stop()
        // Unregister broadcast receiver
        unregisterReceiver(mBroadcastReceiver)
    }

    private fun initRecorder() {
        mOutputFile = getOutputMediaFile()
        if (mOutputFile == null) {
            Log.e(TAG, "Failed to get the file.")
            return
        }
        // Conditional audio recording
        val isAudioRecEnabled = mSharedPreferences.getBoolean("audio_recording", false)
        // Initialise MediaRecorder
        mMediaRecorder = MediaRecorder()
        mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        if (isAudioRecEnabled) {
            mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
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

    private fun startRecording(data: Intent) {
        // TODO: Improve user experience
        if (mIsRecording) {
            return
        }
        // We have started the recording, no more pending recordings.
        mRecScheduled = false
        // We are recording
        mIsRecording = true
        // Remove standard notifications
        mNotificationManager.cancel(NOTIFICATION_RECORD_FINAL_ID)
        // Initialise MediaProjection
        mMediaProjection = mMediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data)
        mMediaProjection?.registerCallback(mMediaProjectionCallback, null)
        // Init recorder
        initRecorder()
        // Create virtual display
        mVirtualDisplay = createVirtualDisplay()
        // Start recording
        mMediaRecorder?.start()
        // Send broadcasts for recording status
        recStatusBroadcast()
        // Update QS Tile status
        toggleQS(true)
    }

    private fun toggleQS(state: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Construct broadcast
            val broadcast = Intent()
            if (state) {
                broadcast.action = RecordQSTileService.ACTION_ENABLE_QS
            } else {
                broadcast.action = RecordQSTileService.ACTION_DISABLE_QS
            }
            // Send broadcast
            Handler().postDelayed({
                sendBroadcast(broadcast)
            }, 250)
        }
    }

    // Implement shake listener
    override fun hearShake() = stopRecording()

    fun stopRecording() {
        // If we are not recording there's no need to get into all these actions
        if (!mIsRecording) {
            return
        }
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
        updateMedia(Uri.fromFile(mOutputFile))
        // Stop notification
        stopForeground(true)
        // Stop shake service, we activate it after we start the recording for saving battery
        mShakeDetector.stop()
        // Send broadcast for recording status
        recStatusBroadcast()
        // Create notification
        createFinalNotification()
        // Toggle QS
        toggleQS(false)
    }

    private fun recStatusBroadcast() {
        val fabBroadcast = Intent(MainActivity.ACTION_UPDATE_FAB)
        // Lord, forgive for what I am going to do here.
        // The MainActivity onResume is slow at registering the BroadcastReceiver,
        // let's set a small postDelayed call.
        Handler().postDelayed({
            sendBroadcast(fabBroadcast)
        }, 250)
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
        return mMediaProjection?.createVirtualDisplay(
            TAG, mDisplayMetrics.widthPixels,
            mDisplayMetrics.heightPixels, mDisplayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder?.surface, null, null
        )
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

    private fun updateMedia(uri: Uri) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
        sendBroadcast(mediaScanIntent)
    }

    /** Notification for the recording progress with stop action */
    private fun createNotification() {
        // Build the various actions
        val intent = Intent(this, ScreenRecorderService::class.java)
            .setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Open MainActivity action
        val mainIntent = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val mainAction = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Build notification
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_REC_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_rec_progress))
            .setWhen(System.currentTimeMillis())
            .setUsesChronometer(true)
            .addAction(
                R.drawable.ic_stop, getString(R.string.notif_rec_stop),
                stopPendingIntent
            )
            .setContentIntent(mainAction)
            .build()
        // Start foreground service
        startForeground(NOTIFICATION_RECORD_ID, builder)
    }

    /** Final notification after the recording is complete with media actions */
    private fun createFinalNotification() {
        // Build the various actions
        val shareIntent = Intent(Intent.ACTION_SEND)
            .setType("video/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val fileUri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".provider", mOutputFile!!
        )
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
        val shareAction = PendingIntent.getActivity(
            this, 0,
            Intent.createChooser(shareIntent, getString(R.string.share_title)),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Delete action
        val deleteIntent = Intent(ACTION_DELETE)
            .putExtra(SCREEN_RECORD_URI, Uri.fromFile(mOutputFile))
        val deleteAction = PendingIntent.getBroadcast(
            this, 0,
            deleteIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_CANCEL_CURRENT
        )
        // Open MainActivity action
        val mainIntent = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val mainAction = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Build notification
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_FINAL_ID)
            .setTicker(getString(R.string.notif_rec_complete))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_rec_complete))
            .setContentText("Video")
            .setWhen(System.currentTimeMillis())
            .addAction(R.drawable.ic_delete, "Delete", deleteAction)
            .addAction(R.drawable.ic_share, "Share", shareAction)
            .setContentIntent(mainAction)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setVibrate(LongArray(0))
            .build()
        // Send notification
        mNotificationManager.notify(NOTIFICATION_RECORD_FINAL_ID, builder)
    }

    /** Get an output file for the recording process */
    @Nullable
    private fun getOutputMediaFile(): File? {
        // Check if media is mounted
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED,
                ignoreCase = true
            )
        ) {
            return null
        }
        // Create folder app
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
            ), "Screen Recorder"
        )

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory")
                return null
            }
        }
        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        // Try to open a new file
        var file: File? = null
        try {
            file = File(
                mediaStorageDir.path + File.separator +
                        "SCR_" + timeStamp + ".mp4"
            )
        } catch (e: IOException) {
            Log.d(TAG, "Failed to create file, error: $e")
        }
        return file
    }

    /** Whether the recording is in progress or not */
    fun isRecording(): Boolean = mIsRecording

    inner class LocalBinder : Binder() {
        fun getService(): ScreenRecorderService = this@ScreenRecorderService
    }

    private inner class LocalBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> if (mIsScreenStopActive) stopRecording()
                ACTION_DELETE -> {
                    mNotificationManager.cancel(NOTIFICATION_RECORD_FINAL_ID)
                    val fileUri = intent.getParcelableExtra<Uri>(SCREEN_RECORD_URI)
                    DeleteVideoTask(this@ScreenRecorderService).execute(fileUri)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ScreenRecorderService"
        // Request code for starting a screen record
        const val REQUEST_CODE_SCREEN_RECORD = 1
        // Notification constants
        private const val NOTIFICATION_CHANNEL_PROGRESS_NAME = "Recording progress"
        private const val NOTIFICATION_CHANNEL_NAME = "Recording notifications"
        private const val NOTIFICATION_CHANNEL_REC_ID =
            "it.jertlok.services.ScreenRecorderService.Recording"
        const val NOTIFICATION_RECORD_ID = 1
        // Channel for notifications only
        private const val NOTIFICATION_CHANNEL_FINAL_ID =
            "it.jertlok.services.ScreenRecorderService.RecordingNotification"
        const val NOTIFICATION_RECORD_FINAL_ID = 2
        // Intent actions
        const val ACTION_START = "it.jertlok.services.ScreenRecorderService.ACTION_START"
        const val ACTION_STOP = "it.jertlok.services.ScreenRecorderService.ACTION_STOP"
        const val ACTION_STOP_DELAYED =
            "it.jertlok.services.ScreenRecorderService.ACTION_STOP_DELAYED"
        const val ACTION_DELETE = "it.jertlok.services.ScreenRecorderService.ACTION_DELETE"
        const val SCREEN_RECORD_URI = "screen_record_file_uri"
    }
}