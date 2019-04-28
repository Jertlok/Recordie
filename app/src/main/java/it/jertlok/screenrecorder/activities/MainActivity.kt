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
import android.provider.MediaStore
import android.util.Log
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.adapters.VideoAdapter
import it.jertlok.screenrecorder.common.ScreenRecorder
import it.jertlok.screenrecorder.common.ScreenVideo
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var mScreenRecorder: ScreenRecorder
    // MediaProjection API
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    // User interface
    private lateinit var bottomBar: BottomAppBar
    private lateinit var fabButton: FloatingActionButton
    // Video list
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mVideoAdapter: VideoAdapter
    private lateinit var mVideoArray: ArrayList<ScreenVideo>
    private lateinit var mLayoutManager: LinearLayoutManager
    // Drawables
    private var fabStartDrawable: Drawable? = null
    private var fabStopDrawable: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // User interface
        bottomBar = findViewById(R.id.bar)
        fabButton = findViewById(R.id.fab)
        mRecyclerView = findViewById(R.id.recycler_video_view)
        // Set video array
        mVideoArray = getVideos()
        mVideoAdapter = VideoAdapter(mVideoArray)
        mLayoutManager = LinearLayoutManager(applicationContext)

        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.adapter = mVideoAdapter

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
                moveTaskToBack(true)
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

        // TODO: This is actually interesting, we will see if I will keep it.
        // Set on scroll listener for recycler view so we can hide the fab button
        // when scrolling
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && !fabButton.isShown) {
                    fabButton.show()
                } else if (dy > 0 && fabButton.isShown){
                    fabButton.hide()
                }
            }
        })
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

    private fun getVideos(): ArrayList<ScreenVideo> {
        val projection = arrayOf(
                MediaStore.Video.Media.DATA, // index: 0
                MediaStore.Video.Media.TITLE, // index: 1
                MediaStore.Video.Media.DURATION, // index: 2
                MediaStore.Video.Media.DATE_TAKEN)
        // Set cursor
        val cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, MediaStore.Video.Media.DATA + " LIKE '%Screen Recorder/SCR%'",
                null,
                // Sort from newest to oldest
                MediaStore.Video.Media.DATE_TAKEN + " DESC")
        // Array List that we will return
        val arrayList = ArrayList<ScreenVideo>()
        // Go through list
        cursor?.apply {
            while (moveToNext()) {
                arrayList.add(ScreenVideo(
                        getString(/* DATA */ 0),
                        getString(/* TITLE */ 1),
                        getString(/* DURATION */ 2)))

                // Let's see what we can get.
                Log.d(TAG, "Data: " + getString(0) + " Duration: "
                        + getString(2))
            }
        }
        // Close the cursor
        cursor?.close()
        // Return array
        return arrayList
    }

    companion object {
        val TAG = "MainActivity"
        // Permission request code
        private const val PERMISSION_REQUESTS = 0
    }
}