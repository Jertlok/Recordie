package it.jertlok.screenrecorder.tasks

import android.os.AsyncTask
import android.provider.MediaStore
import it.jertlok.screenrecorder.activities.MainActivity
import it.jertlok.screenrecorder.common.ScreenVideo
import java.lang.ref.WeakReference

class UpdateSingleVideoTask(context: MainActivity) : AsyncTask<Void, Void, Boolean>() {
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
            MediaStore.Video.Media.DATE_TAKEN
        )
        // Let's try to do the query
        val cursor = contentResolver?.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            MediaStore.Video.Media.DATA + " LIKE '%Screen Recorder/SCR%'",
            null, null
        )
        // Try to get the element
        cursor?.apply {
            // Workaround: Marshmallow contentResolver doesn't distinguish between media URIs
            if (moveToLast()) {
                activity.mVideoArray.add(
                    0, ScreenVideo(
                        getString(/* DATA */ 0),
                        getString(/* TITLE */ 1)/*,
                    getString(/* DURATION */ 2)*/
                    )
                )
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