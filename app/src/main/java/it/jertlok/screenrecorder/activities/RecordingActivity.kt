package it.jertlok.screenrecorder.activities

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.services.ScreenRecorderService

open class RecordingActivity: AppCompatActivity() {

    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mSharedPreferences: SharedPreferences
    private lateinit var mUiModeManager: UiModeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme
        mUiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        setUiTheme()

        // Initialise shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val action = intent.action

        if(action == ACTION_START) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                    ScreenRecorderService.REQUEST_CODE_SCREEN_RECORD)
        } else {
            // We need to stop the recording
            val stopIntent = Intent(this, ScreenRecorderService::class.java)
                    .setAction(ScreenRecorderService.ACTION_STOP)
            startService(stopIntent)
            finish()
        }
    }

    private fun setUiTheme() {
        when (mUiModeManager.nightMode) {
            UiModeManager.MODE_NIGHT_AUTO -> {
                setTheme(R.style.InvisibleActivity)
                whiteHelper()
            }
            UiModeManager.MODE_NIGHT_YES -> setTheme(R.style.InvisibleActivity_Dark)
            UiModeManager.MODE_NIGHT_NO -> whiteHelper()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.decorView.systemUiVisibility = baseFlags or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

            }
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
    }
}