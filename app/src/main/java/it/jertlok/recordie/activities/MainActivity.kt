/*
 *     This file is part of Recordie.
 *
 *     Recordie is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Recordie is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Recordie.  If not, see <https://www.gnu.org/licenses/>.
 */

package it.jertlok.recordie.activities

import android.Manifest
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import it.jertlok.recordie.BuildConfig
import it.jertlok.recordie.R
import it.jertlok.recordie.adapters.VideoAdapter
import it.jertlok.recordie.common.ScreenVideo
import it.jertlok.recordie.common.SdkHelper
import it.jertlok.recordie.interfaces.AdapterInterface
import it.jertlok.recordie.services.ScreenRecorderService
import it.jertlok.recordie.tasks.UpdateSingleVideoTask
import it.jertlok.recordie.tasks.UpdateVideosTask
import it.jertlok.recordie.utils.ThemeHelper
import it.jertlok.recordie.utils.Utils
import it.jertlok.recordie.views.VideoRecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private var mDarkOverride = false
    private var mStoragePermissionGranted = false
    // MediaProjection API
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    // User interface
    private lateinit var bottomBar: BottomAppBar
    private lateinit var fabButton: FloatingActionButton
    private lateinit var mCoordinatorLayout: CoordinatorLayout
    private lateinit var mNavigationView: NavigationView
    private lateinit var bottomDrawer: View
    private lateinit var bottomBehaviour: BottomSheetBehavior<View>
    // Video list
    lateinit var mRecyclerView: VideoRecyclerView
    lateinit var mVideoAdapter: VideoAdapter
    var mVideoArray = ArrayList<ScreenVideo>()
    var mVideoArrayUpdate = ArrayList<ScreenVideo>()
    private lateinit var mLayoutManager: LinearLayoutManager
    // Drawables
    private var fabStartDrawable: Drawable? = null
    private var fabStopDrawable: Drawable? = null
    // Notification manager
    private lateinit var mNotificationManager: NotificationManager
    // Shared preference
    private lateinit var mSharedPreferences: SharedPreferences
    // Broadcast receiver for updating FAB button from service
    private val mBroadcastReceiver = LocalBroadcastReceiver()
    private val mIntentFilter = IntentFilter()
    // ScreenRecorderService
    private var mBound = false
    private lateinit var mBoundService: ScreenRecorderService
    // Service connection
    private val mConnection = LocalServiceConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set theme
        ThemeHelper.setTheme(this, R.style.AppTheme, R.style.AppTheme_Dark)

        // Set contents
        setContentView(R.layout.activity_main)

        // User interface
        mCoordinatorLayout = findViewById(R.id.coordinator_layout)
        bottomBar = findViewById(R.id.bar)
        fabButton = findViewById(R.id.fab)
        mRecyclerView = findViewById(R.id.recycler_video_view)
        mNavigationView = findViewById(R.id.navigation_view)

        // Drawables
        fabStartDrawable = getDrawable(R.drawable.ic_record)
        fabStopDrawable = getDrawable(R.drawable.ic_stop)

        setUpBottomDrawer()

        // Grant permissions if needed
        checkPermissions()

        // Get various system services
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Initialise shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        mDarkOverride = mSharedPreferences.getBoolean("dark_mode", false)

        // Setup RecyclerView
        mVideoAdapter = VideoAdapter(mVideoArray, AdapterInterfaceImpl())
        mLayoutManager = GridLayoutManager(applicationContext, 2)
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.adapter = mVideoAdapter
        mRecyclerView.mEmptyView = findViewById(R.id.empty)

        // TODO: move to android manifest asap
        // Set the various intent filters
        mIntentFilter.addAction(ACTION_DELETE_VIDEO)
        mIntentFilter.addAction(ACTION_UPDATE_FAB)
    }

    /** Dismiss the BottomSheet when clicking outside of its boundaries */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            // If our bottom sheet is expanded
            if (bottomBehaviour.state == BottomSheetBehavior.STATE_EXPANDED) {
                // Get its layout
                val outRect = Rect()
                mNavigationView.getGlobalVisibleRect(outRect)
                // If we are touching outside of the rect, dismiss it
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt()))
                    bottomBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /** Function for setting up all the bottom area */
    private fun setUpBottomDrawer() {
        bottomDrawer = mCoordinatorLayout.findViewById(R.id.bottom_drawer)
        bottomBehaviour = BottomSheetBehavior.from(bottomDrawer)
        bottomBehaviour.state = BottomSheetBehavior.STATE_HIDDEN

        // When clicking on the menu bottom
        bottomBar.setNavigationOnClickListener {
            bottomBehaviour.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }

        // Bottom sheet
        mNavigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Extra actions on the right for interacting with cards
        bottomBar.setOnMenuItemClickListener { i ->
            when (i.itemId) {
                R.id.delete -> {
                    val builder = MaterialAlertDialogBuilder(this@MainActivity)
                    // Set positive button
                    builder.setTitle(R.string.delete_dialog_title)
                    builder.setPositiveButton(R.string.delete) { _, _ ->
                        Utils.deleteFiles(contentResolver, mVideoAdapter.selectedItems)
                        updateMultiDelete()
                        // Clean up selected items
                        cleanUpSelection()
                        // Update menu
                        updateMenuItems()
                    }
                    // Set negative button
                    builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    // Show the dialog
                    builder.show()
                    true
                }
                R.id.share -> {
                    updateMenuItems()
                    if (mVideoAdapter.selectedItems.size == 1)
                        shareVideoFromSelection()
                    else
                        shareVideosFromSelection()
                    // Update menu
                    updateMenuItems()
                    true
                }
                else -> {
                    false
                }
            }
        }

        // Set FAB button behaviour
        fabButton.setOnClickListener {
            // Here we need to understand whether we are recording or not.
            // If we are not recording we can send the intent for recording
            // otherwise we will try to stop the recording.
            if (!mBoundService.isRecording() && !mBoundService.mRecScheduled) {
                startRecording()
            } else if (mBoundService.isRecording()) {
                stopRecording()
            }
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
        // Is storage permission granted on the meantime?
        ensureStoragePermission()
        // Restore bottom behaviour
        bottomBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
        // Check for dark theme override and eventually set new mode
        darkThemeCheck()
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
    }

    override fun onPause() {
        // Unregister broadcast receiver
        unregisterReceiver(mBroadcastReceiver)
        super.onPause()
    }

    // TODO: investigate on app being killed for some reasons.
    override fun onBackPressed() {
        // Handle bottom sheet behaviour on back press
        if (bottomBehaviour.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
            return
        }
        // Handle selected cards on back press
        if (mVideoAdapter.selectedItems.size > 0) {
            // Remove all the selected items
            cleanUpSelection()
            // Disable the menu items on the bottom right
            updateMenuItems()
        } else {
            moveTaskToBack(true)
        }
    }

    private fun cleanUpSelection() {
        // TODO: Not sure if this is the best way...
        mVideoAdapter.selectedItems.clear()
        mVideoAdapter.selectedHolder.forEach { h ->
            h.card.isChecked = false
        }
    }

    private fun startRecording() {
        val startIntent = Intent(this, RecordingActivity::class.java)
            .setAction(RecordingActivity.ACTION_START)
        startActivity(startIntent)
    }

    private fun ensureStoragePermission() {
        if (SdkHelper.atleastM())
            mStoragePermissionGranted =
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    /** Small function for recreating activity in case of dark mode toggle change */
    private fun darkThemeCheck() {
        // If dark theme override has changed we need to recreate
        val currentDarkOverride = mSharedPreferences.getBoolean("dark_mode", false)
        if (mDarkOverride != currentDarkOverride) {
            mDarkOverride = currentDarkOverride
            recreate()
        }
    }

    private fun conditionalFabToggle() {
        if (mBoundService.isRecording()) {
            fabButton.setImageDrawable(fabStopDrawable)
        } else {
            fabButton.setImageDrawable(fabStartDrawable)
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
        UpdateSingleVideoTask(this).execute(mBoundService.mOutputFile?.path)
    }

    private fun updateMultiDelete() {
        mVideoAdapter.selectedItems.forEach { v ->
            mVideoArray.remove(v)
            removeNotificationIfNeeded(v.data)
        }
        mVideoAdapter.notifyDataSetChanged()
    }

    private fun updateDelete(videoData: String) {
        // Get the index for the video to be removed according to the criteria
        val i = mVideoArray.indexOf(mVideoArray.find { s -> s.data == videoData })
        // Remove element
        try {
            mVideoArray.removeAt(i)
            mVideoAdapter.notifyItemRemoved(i)
        } catch (e: ArrayIndexOutOfBoundsException) {
            // Do nothing
        }
        removeNotificationIfNeeded(videoData)
    }

    private fun removeNotificationIfNeeded(videoData: String) {
        if (mBoundService.mOutputFile?.path == videoData) {
            // It means we are deleting the last recorded video, hence we can remove its notification.
            mNotificationManager.cancel(ScreenRecorderService.NOTIFICATION_RECORD_FINAL_ID)
        }
    }

    private fun shareVideoFromSelection() {
        val videoFile = File(mVideoAdapter.selectedItems[0].data)
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"
        shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val fileToShare = FileProvider.getUriForFile(
            this@MainActivity,
            BuildConfig.APPLICATION_ID + ".provider", videoFile
        )
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileToShare)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
    }

    private fun shareVideosFromSelection() {
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        shareIntent.type = "video/*"
        shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        // Build array list of URIs
        val files = ArrayList<Uri>()
        for (video in mVideoAdapter.selectedItems) {
            files.add(
                FileProvider.getUriForFile(
                    this@MainActivity,
                    BuildConfig.APPLICATION_ID + ".provider", File(video.data)
                )
            )
        }
        // Send files!
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.multiple_share_title)))
    }

    private fun stopRecording() {
        mBoundService.stopRecording()
        // Let's reset the FAB icon to start
        fabButton.setImageDrawable(fabStartDrawable)
    }

    private fun checkPermissions() {
        if (SdkHelper.atleastM()) {
            // Check storage permission
            mStoragePermissionGranted = checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!mStoragePermissionGranted) {
                MaterialAlertDialogBuilder(this).apply {
                    setTitle(R.string.storage_permission_title)
                    setMessage(R.string.storage_permission_desc)
                    setFinishOnTouchOutside(false)
                    setCancelable(false)
                    setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ),
                            PERMISSION_REQUESTS
                        )
                    }
                }.show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // If we got the the WRITE_EXTERNAL_STORAGE permission granted
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Let's set the global variable
            mStoragePermissionGranted = true
            updateVideos()
        } else {
            // TODO create a friendly SnackBar giving the user another chance to request the permissions.
            Toast.makeText(this, getString(R.string.permission_storage_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private inner class AdapterInterfaceImpl : AdapterInterface {
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
            val uri = FileProvider.getUriForFile(
                this@MainActivity,
                BuildConfig.APPLICATION_ID + ".provider", videoFile
            )
            intent.setDataAndType(uri, "video/mp4")
            startActivity(intent)
        }

        override fun shareVideo(videoData: String) {
            val videoFile = File(videoData)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "video/*"
            shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val fileToShare = FileProvider.getUriForFile(
                this@MainActivity,
                BuildConfig.APPLICATION_ID + ".provider", videoFile
            )
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileToShare)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
        }

        override fun updateCardCheck() {
            updateMenuItems()
        }
    }

    fun updateMenuItems() {
        val deleteAction = bottomBar.menu.getItem(0)
        val shareAction = bottomBar.menu.getItem(1)
        deleteAction.isVisible = mVideoAdapter.selectedItems.size >= 1
        shareAction.isVisible = mVideoAdapter.selectedItems.size >= 1
    }

    private inner class LocalBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_UPDATE_FAB -> {
                    conditionalFabToggle()
                    updateLastVideo()
                }
                ACTION_DELETE_VIDEO -> {
                    // Let's get the fileUri from the intent
                    val videoData = intent.getStringExtra(ScreenRecorderService.SCREEN_RECORD_URI)
                    updateDelete(videoData)
                }
            }
        }
    }

    private inner class LocalServiceConnection : ServiceConnection {
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

    companion object {
        // Permission request code
        const val PERMISSION_REQUESTS = 0
        // Intent filter
        const val ACTION_UPDATE_FAB = "it.jertlok.activities.MainActivity.ACTION_UPDATE_FAB"
        const val ACTION_DELETE_VIDEO = "it.jertlok.activities.MainActivity.ACTION_DELETE_VIDEO"
    }
}