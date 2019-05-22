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

class UpdateVideosTask(context: MainActivity) : AsyncTask<Void, Void, Boolean>() {
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
            MediaStore.Video.Media.DURATION // index: 2
        )
        // Set cursor
        val cursor = contentResolver?.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            MediaStore.Video.Media.DATA + " LIKE '%Recordie/RCD%'",
            null,
            // Sort from newest to oldest
            MediaStore.Video.Media.DATE_TAKEN + " DESC"
        )
        // Go through list
        cursor?.apply {
            while (moveToNext()) {
                // Build ScreenVideo element
                val screenVideo = ScreenVideo(
                    getString(/* DATA */ 0),
                    getString(/* TITLE */ 1),
                    getString(/* DURATION */ 2)
                )
                activity.mVideoArrayUpdate.add(screenVideo)
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