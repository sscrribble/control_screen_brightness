package com.sscrribble.nightdim

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class DimTileService : TileService() {

    override fun onStartListening() = refresh()

    override fun onClick() {
        if (!Dimmer.toggle(this)) {
            Toast.makeText(this, "권한 설정이 필요해요. 앱을 열어주세요.", Toast.LENGTH_LONG).show()
            openApp()
        }
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val ready = Dimmer.hasSecurePermission(this)
        tile.state = when {
            !ready -> Tile.STATE_UNAVAILABLE
            Dimmer.isOn(this) -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.subtitle = if (!ready) "설정 필요" else null
        tile.updateTile()
    }

    private fun openApp() {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(i)
        }
    }
}
