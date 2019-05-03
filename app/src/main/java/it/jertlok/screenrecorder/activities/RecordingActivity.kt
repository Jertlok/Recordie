package it.jertlok.screenrecorder.activities

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.services.ScreenRecorderService
import java.io.File


open class RecordingActivity: AppCompatActivity() {

    private lateinit var mNotificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            // Let's hide the main task and start a delayed shit
            moveTaskToBack(true)
            // Start screen recorder after 1.5 second
            Handler().postDelayed({
                // Encapsulate media permission
                val startIntent = Intent(this, ScreenRecorderService::class.java)
                        .setAction(ScreenRecorderService.ACTION_START)
                startIntent.putExtra(Intent.EXTRA_INTENT, data)
                startService(startIntent)
            }, 1500)
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