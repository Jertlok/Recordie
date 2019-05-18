package it.jertlok.screenrecorder.common

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES

class SdkHelper {

    companion object {


        fun atleastM(): Boolean {
            return SDK_INT >= VERSION_CODES.M
        }

        fun atleastN(): Boolean {
            return SDK_INT >= VERSION_CODES.N
        }

        fun atleastO(): Boolean {
            return SDK_INT >= VERSION_CODES.O
        }

        fun atleastP(): Boolean {
            return SDK_INT >= VERSION_CODES.P
        }
    }
}