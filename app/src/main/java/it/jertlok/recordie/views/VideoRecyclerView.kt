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

package it.jertlok.recordie.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

// Based on
// https://github.com/googlesamples/android-XYZTouristAttractions implementation by Google

class VideoRecyclerView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0):
    RecyclerView(context, attributeSet, defStyle) {

    var mEmptyView: View? = null

    private var mDataObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            updateEmptyView()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            updateEmptyView()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            updateEmptyView()
        }
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        if (getAdapter() != null) {
            getAdapter()?.unregisterAdapterDataObserver(mDataObserver)
        }
        adapter?.registerAdapterDataObserver(mDataObserver)
        super.setAdapter(adapter)
        updateEmptyView()
    }

    private fun updateEmptyView() {
        if (mEmptyView != null && adapter != null) {
            val showEmptyView = adapter!!.itemCount == 0
            mEmptyView?.visibility = if (showEmptyView) View.VISIBLE else View.GONE
            visibility = if (showEmptyView) View.GONE else View.VISIBLE
        }
    }
}