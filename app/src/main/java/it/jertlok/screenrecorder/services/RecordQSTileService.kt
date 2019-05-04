package it.jertlok.screenrecorder.services

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import it.jertlok.screenrecorder.activities.RecordingActivity

@RequiresApi(Build.VERSION_CODES.N)
open class RecordQSTileService: TileService() {
    // TODO: handle states by binding with ScreenRecorderService
    override fun onClick() {
        super.onClick()
        // Start Tiling recording activity
        if (qsTile.state == Tile.STATE_INACTIVE) {
            qsTile.state = Tile.STATE_ACTIVE
            val startIntent = Intent(this, RecordingActivity::class.java)
                    .setAction(RecordingActivity.ACTION_START)
            startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivityAndCollapse(startIntent)
        } else if (qsTile.state == Tile.STATE_ACTIVE) {
            qsTile.state = Tile.STATE_INACTIVE
            val stopIntent = Intent(this, ScreenRecorderService::class.java)
                    .setAction(ScreenRecorderService.ACTION_STOP)
            startService(stopIntent)
        }
        // Update look
        qsTile.updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        // Set inactive state
        qsTile.state = Tile.STATE_INACTIVE
        // Update look
        qsTile.updateTile()
    }
}