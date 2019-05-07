package it.jertlok.screenrecorder.activities

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.services.ScreenRecorderService

open class RecordingActivity: AppCompatActivity() {

    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mSharedPreferences: SharedPreferences
    private lateinit var mUiModeManager: UiModeManager
    // Permissions
    private var mStoragePermissionGranted = false
    // Workaround
    private var mAction: String? = null
    // ScreenRecorderService
    private var mBound = false
    private lateinit var mBoundService: ScreenRecorderService
    private val mConnection = object : ServiceConnection {
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
        mBound = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set theme
        mUiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        setUiTheme()

        // Initialise shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Store action that is going to be used when the activity
        // gets bounded to the service.
        mAction = intent.action
    }

    private fun startRecording() {
        when (intent.action) {
            ACTION_START -> {
                startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
                        ScreenRecorderService.REQUEST_CODE_SCREEN_RECORD)
            }
            ACTION_QS_START -> {
                checkPermissions()
                if (!mStoragePermissionGranted) {
                    // Get the f... out.
                } else if (!mBoundService.isRecording() && !mBoundService.mRecScheduled) {
                    startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
                            ScreenRecorderService.REQUEST_CODE_SCREEN_RECORD)
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


    private fun setUiTheme() {
        when (mUiModeManager.nightMode) {
            UiModeManager.MODE_NIGHT_AUTO or UiModeManager.MODE_NIGHT_NO -> {
                setTheme(R.style.InvisibleActivity)
                whiteHelper()
            }
            UiModeManager.MODE_NIGHT_YES -> setTheme(R.style.InvisibleActivity_Dark)
        }
    }

    private fun whiteHelper() {
        // TODO: move to when or something easier to read
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val baseFlags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            // Marshmallow conditions
            window.decorView.systemUiVisibility = baseFlags
            // If it's higher than O we need to add something else
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                window.decorView.systemUiVisibility = baseFlags or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // We need to know if at least the storage permission got granted, it will be useful
            // for allowing to update videos asynchronously.
            mStoragePermissionGranted = checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            // Check permissions
            if (!mStoragePermissionGranted || checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    // Show the explanation
                } else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO),
                            MainActivity.PERMISSION_REQUESTS)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // If we got the the WRITE_EXTERNAL_STORAGE permission granted
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Let's set the global variable
            mStoragePermissionGranted = true
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
                    ScreenRecorderService.REQUEST_CODE_SCREEN_RECORD)
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
                Toast.makeText(this, getString(R.string.permission_cast_denied),
                        Toast.LENGTH_SHORT).show()
                // Terminate RecordingActivity, at this time we will still be
                // with the MainActivity on the Foreground.
                finish()
                return
            }
            // Start screen recorder after the user preference
            // Encapsulate media permission
            val startIntent = Intent(this, ScreenRecorderService::class.java)
                    .setAction(ScreenRecorderService.ACTION_START)
            startIntent.putExtra(Intent.EXTRA_INTENT, data)
            startService(startIntent)
            // Terminate activity
            finish()
        }
    }

    companion object {
        private const val TAG = "RecordingActivity"
        // Intent actions
        const val ACTION_START = "it.jertlok.activities.RecordingActivity.ACTION_START"
        const val ACTION_QS_START = "it.jertlok.activities.RecordingActivity.ACTION_QS_START"
    }
}