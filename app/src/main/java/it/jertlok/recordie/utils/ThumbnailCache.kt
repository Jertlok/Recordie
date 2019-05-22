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

package it.jertlok.recordie.utils

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