package it.jertlok.screenrecorder.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.common.ScreenVideo
import it.jertlok.screenrecorder.interfaces.AdapterInterface
import it.jertlok.screenrecorder.tasks.CreateThumbnailTask
import it.jertlok.screenrecorder.utils.ThumbnailCache
import it.jertlok.screenrecorder.utils.Utils


class VideoAdapter(private val videos: ArrayList<ScreenVideo>, private val mInterface: AdapterInterface) :
    RecyclerView.Adapter<VideoAdapter.VideoHolder>() {

    var selectedItems = ArrayList<ScreenVideo>()

    class VideoHolder(private val context: Context, view: View) : RecyclerView.ViewHolder(view) {
        var card: MaterialCardView = view.findViewById(R.id.card)
        var image: ImageView = view.findViewById(R.id.image)
        var title: TextView = view.findViewById(R.id.title)
        var duration: TextView = view.findViewById(R.id.duration)
        // Instantiate cache - referenced by the external CreateThumbnailTask.
        val mCache = ThumbnailCache()

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
        // Set item properties
        holder.title.text = video.title
        holder.duration.text = Utils.formatDuration(video.duration)
        holder.card.setTag(R.id.fileUri, video.data)
        // Let's create the thumbnail
        CreateThumbnailTask(holder).execute(video.data)
        // Start animating
        holder.itemView.animate()
        // Initialise card
        holder.card.isChecked = false
        // Set long click listener
        holder.card.setOnLongClickListener{
            cardBehaviour(holder, video)
            true
        }
        holder.card.setOnClickListener {
            if (selectedItems.size >= 1) {
                cardBehaviour(holder, video)
            } else {
                mInterface.playVideo(video.data)
            }
        }
    }

    private fun cardBehaviour(holder: VideoHolder, video: ScreenVideo) {
        if (!holder.card.isChecked) {
            holder.card.isChecked = true
            selectedItems.add(video)
            mInterface.updateCardCheck()
        } else {
            holder.card.isChecked = false
            selectedItems.remove(video)
            mInterface.updateCardCheck()
        }
    }

    override fun getItemCount(): Int {
        return videos.size
    }
}