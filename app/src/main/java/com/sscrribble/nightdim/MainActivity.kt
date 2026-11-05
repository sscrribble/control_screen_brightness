package com.sscrribble.nightdim

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
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
import com.sscrribble.nightdim.Dimmer.autoOff
import com.sscrribble.nightdim.Dimmer.autoOffMinutes
import com.sscrribble.nightdim.Dimmer.level
import com.sscrribble.nightdim.Dimmer.shakeOff
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

        val shake = findViewById<CheckBox>(R.id.shakeOff)
        shake.isChecked = shakeOff
        shake.setOnCheckedChangeListener { _, checked ->
            shakeOff = checked
            if (checked) askNotificationPermission()
            Dimmer.syncHelpers(this)
        }

        val auto = findViewById<CheckBox>(R.id.autoOff)
        val timeBtn = findViewById<Button>(R.id.autoOffTime)
        fun showTime() {
            timeBtn.text = "%02d:%02d".format(autoOffMinutes / 60, autoOffMinutes % 60)
        }
        showTime()
        auto.isChecked = autoOff
        auto.setOnCheckedChangeListener { _, checked ->
            autoOff = checked
            if (checked) askExactAlarmPermission()
            Dimmer.syncHelpers(this)
        }
        timeBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                autoOffMinutes = h * 60 + m
                showTime()
                Dimmer.syncHelpers(this)
            }, autoOffMinutes / 60, autoOffMinutes % 60, true).show()
        }

        findViewById<Button>(R.id.addTile).setOnClickListener { requestTile() }
        if (shakeOff) askNotificationPermission()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }

    private fun askExactAlarmPermission() {
        if (!getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
            Toast.makeText(this, "정확한 시각에 끄려면 '알람 및 리마인더'를 허용해주세요", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            )
        }
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
        if (ok) Dimmer.syncHelpers(this) // 권한 변경 등 반영 (예: 정확한 알람 허용 후)
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
