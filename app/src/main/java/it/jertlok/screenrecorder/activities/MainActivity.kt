package it.jertlok.screenrecorder.activities

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.drawable.Drawable
import android.hardware.SensorManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.jertlok.screenrecorder.BuildConfig
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.adapters.VideoAdapter
import it.jertlok.screenrecorder.services.ScreenRecorderService
import it.jertlok.screenrecorder.common.ScreenVideo
import java.io.File
import java.lang.ref.WeakReference
import com.squareup.seismic.ShakeDetector

class MainActivity : AppCompatActivity(), ShakeDetector.Listener {

    // TODO: when removing an element there's no need to query once again
    // TODO: the content resolver, we can just remove the element directly
    // TODO: on the mVideoArray and then notify the adapter!

    private var mStoragePermissionGranted = false
    // MediaProjection API
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    // User interface
    private lateinit var bottomBar: BottomAppBar
    private lateinit var fabButton: FloatingActionButton
    private var mRecording = false
    // Video list
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mVideoAdapter: VideoAdapter
    private var mVideoArray = ArrayList<ScreenVideo>()
    private lateinit var mLayoutManager: LinearLayoutManager
    // Drawables
    private var fabStartDrawable: Drawable? = null
    private var fabStopDrawable: Drawable? = null
    // Content Observer
    private lateinit var mVideoContentObserver: VideoContentObserver
    // Notification manager
    private lateinit var mNotificationManager: NotificationManager
    // Shake detector
    private lateinit var mSensorManager: SensorManager
    private lateinit var mShakeDetector: ShakeDetector
    private var mIsShakeActive = false
    // Regex for updating video files
    private val mPattern = "content://media/external/video/media.*".toRegex()
    // Shared preference
    private lateinit var mSharedPreferences: SharedPreferences
    private lateinit var mSharedPrefListener: SharedPreferences.OnSharedPreferenceChangeListener
    // ScreenRecorderService
    private var mBound = false
    private lateinit var mBoundService: ScreenRecorderService

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mBoundService = (service as ScreenRecorderService.LocalBinder).getService()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Grant permissions if needed
        checkPermissions()

        // Get various system services
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mMediaProjectionManager = getSystemService(
                Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Register video content observer
        mVideoContentObserver = VideoContentObserver(Handler())
        contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true, mVideoContentObserver)

        // Initialise shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        // Set shared preference listener
        mSharedPrefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if ("shake_stop" == key) {
                // Get the new value for the preference
                mIsShakeActive = mSharedPreferences.getBoolean("shake_stop", false)
                // Our preference has changed, we also need to either start / stop the service
                if (mIsShakeActive) mShakeDetector.start(mSensorManager) else mShakeDetector.stop()
            }
        }
        // Register shared preference listener
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefListener)

        // Shake detector
        mIsShakeActive = mSharedPreferences.getBoolean("shake_stop", false)
        mShakeDetector = ShakeDetector(this)
        // Start ShakeDetector only if needed
        if (mIsShakeActive) {
            mShakeDetector.start(mSensorManager)
        }

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

        // Set actions for the FAB
        fabButton.setOnClickListener {
            // Here we need to understand whether we are recording or not.
            // If we are not recording we can send the intent for recording
            // otherwise we will try to stop the recording.
            if (!mBoundService.isRecording()) {
                // Start invisible activity
                startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
                        ScreenRecorderService.REQUEST_CODE_SCREEN_RECORD)
                mRecording = true
            } else {
                stopRecording()
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

    override fun onStart() {
        super.onStart()
        // Bind to ScreenRecorderService
        val intent = Intent(this, ScreenRecorderService::class.java)
        startService(intent)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        // TODO: create toggle method
        if (mRecording) {
            fabButton.setImageDrawable(fabStopDrawable)
        } else {
            fabButton.setImageDrawable(fabStartDrawable)
        }
        updateVideos()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister video content observer
        contentResolver.unregisterContentObserver(mVideoContentObserver)
        // Unregister shared preference listener
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener)
        // Stop shaking service if it's active
        mShakeDetector.stop()
    }

    override fun onStop() {
        super.onStop()
        unbindService(mConnection)
        mBound = false
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
                            PERMISSION_REQUESTS)
                }
            }
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

    private fun updateVideos() {
        if (!mStoragePermissionGranted) {
            return
        }
        // This task will update the video array in the background
        UpdateVideoTask(this).execute()
    }

    private fun stopRecording() {
        mBoundService.stopRecording()
        // Let's reset the FAB icon to start
        fabButton.setImageDrawable(fabStartDrawable)
        // Try to notify that we have created a new file
        notifyNewMedia(mBoundService.mOutputFile)
        mRecording = false
    }

    override fun hearShake() {
        if (mBoundService.isRecording()) {
            stopRecording()
            // In this case it would be great to return on the main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
    }

    private inner class EventInterfaceImpl : VideoAdapter.EventInterface {
        override fun deleteEvent(videoData: String) {
            // Do nothing
        }

        override fun playVideo(videoData: String) {
            val videoFile = File(videoData)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val uri = FileProvider.getUriForFile(this@MainActivity,
                    BuildConfig.APPLICATION_ID + ".provider", videoFile)
            intent.setDataAndType(uri, "video/mp4")
            startActivity(intent)
        }

        override fun shareVideo(videoData: String) {
            val videoFile = File(videoData)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "video/*"
            shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val fileToShare = FileProvider.getUriForFile(this@MainActivity,
                    BuildConfig.APPLICATION_ID + ".provider", videoFile)
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileToShare)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScreenRecorderService.REQUEST_CODE_SCREEN_RECORD) {
            if (resultCode != Activity.RESULT_OK) {
                // The user did not grant the permission
                Toast.makeText(this, getString(R.string.permission_cast_denied),
                        Toast.LENGTH_SHORT).show()
                // Terminate RecordingActivity, at this time we will still be
                // with the MainActivity on the Foreground.
                mRecording = false
                return
            }
            // Let's hide the main task and start a delayed shit
            moveTaskToBack(true)
            Handler().postDelayed({
                // Start screen recorder after 1.5 second
                mBoundService.startRecording(resultCode, data)
                mRecording = true
            }, 1500)
        }
    }

    private inner class VideoContentObserver(handler: Handler) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (mPattern.containsMatchIn(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())) {
                updateVideos()
            }
        }
    }

    private class UpdateVideoTask(context: MainActivity): AsyncTask<String, Void, Boolean>() {
        private val activityRef: WeakReference<MainActivity> = WeakReference(context)

        override fun doInBackground(vararg params: String?): Boolean {
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
        // Permission request code
        private const val PERMISSION_REQUESTS = 0
    }
}