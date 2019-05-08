package it.jertlok.screenrecorder.tasks

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.provider.MediaStore
import it.jertlok.screenrecorder.activities.MainActivity
import it.jertlok.screenrecorder.services.ScreenRecorderService
import java.io.File
import java.lang.ref.WeakReference

// TODO: find a way for making this work for both Services and Activities

/** Asynchronous task for deleting a video in the background */
class DeleteVideoTask(context: Service) : AsyncTask<Uri, Void, Boolean>() {
    private val activityRef: WeakReference<Service> = WeakReference(context)
    private lateinit var mFileUri: String
    override fun doInBackground(vararg params: Uri): Boolean {
        val activity = activityRef.get()
        if (params.size != 1 || activity == null) {
            return false
        }
        // Get file uri
        mFileUri = params[0].path!!
        val contentResolver = activity.contentResolver
        // The file we need to remove
        val where = "${MediaStore.Video.Media.DATA} = '$mFileUri'"
        // The resulting rows, that in our case must be a single value
        val rows = contentResolver.delete(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            where, null
        )
        // If we find the file inside our content resolver
        if (rows != 0) {
            // Let's try to remove the file
            try {
                val file = File(mFileUri)
                if (file.delete()) {
                    return true
                }
            } catch (e: Exception) {
                // TODO: handle exceptions
            }
        }
        // We did not find the file
        return false
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        // If the activity is null, get out of here.
        val activity = activityRef.get() ?: return
        // Notify to update the video removed
        val deleteBroadcast = Intent(MainActivity.ACTION_DELETE_VIDEO)
            .putExtra(ScreenRecorderService.SCREEN_RECORD_URI, mFileUri)
        // Send broadcast, this will be listened the MainActivity
        activity.sendBroadcast(deleteBroadcast)
    }
}