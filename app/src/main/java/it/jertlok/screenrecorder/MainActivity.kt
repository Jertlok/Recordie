package it.jertlok.screenrecorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = "MainActivity"
        private const val REQUEST_CODE_SCREEN_RECORD = 1
    }

    private lateinit var mScreenRecorder: ScreenRecorder
    // MediaProjection API
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    // User interface
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Instantiate Screen Recorder class
        mScreenRecorder = ScreenRecorder(applicationContext)

        // TODO: Make this variable local if I realise it's not needed elsewhere.
        mMediaProjectionManager = applicationContext.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // User interface
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        // Set on click listener for starting the recording
        startButton.setOnClickListener {
            // Request to user the screen cast permission
            val recorderIntent = mMediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(recorderIntent, REQUEST_CODE_SCREEN_RECORD)
        }
        // Set on click listener for stopping the recording
        stopButton.setOnClickListener {
            // Stop recording
            mScreenRecorder.stopRecording()
            // TODO: Make this check a little bit more reliable
            // If the file got created
            if (mScreenRecorder.mOutputFile != null) {
                val contentUri = Uri.fromFile(mScreenRecorder.mOutputFile)
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
                sendBroadcast(mediaScanIntent)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO: Create some toasts?
        if (requestCode == REQUEST_CODE_SCREEN_RECORD) {
            if (resultCode != Activity.RESULT_OK) {
                // The user did not grant the permission
                return
            }
            // Otherwise we can start the screen record
            mScreenRecorder.startRecording(resultCode, data)
        }
    }
}
