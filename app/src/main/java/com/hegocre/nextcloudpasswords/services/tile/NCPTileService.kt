package com.hegocre.nextcloudpasswords.services.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.activities.MainActivity

class NCPTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            startActivity(intent)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.label = getString(R.string.app_name)
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }
}