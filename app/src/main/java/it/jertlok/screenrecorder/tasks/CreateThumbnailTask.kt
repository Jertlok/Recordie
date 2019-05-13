package it.jertlok.screenrecorder.tasks

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.media.ThumbnailUtils
import android.os.AsyncTask
import android.provider.MediaStore
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import it.jertlok.screenrecorder.R
import it.jertlok.screenrecorder.adapters.VideoAdapter
import java.lang.ref.WeakReference

class CreateThumbnailTask(adapter: VideoAdapter, holder: VideoAdapter.VideoHolder) : AsyncTask<String, Void, Boolean>() {
    // We need to do operations on the caller, so we create a weak reference.
    private val adapterRef: WeakReference<VideoAdapter> = WeakReference(adapter)
    private val holderRef: WeakReference<VideoAdapter.VideoHolder> = WeakReference(holder)

    private var mThumbnail: Bitmap? = null

    override fun doInBackground(vararg params: String?): Boolean {
        val adapter = adapterRef.get() ?: return false
        if (params.size > 1) {
            return false
        }
        val fileUri = params[0]

        synchronized(adapter.mCache) {
            if (adapter.mCache.get(fileUri) == null) {
                mThumbnail = ThumbnailUtils.createVideoThumbnail(
                    fileUri,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )
                if (mThumbnail != null) {
                    adapter.mCache.put(fileUri, mThumbnail)
                }
            } else {
                mThumbnail = adapter.mCache.get(fileUri)
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
            // TODO temporary use some fade to make glitch appear less
            val transition = TransitionDrawable(arrayOf(ColorDrawable(Color.TRANSPARENT),
                mThumbnail?.toDrawable(holder.itemView.resources)))
            holderRef.get()?.image?.setImageDrawable(transition)
            holderRef.get()?.image?.scaleType = ImageView.ScaleType.CENTER_CROP
            transition.startTransition(350)
        } else {
            val transition = TransitionDrawable(arrayOf(ColorDrawable(Color.TRANSPARENT),
                holder.itemView.context.getDrawable(R.drawable.ic_movie)))
            // We couldn't create / load a thumbnail, so we set the placeholder.
            holder.image.setImageDrawable(transition)
            transition.startTransition(350)
        }
    }
}