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

package it.jertlok.recordie.services

import android.content.*
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import it.jertlok.recordie.activities.RecordingActivity

@RequiresApi(Build.VERSION_CODES.N)
open class RecordQSTileService : TileService() {
    // Broadcast receiver for updating QS button from service
    private val mBroadcastReceiver = LocalBroadcastReceiver()
    private val mIntentFilter = IntentFilter()
    // ScreenRecorderService
    private var mBound = false
    private lateinit var mBoundService: ScreenRecorderService
    private val mConnection = LocalServiceConnection()

    override fun onCreate() {
        super.onCreate()
        // Set intent filter
        mIntentFilter.addAction(ACTION_ENABLE_QS)
        mIntentFilter.addAction(ACTION_DISABLE_QS)
        // Register receiver
        registerReceiver(mBroadcastReceiver, mIntentFilter)
        // Bind to ScreenRecorderService
        val intent = Intent(this, ScreenRecorderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind service
        unbindService(mConnection)
        mBound = false
        // Unregister broadcast receiver
        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onClick() {
        super.onClick()
        // Start Tiling recording activity
        if (mBound && !mBoundService.isRecording() && !mBoundService.mRecScheduled) {
            val startIntent = Intent(this, RecordingActivity::class.java)
                .setAction(RecordingActivity.ACTION_QS_START)
            startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivityAndCollapse(startIntent)
        } else if (mBound && mBoundService.isRecording()) {
            // Stop service
            val stopIntent = Intent(this, ScreenRecorderService::class.java)
                .setAction(ScreenRecorderService.ACTION_STOP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(stopIntent)
            } else {
                startService(stopIntent)
            }
        }
    }


    private fun setQsToggle(state: Boolean) {
        if (state) qsTile.state = Tile.STATE_ACTIVE else qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

    private inner class LocalBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_ENABLE_QS -> setQsToggle(true)
                ACTION_DISABLE_QS -> setQsToggle(false)
            }
        }
    }

    private inner class LocalServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mBoundService = (service as ScreenRecorderService.LocalBinder).getService()
            mBound = true
            // Set toggle
            if (mBoundService.isRecording()) {
                setQsToggle(true)
            } else {
                setQsToggle(false)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mBound = false
        }
    }

    companion object {
        const val ACTION_DISABLE_QS = "it.jertlok.services.RecordQSTileService.ACTION_DISABLE_QS"
        const val ACTION_ENABLE_QS = "it.jertlok.services.RecordQSTileService.ACTION_ENABLE_QS"
    }
}