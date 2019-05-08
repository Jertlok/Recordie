package it.jertlok.screenrecorder.utils

import android.graphics.Bitmap
import android.util.LruCache

class ThumbnailCache : LruCache<String, Bitmap>(CACHE_SIZE) {

    override fun sizeOf(key: String?, value: Bitmap): Int {
        return value.byteCount
    }

    companion object {
        private const val CACHE_SIZE = 4 * 1024 * 1024 // 4MiB
    }
}