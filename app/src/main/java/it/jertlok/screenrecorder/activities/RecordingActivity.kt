package it.jertlok.screenrecorder.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScreenRecorder.REQUEST_CODE_SCREEN_RECORD) {
            if (resultCode != Activity.RESULT_OK) {
                // The user did not grant the permission
                Toast.makeText(this, getString(R.string.permission_cast_denied),
                        Toast.LENGTH_SHORT).show()
                // We will now reopen the main activity
                val openMainActivity = Intent(this, MainActivity::class.java)
                openMainActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(openMainActivity)
                // Terminate RecordingActivity and get out of here!
                finish()
                return
            }
            // Otherwise we can start the screen record
            mScreenRecorder.startRecording(resultCode, data)
            // Terminate activity
            finish()
        }
    }

    private fun notifyNewMedia(file: File?) {
        // TODO: make a better check
        if (file != null) {
            val contentUri = Uri.fromFile(file)
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
            sendBroadcast(mediaScanIntent)
        }
    }
}