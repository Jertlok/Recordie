package it.jertlok.screenrecorder.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.common.ScreenVideo
import java.io.File
import java.lang.Exception

class VideoAdapter(private val videos: ArrayList<ScreenVideo>, private val mInterface: EventInterface) :
        RecyclerView.Adapter<VideoAdapter.VideoHolder>() {

    class VideoHolder(private val context: Context, private val view: View) : RecyclerView.ViewHolder(view) {
        var image: ImageView = view.findViewById(R.id.image)
        var title: TextView = view.findViewById(R.id.title)
        var deleteButton: Button = view.findViewById(R.id.delete)
        var shareButton: Button = view.findViewById(R.id.share)
        // var duration: TextView = view.findViewById(R.id.duration)

        fun bindView(eventInterface: EventInterface) {
            deleteButton.setOnClickListener {
                // TODO: Add confirmation dialog
                deleteFile(deleteButton.getTag(R.id.fileUri).toString())
                eventInterface.deleteEvent()
            }

            shareButton.setOnClickListener {
                Toast.makeText(context, "To be implemented...", Toast.LENGTH_SHORT).show()
            }

            image.setOnClickListener {
                // Toast.makeText(context, "To be implemented...", Toast.LENGTH_SHORT).show()
                eventInterface.playVideo(deleteButton.getTag(R.id.fileUri).toString())
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

        private fun startVideo(videoData: String) {

        }

        companion object {
            private const val TAG = "VideoAdapter"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.video_list_row,
                parent, false)
//        mThumbnail = LoadImageAsync().execute(videos as String).get()
        return VideoHolder(parent.context, itemView)
    }

    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        // We need to set the values, for now I am going to put something hardcoded.
        val video = videos[position]
        // Let's create the thumbnail
        // TODO: We need to do some sort of async task...
        // TODO: We need to add onClickListener for images and actions
//        val thumbnail = ThumbnailUtils.createVideoThumbnail(video.data,
//              MediaStore.Video.Thumbnails.MINI_KIND)
//        holder.image.setImageBitmap(mThumbnail[position])
        holder.title.text = video.title
        holder.deleteButton.setTag(R.id.fileUri, video.data)

        // So we can communicate from others activity
        holder.bindView(mInterface)
    }

    override fun getItemCount(): Int {
        return videos.size
    }

    interface EventInterface {
        fun deleteEvent()

        fun playVideo(videoData: String)
    }

//    class LoadImageAsync : AsyncTask<String, Int, ArrayList<Bitmap>>() {
////        override fun doInBackground(var): Bitmap {
////            var bitmapArr = Array<Bitmap>(videos.size)
////            return ThumbnailUtils.createVideoThumbnail(videos,
////                    MediaStore.Video.Thumbnails.MINI_KIND)
////        }
//
//        override fun doInBackground(vararg params: String?): ArrayList<Bitmap> {
//            val videos = params[0] as ArrayList<ScreenVideo>
//
//            val bitmapArray = ArrayList<Bitmap>()
//
//            videos.forEach { vid -> bitmapArray.add(ThumbnailUtils.createVideoThumbnail(vid.data,
//                    MediaStore.Video.Thumbnails.MINI_KIND))}
//
//            return bitmapArray
//        }
//    }
}