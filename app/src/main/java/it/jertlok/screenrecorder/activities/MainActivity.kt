package it.jertlok.screenrecorder.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.common.ScreenRecorder
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var mScreenRecorder: ScreenRecorder
    // MediaProjection API
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    // User interface
    private lateinit var bottomBar: BottomAppBar
    private lateinit var fabButton: FloatingActionButton
    // Drawables
    private var fabStartDrawable: Drawable? = null
    private var fabStopDrawable: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // User interface
        bottomBar = findViewById(R.id.bar)
        fabButton = findViewById(R.id.fab)
        // Drawables
        fabStartDrawable = getDrawable(R.drawable.ic_outline_record)
        fabStopDrawable = getDrawable(R.drawable.ic_outline_stop)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    // Show the explanation
                } else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO),
                            PERMISSION_REQUESTS)
                }
            }
        }

        // Instantiate Screen Recorder class
        mScreenRecorder = ScreenRecorder.getInstance(applicationContext)

        // TODO: Make this variable local if I realise it's not needed elsewhere.
        mMediaProjectionManager = applicationContext.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Set actions for the FAB
        fabButton.setOnClickListener {
            // Here we need to understand whether we are recording or not.
            // If we are not recording we can send the intent for recording
            // otherwise we will try to stop the recording.
            if (!mScreenRecorder.isRecording()) {
                // Start invisible activity
                startActivity(Intent(this, RecordingActivity::class.java))
                // Terminate MainActivity
                finish()
            } else {
                mScreenRecorder.stopRecording()
                // Let's reset the FAB icon to start
                fabButton.setImageDrawable(fabStartDrawable)
                // Try to notify that we have created a new file
                notifyNewMedia(mScreenRecorder.mOutputFile)
            }
        }

        // TODO: this is indeed temporary, just for development purposes
        // TODO: and also because I am being lazy in regards of the UI for now.
        // Set something for menu
        bottomBar.setOnClickListener {
            // Start Settings activity
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mScreenRecorder.isRecording()) {
            fabButton.setImageDrawable(fabStopDrawable)
        }
    }

    // TODO: I hate to repeat my self, check @link{RecordingActivity} - we have
    // TODO: the same method down there.
    private fun notifyNewMedia(file: File?) {
        // TODO: make a better check
        if (file != null) {
            val contentUri = Uri.fromFile(file)
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
            sendBroadcast(mediaScanIntent)
        }
    }

    companion object {
        val TAG = "MainActivity"
        // Permission request code
        private const val PERMISSION_REQUESTS = 0
    }
}