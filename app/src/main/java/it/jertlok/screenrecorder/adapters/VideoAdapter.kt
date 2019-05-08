package it.jertlok.screenrecorder.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.common.ScreenVideo
import it.jertlok.screenrecorder.interfaces.AdapterInterface
import it.jertlok.screenrecorder.tasks.CreateThumbnailTask
import it.jertlok.screenrecorder.utils.ThumbnailCache
import it.jertlok.screenrecorder.utils.Utils

class VideoAdapter(private val videos: ArrayList<ScreenVideo>, private val mInterface: AdapterInterface) :
    RecyclerView.Adapter<VideoAdapter.VideoHolder>() {

    class VideoHolder(private val context: Context, view: View) : RecyclerView.ViewHolder(view) {
        var image: ImageView = view.findViewById(R.id.image)
        var title: TextView = view.findViewById(R.id.title)
        var deleteButton: MaterialButton = view.findViewById(R.id.delete)
        private var shareButton: MaterialButton = view.findViewById(R.id.share)
        // Instantiate cache - referenced by the external CreateThumbnailTask.
        val mCache = ThumbnailCache()

        fun bindView(eventInterface: AdapterInterface) {
            // TODO: move this thing into image
            val videoData = deleteButton.getTag(R.id.fileUri).toString()
            deleteButton.setOnClickListener {
                val builder = MaterialAlertDialogBuilder(context)
                // Set positive button
                builder.setTitle("Delete screen record?")
                builder.setPositiveButton(R.string.delete) { _, _ ->
                    Utils.deleteFile(context.contentResolver, videoData)
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.video_list_row,
            parent, false
        )
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
}