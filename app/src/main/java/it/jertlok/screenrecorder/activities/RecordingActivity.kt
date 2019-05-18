package it.jertlok.screenrecorder.activities

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.common.SdkHelper
import it.jertlok.screenrecorder.services.ScreenRecorderService
import it.jertlok.screenrecorder.utils.ThemeHelper

open class RecordingActivity : AppCompatActivity() {

    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mSharedPreferences: SharedPreferences
    // Workaround
    private var mAction: String? = null
    // ScreenRecorderService
    private var mBound = false
    private lateinit var mBoundService: ScreenRecorderService
    private val mConnection = LocalServiceConnection()
    // Make permission checker smarter
    private var mIncludesAudio = false
    private var mDrawOverlay = false

    override fun onStart() {
        super.onStart()
        // Bind to ScreenRecorderService
        val intent = Intent(this, ScreenRecorderService::class.java)
        startService(intent)
        bindService(intent, mConnection, Context.BIND_IMPORTANT)
    }

    override fun onStop() {
        super.onStop()
        unbindService(mConnection)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme
        ThemeHelper.setTheme(this, R.style.InvisibleActivity, R.style.InvisibleActivity_Dark)

        // Initialise shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        mIncludesAudio = mSharedPreferences.getBoolean("audio_recording", false)
        // Check if we can overlay
        if (SdkHelper.atleastM())
            mDrawOverlay = (mSharedPreferences.getInt("rec_schedule", 3) > 0)
                .and(Settings.canDrawOverlays(applicationContext))

        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Store action that is going to be used when the activity
        // gets bounded to the service.
        mAction = intent.action
    }

    private fun startRecording() {
        when (intent.action) {
            ACTION_START -> checkPermissionsAndStart()
            ACTION_QS_START -> {
                if (!mBoundService.isRecording() && !mBoundService.mRecScheduled) {
                    checkPermissionsAndStart()
                } else {
                    // Finish immediately, it allows us to reuse some code for the launcher shortcut.
                    // TODO: make the launcher shortcut dynamic...
                    finish()
                }
            }
            else -> {
                // We need to stop the recording
                val stopIntent = Intent(this, ScreenRecorderService::class.java)
                    .setAction(ScreenRecorderService.ACTION_STOP)
                startService(stopIntent)
                finish()
            }
        }
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check permissions
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                // Set basic permission
                val permissions = arrayListOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                // Add audio only if needed
                if (mIncludesAudio) permissions.add(Manifest.permission.RECORD_AUDIO)
                // Request permissions
                requestPermissions(permissions.toTypedArray(), MainActivity.PERMISSION_REQUESTS)
            } else {
                createScreenCapturePermission()
            }
        }
    }

    private fun createScreenCapturePermission() {
        startActivityForResult(
            mMediaProjectionManager.createScreenCaptureIntent(),
            ScreenRecorderService.REQUEST_CODE_SCREEN_RECORD
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // If we got the the WRITE_EXTERNAL_STORAGE permission granted
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && !mIncludesAudio) {
            createScreenCapturePermission()
        } else if (mIncludesAudio && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED
        ) {
            createScreenCapturePermission()
        } else {
            Toast.makeText(this, getString(R.string.permission_usages_denied), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /* Here we will check whether we got the casting permission and eventually
     * start the recording.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScreenRecorderService.REQUEST_CODE_SCREEN_RECORD) {
            if (resultCode != Activity.RESULT_OK) {
                // The user did not grant the permission
                Toast.makeText(
                    this, getString(R.string.permission_cast_denied),
                    Toast.LENGTH_SHORT
                ).show()
                // Terminate RecordingActivity, at this time we will still be
                // with the MainActivity on the Foreground.
                finish()
                return
            }
            // Start screen recorder after the user preference
            // Encapsulate media permission
            val startIntent = Intent(this, ScreenRecorderService::class.java).apply {
                action = ScreenRecorderService.ACTION_START
                putExtra(Intent.EXTRA_INTENT, data)
            }
            startService(startIntent)
            // Terminate activity
            finish()
        }
    }

    private inner class LocalServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mBoundService = (service as ScreenRecorderService.LocalBinder).getService()
            mBound = true
            // Serve the action
            startRecording()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mBound = false
        }
    }

    companion object {
        // Intent actions
        const val ACTION_START = "it.jertlok.activities.RecordingActivity.ACTION_START"
        const val ACTION_QS_START = "it.jertlok.activities.RecordingActivity.ACTION_QS_START"
    }
}