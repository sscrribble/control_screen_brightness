package com.sscrribble.nightdim

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import kotlin.math.sqrt

/**
 * 디밍이 켜져 있는 동안만 돌아가는 서비스.
 * 폰을 세게 흔들면 디밍을 끈다. 다른 경로로 디밍이 꺼지면 스스로 종료.
 */
class DimService : Service(), SensorEventListener {

    companion object {
        private const val CHANNEL = "dim"
        private const val ACTION_OFF = "off"
        private const val SHAKE_G = 2.3f        // 중력 대비 가속도 배수
        private const val SHAKES_NEEDED = 3     // 이 횟수만큼
        private const val WINDOW_MS = 1200L     // 이 시간 안에 흔들어야 함

        fun start(ctx: Context) {
            try {
                ctx.startForegroundService(Intent(ctx, DimService::class.java))
            } catch (e: Exception) {
                // 백그라운드 시작 제한 등. 디밍 자체는 정상 동작.
                Log.w("NightDim", "shake service start failed", e)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, DimService::class.java))
        }
    }

    private lateinit var sensors: SensorManager
    private val shakeTimes = ArrayDeque<Long>()
    private var lastShake = 0L

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (!Dimmer.isOn(this@DimService)) Dimmer.onTurnedOffExternally(this@DimService)
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        sensors = getSystemService(SensorManager::class.java)
        startForeground(
            1, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("reduce_bright_colors_activated"), false, observer
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OFF) {
            Dimmer.setOn(this, false)
        } else if (!Dimmer.isOn(this)) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        sensors.unregisterListener(this)
        contentResolver.unregisterContentObserver(observer)
        super.onDestroy()
    }

    override fun onSensorChanged(e: SensorEvent) {
        val (x, y, z) = e.values
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (g < SHAKE_G) return
        val now = System.currentTimeMillis()
        if (now - lastShake < 150) return // 한 번 흔든 게 여러 번 잡히지 않게
        lastShake = now
        shakeTimes.addLast(now)
        while (shakeTimes.isNotEmpty() && now - shakeTimes.first() > WINDOW_MS) shakeTimes.removeFirst()
        if (shakeTimes.size >= SHAKES_NEEDED) {
            shakeTimes.clear()
            Dimmer.setOn(this, false)
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "디밍 상태", NotificationManager.IMPORTANCE_LOW)
        )
        val off = PendingIntent.getService(
            this, 1, Intent(this, DimService::class.java).setAction(ACTION_OFF),
            PendingIntent.FLAG_IMMUTABLE
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_dim)
            .setContentTitle("수면 디밍 켜짐")
            .setContentText("폰을 흔들면 꺼져요")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "끄기", off).build())
            .setOngoing(true)
            .build()
    }
}
