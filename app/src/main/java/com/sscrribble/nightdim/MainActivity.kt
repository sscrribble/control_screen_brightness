package com.sscrribble.nightdim

import android.app.Activity
import android.app.StatusBarManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.quicksettings.TileService
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.sscrribble.nightdim.Dimmer.level
import com.sscrribble.nightdim.Dimmer.useMinBrightness

class MainActivity : Activity() {

    private val adbCmd by lazy {
        "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
    }

    private lateinit var status: TextView
    private lateinit var setup: View
    private lateinit var toggle: Button
    private lateinit var minBright: CheckBox
    private lateinit var levelText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        setup = findViewById(R.id.setup)
        toggle = findViewById(R.id.toggle)
        minBright = findViewById(R.id.minBrightness)
        levelText = findViewById(R.id.levelText)

        findViewById<TextView>(R.id.adbCmd).text = adbCmd
        findViewById<Button>(R.id.copyAdb).setOnClickListener {
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("adb", adbCmd))
            Toast.makeText(this, "복사됨", Toast.LENGTH_SHORT).show()
        }

        toggle.setOnClickListener {
            Dimmer.toggle(this)
            refresh()
        }

        val seek = findViewById<SeekBar>(R.id.level)
        seek.progress = level
        levelText.text = "강도: $level"
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                levelText.text = "강도: $p"
                if (fromUser) { level = p; Dimmer.applyLevel(this@MainActivity) }
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })

        minBright.setOnCheckedChangeListener { _, checked ->
            if (checked && !Dimmer.canWriteSystem(this)) {
                minBright.isChecked = false
                startActivity(
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
                )
                return@setOnCheckedChangeListener
            }
            useMinBrightness = checked
        }

        findViewById<Button>(R.id.addTile).setOnClickListener { requestTile() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val ok = Dimmer.hasSecurePermission(this)
        setup.visibility = if (ok) View.GONE else View.VISIBLE
        toggle.isEnabled = ok
        val on = ok && Dimmer.isOn(this)
        status.text = when {
            !ok -> "⚠ 최초 1회 설정이 필요해요"
            on -> "🌙 디밍 켜짐"
            else -> "디밍 꺼짐"
        }
        toggle.text = if (on) "끄기" else "켜기"
        minBright.isChecked = useMinBrightness && Dimmer.canWriteSystem(this)
        TileService.requestListeningState(this, ComponentName(this, DimTileService::class.java))
    }

    private fun requestTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            getSystemService(StatusBarManager::class.java).requestAddTileService(
                ComponentName(this, DimTileService::class.java),
                getString(R.string.tile_label),
                Icon.createWithResource(this, R.drawable.ic_dim),
                mainExecutor
            ) {}
        } else {
            Toast.makeText(
                this, "빠른 설정 패널 ✏️(편집)에서 '수면 디밍' 타일을 끌어다 놓으세요",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
