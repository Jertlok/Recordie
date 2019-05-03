package it.jertlok.screenrecorder.services

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
open class RecordQSTileService: TileService() {
    // TODO: handle states...
    override fun onClick() {
        // Start Tiling recording activity
//        val intent = Intent(this, ScreenRecorderService::class.java)
//                .setAction(ScreenRecorderService.ACTION_STOP)
//        startService(intent)
        super.onClick()
    }
}