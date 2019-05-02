package it.jertlok.screenrecorder.adapters

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.AsyncTask
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.common.ScreenVideo
import java.io.File
import java.lang.Exception
import java.lang.ref.WeakReference

class VideoAdapter(private val videos: ArrayList<ScreenVideo>, private val mInterface: EventInterface) :
        RecyclerView.Adapter<VideoAdapter.VideoHolder>() {

    class VideoHolder(private val context: Context, view: View) : RecyclerView.ViewHolder(view) {
        var image: ImageView = view.findViewById(R.id.image)
        var title: TextView = view.findViewById(R.id.title)
        var deleteButton: Button = view.findViewById(R.id.delete)
        var shareButton: Button = view.findViewById(R.id.share)

        fun bindView(eventInterface: EventInterface) {
            // TODO: move this thing into image
            val videoData = deleteButton.getTag(R.id.fileUri).toString()
            deleteButton.setOnClickListener {
                val builder = AlertDialog.Builder(context,
                        R.style.Theme_MaterialComponents_Dialog_Alert)
                // Set positive button
                builder.setTitle("Delete screen record")
                builder.setPositiveButton(android.R.string.yes) { _, _ ->
                    deleteFile(videoData)
                    eventInterface.deleteEvent(videoData)
                }
                // Set negative button
                builder.setNeutralButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                // Show the dialog
                builder.show()
            }

            shareButton.setOnClickListener {
                eventInterface.shareVideo(videoData)
            }

            image.setOnClickListener {
                // Toast.makeText(context, "To be implemented...", Toast.LENGTH_SHORT).show()
                eventInterface.playVideo(videoData)
            }
        }

        /**
         * Deletes a file from the storage and the android database
         *
         * param videoData: It's basically the path resulting from a previous content
         * resolver query. So this thing acts as the file path for doing all the work.
         *
         * return boolean: true if it has deleted the file, false if not
         */
        private fun deleteFile(videoData: String): Boolean {
            val contentResolver = context.contentResolver
            // The file we need to remove
            val where = "${MediaStore.Video.Media.DATA} = '$videoData'"
            // The resulting rows, that in our case must be a single value
            val rows = contentResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    where, null)
            // If we find the file inside our content resolver
            if (rows != 0) {
                // Let's try to remove the file
                try {
                    val file = File(videoData)
                    if (file.delete()) {
                        return true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Couldn't remove file: $videoData")
                    return false
                }
            }
            // We did not find the file
            return false
        }

        companion object {
            private const val TAG = "VideoAdapter"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.video_list_row,
                parent, false)
        return VideoHolder(parent.context, itemView)
    }

    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        // We need to set the values, for now I am going to put something hardcoded.
        val video = videos[position]
        holder.title.text = video.title
        holder.deleteButton.setTag(R.id.fileUri, video.data)
        // Let's create the thumbnail
        CreateThumbnailTask(holder).execute(video.data)
        // So we can communicate from others activity
        holder.bindView(mInterface)
    }

    override fun getItemCount(): Int {
        return videos.size
    }

    interface EventInterface {
        // Called when we click on delete button
        fun deleteEvent(videoData: String)
        // Called when we click on play button
        fun playVideo(videoData: String)
        // Called when we click on share button
        fun shareVideo(videoData: String)
    }

    private class CreateThumbnailTask(context: VideoHolder): AsyncTask<String, Void, Boolean>() {
        val holderRef: WeakReference<VideoHolder> = WeakReference(context)
        private var mThumbnail: Bitmap? = null

        override fun doInBackground(vararg params: String?): Boolean {
            if (params.size > 1) {
                return false
            }
            val fileUri = params[0]
            mThumbnail =
                    ThumbnailUtils.createVideoThumbnail(fileUri, MediaStore.Video.Thumbnails.MINI_KIND)
            return true
        }

        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            // Set out thumbnail to be center crop
            if (mThumbnail != null) {
                holderRef.get()?.image?.setImageBitmap(mThumbnail)
                holderRef.get()?.image?.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }
}