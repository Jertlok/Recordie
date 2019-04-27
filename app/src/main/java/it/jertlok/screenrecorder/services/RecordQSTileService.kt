package it.jertlok.screenrecorder.services

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import it.jertlok.screenrecorder.activities.RecordingActivity

@RequiresApi(Build.VERSION_CODES.N)
open class RecordQSTileService: TileService() {
    // TODO: handle states...
    override fun onClick() {
        // Start Tiling recording activity
        val intent = Intent(this, RecordingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
        super.onClick()
    }
}