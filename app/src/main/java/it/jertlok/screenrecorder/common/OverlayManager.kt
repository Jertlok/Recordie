/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.jertlok.screenrecorder.common

import android.annotation.SuppressLint
import android.graphics.PixelFormat.TRANSLUCENT
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.WindowManager.LayoutParams.*
import android.widget.TextView
import it.jertlok.screenrecorder.R

/** @author Aidan Follestad (@afollestad) **/
interface OverlayManager {

    /**
     * Returns true if a countdown is in progress.
     */
    fun isCountingDown(): Boolean

    /**
     * Returns true if a countdown is configured when recording starts.
     */
    fun willCountdown(): Boolean

    /**
     * Counts down starting at the value of the countdown preference, showing a red number in the
     * middle of the screen for each second. The given [finished] callback is invoked when we reach 0.
     */
    fun countdown(finished: () -> Unit, time: Int)
}

/** @author Aidan Follestad (@afollestad) **/
class RealOverlayManager(
    private val windowManager: WindowManager,
    private val layoutInflater: LayoutInflater
) : OverlayManager {
    companion object {
        private const val SECOND = 1000L
    }

    private var isCountingDown: Boolean = false

    override fun isCountingDown() = isCountingDown

    override fun willCountdown() = /* countdownPref.get() > 0 */ true

    @SuppressLint("InflateParams")
    override fun countdown(finished: () -> Unit, time: Int) {
        isCountingDown = true
        if (time <= 0) {
            isCountingDown = false
            finished()
            return
        }

        val textView: TextView =
            layoutInflater.inflate(R.layout.countdown_textview, null, false) as TextView
        textView.text = "$time"

        @Suppress("DEPRECATION")
        @SuppressLint("InlinedApi")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TYPE_APPLICATION_OVERLAY
        } else {
            TYPE_SYSTEM_OVERLAY
        }
        val params = LayoutParams(
            WRAP_CONTENT, // width
            WRAP_CONTENT, // height
            type,
            FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL, // flags
            TRANSLUCENT // format
        )
        windowManager.addView(textView, params)

        nextCountdown(textView, time, finished)
    }

    private fun nextCountdown(
        view: TextView,
        nextSecond: Int,
        finished: () -> Unit
    ) {
        view.text = "$nextSecond"
        if (nextSecond == 0) {
            // Immediately remove the view, so it doesn't appear on the recording.
            windowManager.removeViewImmediate(view)
            isCountingDown = false
            finished()
            return
        }
        view.postDelayed({
            nextCountdown(view, nextSecond - 1, finished)
        }, SECOND)
    }
}
