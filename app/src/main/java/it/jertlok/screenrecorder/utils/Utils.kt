package it.jertlok.screenrecorder.utils

import android.util.DisplayMetrics

open class Utils {

    // Most of them will be static methods, we will see what to do later.

    companion object {
        /**
         * This function will give us the maximum resolution available, which
         *  will be useful for trimming down the other resolutions available and
         *  set the default resolution for the video configuration.
         *
         *  @param realMetrics The display metrics got from the windowManager
         */
        fun getDisplayResolution(realMetrics: DisplayMetrics): String {
            // Let's get the display values
            val width = realMetrics.widthPixels
            val height = realMetrics.heightPixels
            // Return the resolution in the [width]x[height] format
            return height.toString() + "x" + width.toString()
        }
    }
}