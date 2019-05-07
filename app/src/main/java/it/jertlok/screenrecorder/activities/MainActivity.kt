package it.jertlok.screenrecorder.activities

import android.Manifest
import android.app.NotificationManager
import android.app.UiModeManager
import android.content.*
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.drawable.Drawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
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
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    // TODO: when removing an element there's no need to query once again
    // TODO: the content resolver, we can just remove the element directly
    // TODO: on the mVideoArray and then notify the adapter!

    private var mStoragePermissionGranted = false
    // MediaProjection API
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    // User interface
    private lateinit var bottomBar: BottomAppBar
    private lateinit var fabButton: FloatingActionButton
    // Video list
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mVideoAdapter: VideoAdapter
    private var mVideoArray = ArrayList<ScreenVideo>()
    private var mVideoArrayUpdate = ArrayList<ScreenVideo>()
    private lateinit var mLayoutManager: LinearLayoutManager
    // Drawables
    private var fabStartDrawable: Drawable? = null
    private var fabStopDrawable: Drawable? = null
    // Content Observer
    private lateinit var mVideoContentObserver: VideoContentObserver
    // Regex for updating video files
    private val mPattern = "content://media/external/video/media.*".toRegex()
    // Notification manager
    private lateinit var mNotificationManager: NotificationManager
    // Shared preference
    private lateinit var mSharedPreferences: SharedPreferences
    // Broadcast receiver for updating FAB button from service
    private val mBroadcastReceiver = LocalBroadcastReceiver()
    private val mIntentFilter = IntentFilter()
    // UiModeManager
    private lateinit var mUiModeManager: UiModeManager
    // ScreenRecorderService
    private var mBound = false
    private lateinit var mBoundService: ScreenRecorderService
    // Service connection
    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mBoundService = (service as ScreenRecorderService.LocalBinder).getService()
            mBound = true
            // Conditional FAB update
            conditionalFabToggle()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme
        mUiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        setUiTheme()

        // Set contents
        setContentView(R.layout.activity_main)

        // Grant permissions if needed
        checkPermissions()

        // TODO: move to android manifest asap
        // Set the various intent filters
        mIntentFilter.addAction(ACTION_DELETE_VIDEO)
        mIntentFilter.addAction(ACTION_UPDATE_FAB)

        // Get various system services
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mMediaProjectionManager = getSystemService(
                Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Register video content observer
        mVideoContentObserver = VideoContentObserver(Handler())
        contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true, mVideoContentObserver)

        // Initialise shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // User interface
        bottomBar = findViewById(R.id.bar)
        fabButton = findViewById(R.id.fab)
        mRecyclerView = findViewById(R.id.recycler_video_view)

        // Set adapter
        mVideoAdapter = VideoAdapter(mVideoArray, EventInterfaceImpl())

        mLayoutManager = GridLayoutManager(applicationContext, 2)

        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.adapter = mVideoAdapter

        // Drawables
        fabStartDrawable = getDrawable(R.drawable.ic_record)
        fabStopDrawable = getDrawable(R.drawable.ic_stop)

        // Set actions for the FAB
        fabButton.setOnClickListener {
            // Here we need to understand whether we are recording or not.
            // If we are not recording we can send the intent for recording
            // otherwise we will try to stop the recording.
            if (!mBoundService.isRecording() && !mBoundService.mRecScheduled) {
                // Start invisible activity
                val startIntent = Intent(this, RecordingActivity::class.java)
                        .setAction(RecordingActivity.ACTION_START)
                startActivity(startIntent)
            } else if (mBoundService.isRecording()) {
                stopRecording()
            }
        }

        // TODO: this is indeed temporary, just for development purposes
        // TODO: and also because I am being lazy in regards of the UI for now.
        // Set something for menu
        bottomBar.setNavigationOnClickListener {
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
        // Register broadcast receiver
        registerReceiver(mBroadcastReceiver, mIntentFilter)
        // Remove all the app notifications
        mNotificationManager.cancel(ScreenRecorderService.NOTIFICATION_RECORD_FINAL_ID)
        // Update videos
        updateVideos()
    }

    override fun onStop() {
        super.onStop()
        unbindService(mConnection)
        mBound = false
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister video content observer
        contentResolver.unregisterContentObserver(mVideoContentObserver)
    }

    override fun onPause() {
        // Unregister broadcast receiver
        unregisterReceiver(mBroadcastReceiver)
        super.onPause()
    }

    // TODO: investigate on app being killed for some reasons.
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun setUiTheme() {
        when (mUiModeManager.nightMode) {
            UiModeManager.MODE_NIGHT_AUTO or UiModeManager.MODE_NIGHT_NO -> {
                setTheme(R.style.AppTheme)
                whiteHelper()
            }
            UiModeManager.MODE_NIGHT_YES -> setTheme(R.style.AppTheme_Dark)
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

    /** Function for retrieving all the video list */
    private fun updateVideos() {
        if (!mStoragePermissionGranted) {
            return
        }
        // This task will update the video array in the background
        UpdateVideosTask(this).execute()
    }

    /** Function for retrieving the last element from the content resolver */
    private fun updateLastVideo() {
        if (!mStoragePermissionGranted) {
            return
        }
        // This task will update the video array in the background
        UpdateSingleVideoTask(this).execute()
    }

    private fun updateDelete(videoData: String) {
        val position = mVideoArray.indexOf(mVideoArray.find { s -> s.data == videoData})
        mVideoArray.removeAt(position)
        mVideoAdapter.notifyItemRemoved(position)
    }

    private fun stopRecording() {
        mBoundService.stopRecording()
        // Let's reset the FAB icon to start
        fabButton.setImageDrawable(fabStartDrawable)
    }

    private inner class EventInterfaceImpl : VideoAdapter.EventInterface {
        override fun deleteEvent(videoData: String) {
            updateDelete(videoData)
            if (mBoundService.mOutputFile?.path == videoData) {
                // It means we are deleting the last recorded video, hence we can remove its notif.
                mNotificationManager.cancel(ScreenRecorderService.NOTIFICATION_RECORD_FINAL_ID)
            }
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

    private inner class VideoContentObserver(handler: Handler) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // On new file added
            if (mPattern.containsMatchIn(uri.toString())) {
                updateLastVideo()
            }
        }
    }

    private class UpdateSingleVideoTask(context: MainActivity): AsyncTask<Void, Void, Boolean>() {
        private val activityRef: WeakReference<MainActivity> = WeakReference(context)

        override fun doInBackground(vararg params: Void): Boolean {
            val activity = activityRef.get()
            if (activity == null || activity.isFinishing || params.size > 1) {
                return false
            }
            val contentResolver = activity.contentResolver
            // The columns we need to retrieve
            val projection = arrayOf(
                    MediaStore.Video.Media.DATA, // index: 0
                    MediaStore.Video.Media.TITLE, // index: 1
                    MediaStore.Video.Media.DURATION, // index: 2
                    MediaStore.Video.Media.DATE_TAKEN)
            // Let's try to do the query
            val cursor = contentResolver?.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        MediaStore.Video.Media.DATA + " LIKE '%Screen Recorder/SCR%'",
                        null, null)
            // Try to get the element
            cursor?.apply {
                // Workaround: Marshmallow contentResolver doesn't distinguish between media URIs
                if (moveToLast()) {
                    activity.mVideoArray.add(0, ScreenVideo(
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
            // Notify that the data has changed.
            activity.mVideoAdapter.notifyItemInserted(0)
            // Do not scroll ffs.
            activity.mRecyclerView.smoothScrollToPosition(0)
        }
    }

    private class UpdateVideosTask(context: MainActivity): AsyncTask<Void, Void, Boolean>() {
        private val activityRef: WeakReference<MainActivity> = WeakReference(context)

        override fun doInBackground(vararg params: Void): Boolean {
            val activity = activityRef.get()
            if (activity == null || activity.isFinishing) {
                return false
            }
            val contentResolver = activity.contentResolver
            // Clear array
            activity.mVideoArrayUpdate.clear()
            val projection = arrayOf(
                    MediaStore.Video.Media.DATA, // index: 0
                    MediaStore.Video.Media.TITLE, // index: 1
                    MediaStore.Video.Media.DURATION, // index: 2
                    MediaStore.Video.Media.DATE_TAKEN)
            // Set cursor
            val cursor = contentResolver?.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Video.Media.DATA + " LIKE '%Screen Recorder/SCR%'",
                    null,
                    // Sort from newest to oldest
                    MediaStore.Video.Media.DATE_TAKEN + " DESC")
            // Go through list
            cursor?.apply {
                while (moveToNext()) {
                    activity.mVideoArrayUpdate.add(ScreenVideo(
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
            // Notify that the data has changed.
            if (activity.mVideoArray.size != activity.mVideoArrayUpdate.size) {
                // Clear the main array
                activity.mVideoArray.clear()
                // Add the elements from the update array
                activity.mVideoArray.addAll(activity.mVideoArrayUpdate)
                activity.mVideoAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun conditionalFabToggle() {
        if (mBoundService.isRecording()) {
            fabButton.setImageDrawable(fabStopDrawable)
        } else {
            fabButton.setImageDrawable(fabStartDrawable)
        }
    }

    private inner class LocalBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_UPDATE_FAB -> conditionalFabToggle()
                ACTION_DELETE_VIDEO -> {
                    // Let's get the fileUri from the intent
                    val videoData = intent.getStringExtra(ScreenRecorderService.SCREEN_RECORD_URI)
                    updateDelete(videoData)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        // Permission request code
        const val PERMISSION_REQUESTS = 0
        // Intent filter
        const val ACTION_UPDATE_FAB = "it.jertlok.activities.MainActivity.ACTION_UPDATE_FAB"
        const val ACTION_DELETE_VIDEO = "it.jertlok.activities.MainActivity.ACTION_DELETE_VIDEO"
    }
}