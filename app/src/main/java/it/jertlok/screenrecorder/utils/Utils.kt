package it.jertlok.screenrecorder.utils

import android.content.ContentResolver
import android.provider.MediaStore
import android.util.DisplayMetrics
import it.jertlok.screenrecorder.common.ScreenVideo
import java.io.File
import java.util.concurrent.TimeUnit

open class Utils {

    // Most of them will be static methods, we will see what to do later.

    companion object {
        /**
         * This function will give us the maximum resolution available, which
         *  will be useful for trimming down the other resolutions available and
         *  set the default resolution for the video configuration.
         *
         *  @param realMetrics The display metrics got from the windowManager
         */
        fun getDisplayResolution(realMetrics: DisplayMetrics): String {
            // Let's get the display values
            val width = realMetrics.widthPixels
            val height = realMetrics.heightPixels
            // Return the resolution in the [width]x[height] format
            return height.toString() + "x" + width.toString()
        }

        /**
         * Deletes a file from the storage and the android database
         *
         * @param contentResolver
         * @param videoData: It's basically the path resulting from a previous content
         * resolver query. So this thing acts as the file path for doing all the work.
         *
         * @return boolean: true if it has deleted the file, false if not.
         */
        fun deleteFile(contentResolver: ContentResolver, videoData: String): Boolean {
            // The file we need to remove
            val where = "${MediaStore.Video.Media.DATA} = '$videoData'"
            // The resulting rows, that in our case must be a single value
            val rows = contentResolver.delete(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                where, null
            )
            // If we find the file inside our content resolver
            if (rows != 0) {
                // Let's try to remove the file
                try {
                    val file = File(videoData)
                    if (file.delete()) {
                        return true
                    }
                } catch (e: Exception) {
                    // Do nothing for now
                }
            }
            // We did not find the file
            return false
        }

        /**
         * Deletes a file from the storage and the android database
         *
         * @param contentResolver
         * @param videoData: It's basically the path resulting from a previous content
         * resolver query. So this thing acts as the file path for doing all the work.
         *
         * @return boolean: true if it has deleted the file, false if not.
         */
        fun deleteFiles(contentResolver: ContentResolver, videos: ArrayList<ScreenVideo>): Boolean {
            for (video: ScreenVideo in videos) {
                // The file we need to remove
                val where = "${MediaStore.Video.Media.DATA} = '${video.data}'"
                // The resulting rows, that in our case must be a single value
                val rows = contentResolver.delete(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    where, null
                )
                // If we find the file inside our content resolver
                if (rows != 0) {
                    // Let's try to remove the file
                    try {
                        val file = File(video.data)
                        if (file.delete()) {
                            // Do nothing for now
                        }
                    } catch (e: Exception) {
                        // Do nothing for now
                    }
                }
            }
            return true
        }

        fun formatDuration(duration: String): String {
            val longDuration = duration.toLong()

            return String.format("%dm:%ds",
                TimeUnit.MILLISECONDS.toMinutes(longDuration),
                TimeUnit.MILLISECONDS.toSeconds(longDuration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(longDuration)))
        }
    }
}