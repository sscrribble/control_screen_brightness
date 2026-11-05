package com.sscrribble.nightdim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * 안드로이드 기본 "Extra dim(밝기 더 낮추기)" 기능을 직접 제어한다.
 *
 * 오버레이(반투명 검은 막)를 띄우는 방식과 달리 디스플레이 색 변환 단계에서 적용되므로
 * 스크린샷/화면 녹화에는 어둡게 찍히지 않는다.
 */
object Dimmer {
    private const val KEY_ACTIVATED = "reduce_bright_colors_activated"
    private const val KEY_LEVEL = "reduce_bright_colors_level" // 0..100, 클수록 어두움
    private const val KEY_PERSIST = "reduce_bright_colors_persist_across_reboots"

    private const val PREFS = "nightdim"
    private const val P_LEVEL = "level"
    private const val P_MIN_BRIGHTNESS = "min_brightness"
    private const val P_SAVED_BRIGHTNESS = "saved_brightness"
    private const val P_SAVED_MODE = "saved_mode"
    private const val P_SHAKE_OFF = "shake_off"
    private const val P_AUTO_OFF = "auto_off"
    private const val P_AUTO_OFF_MIN = "auto_off_minutes"

    fun hasSecurePermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun canWriteSystem(ctx: Context) = Settings.System.canWrite(ctx)

    fun isOn(ctx: Context) =
        Settings.Secure.getInt(ctx.contentResolver, KEY_ACTIVATED, 0) == 1

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var Context.level: Int
        get() = prefs(this).getInt(P_LEVEL, 100)
        set(v) = prefs(this).edit().putInt(P_LEVEL, v).apply()

    var Context.useMinBrightness: Boolean
        get() = prefs(this).getBoolean(P_MIN_BRIGHTNESS, true)
        set(v) = prefs(this).edit().putBoolean(P_MIN_BRIGHTNESS, v).apply()

    var Context.shakeOff: Boolean
        get() = prefs(this).getBoolean(P_SHAKE_OFF, true)
        set(v) = prefs(this).edit().putBoolean(P_SHAKE_OFF, v).apply()

    var Context.autoOff: Boolean
        get() = prefs(this).getBoolean(P_AUTO_OFF, false)
        set(v) = prefs(this).edit().putBoolean(P_AUTO_OFF, v).apply()

    /** 자동 끄기 시각 (자정부터 분 단위), 기본 07:00 */
    var Context.autoOffMinutes: Int
        get() = prefs(this).getInt(P_AUTO_OFF_MIN, 7 * 60)
        set(v) = prefs(this).edit().putInt(P_AUTO_OFF_MIN, v).apply()

    /** 켜져 있으면 강도만 즉시 반영 */
    fun applyLevel(ctx: Context) {
        if (!hasSecurePermission(ctx)) return
        Settings.Secure.putInt(ctx.contentResolver, KEY_LEVEL, ctx.level)
    }

    /** @return 성공 여부 (권한 없으면 false) */
    fun setOn(ctx: Context, on: Boolean): Boolean {
        if (!hasSecurePermission(ctx)) return false
        val cr = ctx.contentResolver
        if (on) {
            Settings.Secure.putInt(cr, KEY_LEVEL, ctx.level)
            Settings.Secure.putInt(cr, KEY_PERSIST, 0)
            Settings.Secure.putInt(cr, KEY_ACTIVATED, 1)
            if (ctx.useMinBrightness) lowerBrightness(ctx)
        } else {
            Settings.Secure.putInt(cr, KEY_ACTIVATED, 0)
            restoreBrightness(ctx)
        }
        syncHelpers(ctx)
        return true
    }

    /** 켜짐 상태에 맞춰 흔들기 감지 서비스와 자동 끄기 알람을 시작/정리 */
    fun syncHelpers(ctx: Context) {
        val on = isOn(ctx)
        if (on && ctx.shakeOff) DimService.start(ctx) else DimService.stop(ctx)
        if (on && ctx.autoOff) AutoOff.schedule(ctx) else AutoOff.cancel(ctx)
    }

    /** 다른 경로(시스템 타일 등)로 꺼졌을 때 뒷정리 */
    fun onTurnedOffExternally(ctx: Context) {
        restoreBrightness(ctx)
        syncHelpers(ctx)
    }

    fun toggle(ctx: Context): Boolean = setOn(ctx, !isOn(ctx))

    private fun lowerBrightness(ctx: Context) {
        if (!canWriteSystem(ctx)) return
        val cr = ctx.contentResolver
        val p = prefs(ctx)
        if (!p.contains(P_SAVED_BRIGHTNESS)) {
            p.edit()
                .putInt(P_SAVED_BRIGHTNESS, Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128))
                .putInt(P_SAVED_MODE, Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 0))
                .apply()
        }
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, 0)
    }

    private fun restoreBrightness(ctx: Context) {
        val p = prefs(ctx)
        if (!p.contains(P_SAVED_BRIGHTNESS) || !canWriteSystem(ctx)) return
        val cr = ctx.contentResolver
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, p.getInt(P_SAVED_BRIGHTNESS, 128))
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, p.getInt(P_SAVED_MODE, 0))
        p.edit().remove(P_SAVED_BRIGHTNESS).remove(P_SAVED_MODE).apply()
    }
}
