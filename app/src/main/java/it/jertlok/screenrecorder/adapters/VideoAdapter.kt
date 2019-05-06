package it.jertlok.screenrecorder.adapters

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.AsyncTask
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.DialogCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
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
        var deleteButton: MaterialButton = view.findViewById(R.id.delete)
        private var shareButton: MaterialButton = view.findViewById(R.id.share)

        val mCache = object : LruCache<String, Bitmap>(CACHE_SIZE) {
            override fun sizeOf(key: String?, value: Bitmap): Int {
                return value.byteCount
            }
        }

        fun bindView(eventInterface: EventInterface) {
            // TODO: move this thing into image
            val videoData = deleteButton.getTag(R.id.fileUri).toString()
            deleteButton.setOnClickListener {
                val builder = MaterialAlertDialogBuilder(context)
                // Set positive button
                builder.setTitle("Delete screen record?")
                builder.setPositiveButton(R.string.yes) { _, _ ->
                    deleteFile(videoData)
                    eventInterface.deleteEvent(videoData)
                }
                // Set negative button
                builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
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
        // Add a simple animation
        val animation = AnimationUtils.loadAnimation(parent.context, android.R.anim.fade_in)
        animation.duration = 500
        itemView.animation = animation
        return VideoHolder(parent.context, itemView)
    }

    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        // We need to set the values, for now I am going to put something hardcoded.
        val video = videos[position]
        holder.title.text = video.title
        holder.deleteButton.setTag(R.id.fileUri, video.data)
        // Let's create the thumbnail
        CreateThumbnailTask(holder).execute(video.data)
        // Start animating
        holder.itemView.animate()
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
            val holder = holderRef.get() ?: return false
            if (params.size > 1) {
                return false
            }
            val fileUri = params[0]

            synchronized (holder.mCache) {
                if (holder.mCache.get(fileUri) == null) {
                    mThumbnail = ThumbnailUtils.createVideoThumbnail(fileUri,
                            MediaStore.Video.Thumbnails.MINI_KIND)
                    if (mThumbnail != null) {
                        holder.mCache.put(fileUri, mThumbnail)
                    }
                } else {
                    mThumbnail = holder.mCache.get(fileUri)
                }
            }
            return true
        }

        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            // Get element
            val holder = holderRef.get() ?: return
            // Set out thumbnail to be center crop
            if (mThumbnail != null) {
                holderRef.get()?.image?.setImageBitmap(mThumbnail)
                holderRef.get()?.image?.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                val placeholder = holder.itemView.context.getDrawable(R.drawable.ic_movie)
                holder.image.setImageDrawable(placeholder)
            }
        }
    }

    companion object {
        private const val CACHE_SIZE = 4 * 1024 * 1024 // 4MiB
    }
}