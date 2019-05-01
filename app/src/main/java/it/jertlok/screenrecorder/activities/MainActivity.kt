package it.jertlok.screenrecorder.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.drawable.Drawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.jertlok.screenrecorder.BuildConfig
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.adapters.VideoAdapter
import it.jertlok.screenrecorder.common.ScreenRecorder
import it.jertlok.screenrecorder.common.ScreenVideo
import java.io.File
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    // TODO: when removing an element there's no need to query once again
    // TODO: the content resolver, we can just remove the element directly
    // TODO: on the mVideoArray and then notify the adapter!

    private var mStoragePermissionGranted = false
    private lateinit var mScreenRecorder: ScreenRecorder
    // MediaProjection API
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    // User interface
    private lateinit var bottomBar: BottomAppBar
    private lateinit var fabButton: FloatingActionButton
    // Video list
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mVideoAdapter: VideoAdapter
    private var mVideoArray = ArrayList<ScreenVideo>()
    private lateinit var mLayoutManager: LinearLayoutManager
    // Drawables
    private var fabStartDrawable: Drawable? = null
    private var fabStopDrawable: Drawable? = null

    // Content Observer
    private lateinit var mContentObserver: VideoContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Grant permissions if needed
        checkPermissions()

        contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true, VideoContentObserver(Handler()))

        // User interface
        bottomBar = findViewById(R.id.bar)
        fabButton = findViewById(R.id.fab)
        mRecyclerView = findViewById(R.id.recycler_video_view)

        // Set adapter
        mVideoAdapter = VideoAdapter(mVideoArray, EventInterfaceImpl())
        mLayoutManager = LinearLayoutManager(applicationContext)

        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.adapter = mVideoAdapter

        // Update videos
        updateVideos()

        // Drawables
        fabStartDrawable = getDrawable(R.drawable.ic_outline_record)
        fabStopDrawable = getDrawable(R.drawable.ic_outline_stop)

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
        }
    }

    override fun onResume() {
        super.onResume()
        if (mScreenRecorder.isRecording()) {
            fabButton.setImageDrawable(fabStopDrawable)
        }
        updateVideos()
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(mContentObserver)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // If we got the the WRITE_EXTERNAL_STORAGE permission granted
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Let's set the global variable
            mStoragePermissionGranted = true
            updateVideos()
        }
    }

    private fun checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
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
                            PERMISSION_REQUESTS)
                }
            }
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

    private fun updateVideos() {
        if (!mStoragePermissionGranted) {
            return
        }
        // This task will update the video array in the background
        UpdateVideoTask(this).execute()
    }

    private inner class EventInterfaceImpl : VideoAdapter.EventInterface {
        override fun deleteEvent() {
            // TODO: add delete task
            updateVideos()
        }

        override fun playVideo(videoData: String) {
            val videoFile = File(videoData)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val uri = FileProvider.getUriForFile(this@MainActivity,
                    BuildConfig.APPLICATION_ID + ".provider", videoFile)
            intent.setDataAndType(uri, "video/mpeg")
            startActivity(intent)
        }
    }

    private inner class VideoContentObserver(handler: Handler) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // Update video adapter when something changes in the content database
            if (uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI) {
                updateVideos()
            }
        }
    }

    private class UpdateVideoTask(context: MainActivity): AsyncTask<Void, Void, Boolean>() {
        private val activityRef: WeakReference<MainActivity> = WeakReference(context)

        override fun doInBackground(vararg params: Void?): Boolean {
            val activity = activityRef.get()
            if (activity == null || activity.isFinishing) {
                return false
            }

            val contentResolver = activity.contentResolver
            // Clear array
            activity.mVideoArray.clear()
            val projection = arrayOf(
                    MediaStore.Video.Media.DATA, // index: 0
                    MediaStore.Video.Media.TITLE, // index: 1
                    MediaStore.Video.Media.DURATION, // index: 2
                    MediaStore.Video.Media.DATE_TAKEN)
            // Set cursor
            val cursor = contentResolver?.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection, MediaStore.Video.Media.DATA + " LIKE '%Screen Recorder/SCR%'",
                    null,
                    // Sort from newest to oldest
                    MediaStore.Video.Media.DATE_TAKEN + " DESC")
            // Go through list
            cursor?.apply {
                while (moveToNext()) {
                    activity.mVideoArray.add(ScreenVideo(
                            getString(/* DATA */ 0),
                            getString(/* TITLE */ 1),
                            getString(/* DURATION */ 2)))
                }
            }
            // Close the cursor
            cursor?.close()
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            val activity = activityRef.get()
            if (activity == null || activity.isFinishing) {
                return
            }
            // Notify that the data has changed
            activity.mVideoAdapter.notifyDataSetChanged()
        }

    }

    companion object {
        private const val TAG = "MainActivity"
        // Permission request code
        private const val PERMISSION_REQUESTS = 0
    }
}