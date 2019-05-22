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

package it.jertlok.recordie.tasks

import android.os.AsyncTask
import android.provider.MediaStore
import it.jertlok.recordie.activities.MainActivity
import it.jertlok.recordie.common.ScreenVideo
import java.lang.ref.WeakReference

class UpdateSingleVideoTask(context: MainActivity) : AsyncTask<String, Void, Boolean>() {
    private val activityRef: WeakReference<MainActivity> = WeakReference(context)
    private var mDeleteAction = false

    override fun doInBackground(vararg params: String): Boolean {
        val activity = activityRef.get()
        if (activity == null || activity.isFinishing || params.size > 1) {
            return false
        }
        // Get fileUri
        val fileUri = params[0]
        // Get content resolver
        val contentResolver = activity.contentResolver
        // The columns we need to retrieve
        val projection = arrayOf(
            MediaStore.Video.Media.DATA, // index: 0
            MediaStore.Video.Media.TITLE, // index: 1
            MediaStore.Video.Media.DURATION // index: 2
        )
        // Let's try to do the query
        val cursor = contentResolver?.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            MediaStore.Video.Media.DATA + " = '$fileUri'",
            null, null
        )
        // Try to get the element
        cursor?.apply {
            // Workaround: Marshmallow contentResolver doesn't distinguish between media URIs
            if (moveToFirst()) {
                // Build ScreenVideo element
                val screenVideo = ScreenVideo(
                    getString(/* DATA */ 0),
                    getString(/* TITLE */ 1),
                    getString(/* DURATION */ 2)
                )
                activity.mVideoArray.add(0, screenVideo)
            } else {
                mDeleteAction = true
            }
        }
        // Close the cursor
        cursor?.close()
        return true
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        val activity = activityRef.get()
        if (activity == null || activity.isFinishing || mDeleteAction) {
            return
        }
        // Notify that the data has changed.
        activity.mVideoAdapter.notifyItemInserted(0)
        // Do not scroll ffs.
        activity.mRecyclerView.smoothScrollToPosition(0)
    }
}