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

package it.jertlok.recordie.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import it.jertlok.recordie.R
import it.jertlok.recordie.common.ScreenVideo
import it.jertlok.recordie.interfaces.AdapterInterface
import it.jertlok.recordie.tasks.CreateThumbnailTask
import it.jertlok.recordie.utils.ThumbnailCache
import it.jertlok.recordie.utils.Utils


class VideoAdapter(private val videos: ArrayList<ScreenVideo>, private val mInterface: AdapterInterface) :
    RecyclerView.Adapter<VideoAdapter.VideoHolder>() {

    var selectedItems = ArrayList<ScreenVideo>()
    var selectedHolder = ArrayList<VideoHolder>()
    val mCache = ThumbnailCache()

    class VideoHolder(private val context: Context, view: View) : RecyclerView.ViewHolder(view) {
        var card: MaterialCardView = view.findViewById(R.id.card)
        var image: ImageView = view.findViewById(R.id.image)
        var title: TextView = view.findViewById(R.id.title)
        var duration: TextView = view.findViewById(R.id.duration)
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
        // Get the video
        val video = videos[position]
        // Set item properties
        holder.title.text = video.title
        holder.duration.text = Utils.formatDuration(video.duration)
        holder.card.setTag(R.id.fileUri, video.data)
        // Let's create the thumbnail
        CreateThumbnailTask(this@VideoAdapter, holder).execute(video.data)
        // Initialise card
        holder.card.isChecked = false
        // Start animating
        holder.itemView.animate()
        // Set long click listener
        holder.card.setOnLongClickListener {
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
            selectedHolder.add(holder)
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