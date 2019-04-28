package it.jertlok.screenrecorder.adapters

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.AsyncTask
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.common.ScreenVideo

class VideoAdapter(private val videos: ArrayList<ScreenVideo>) :
        RecyclerView.Adapter<VideoAdapter.VideoHolder>() {

    private lateinit var mThumbnail: ArrayList<Bitmap>

    class VideoHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var image: ImageView = view.findViewById(R.id.image)
        var title: TextView = view.findViewById(R.id.title)
        // var duration: TextView = view.findViewById(R.id.duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.video_list_row,
                parent, false)
//        mThumbnail = LoadImageAsync().execute(videos as String).get()
        return VideoHolder(itemView)
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
        // holder.duration.text = video.duration

    }

    override fun getItemCount(): Int {
        return videos.size
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