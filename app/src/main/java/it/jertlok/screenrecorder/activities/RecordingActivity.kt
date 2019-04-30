package it.jertlok.screenrecorder.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.common.ScreenRecorder
import java.io.File

open class RecordingActivity: AppCompatActivity() {

    private lateinit var mScreenRecorder: ScreenRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        mScreenRecorder = ScreenRecorder.getInstance(applicationContext)
        if(!mScreenRecorder.isRecording()) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                    ScreenRecorder.REQUEST_CODE_SCREEN_RECORD)
        } else {
            // We need to stop the recording
            mScreenRecorder.stopRecording()
            notifyNewMedia(mScreenRecorder.mOutputFile)
            finish()
        }
    }

    /* Here we will check whether we got the casting permission and eventually
     * start the recording.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScreenRecorder.REQUEST_CODE_SCREEN_RECORD) {
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
            Handler().postDelayed({
                // Start screen recorder after 1.5 second
                mScreenRecorder.startRecording(resultCode, data)
            }, 1500)
            // Terminate activity
            finish()
        }
    }

    /**
     * Function that sends a Broadcast and triggers a media scan with
     * a file URI, so it will be added down gallery apps.
     *
     * @param file: the file that will be used for extracting the URI
     */
    private fun notifyNewMedia(file: File?) {
        // TODO: make a better check
        if (file != null) {
            val contentUri = Uri.fromFile(file)
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
            sendBroadcast(mediaScanIntent)
        }
    }

    companion object {
        private const val TAG = "RecordingActivity"
    }
}