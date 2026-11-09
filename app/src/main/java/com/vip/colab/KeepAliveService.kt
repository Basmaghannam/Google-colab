package com.vip.colab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

/** Holds a CPU wake lock + Wi-Fi lock and a foreground notification while Colab runs in the browser. */
class KeepAliveService : Service() {
    companion object {
        @Volatile var running = false
        const val ACTION_STOP = "com.vip.colab.STOP"
    }

    private var cpu: PowerManager.WakeLock? = null
    private var wifi: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("ka", "Keep Alive", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, KeepAliveService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_pause), "إيقاف", stop,
        ).build()
        val n = Notification.Builder(this, "ka")
            .setContentTitle("Colab Keep Alive")
            .setContentText("الجهاز والشبكة يبقيان نشطين")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(open)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        if (cpu == null) {
            cpu = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "colab:cpu")
            cpu?.acquire()
        }
        if (wifi == null) {
            try {
                wifi = applicationContext.getSystemService(WifiManager::class.java)
                    .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "colab:wifi")
                wifi?.acquire()
            } catch (e: Exception) {
                wifi = null
            }
        }
        running = true
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        cpu?.release()
        cpu = null
        wifi?.let { if (it.isHeld) it.release() }
        wifi = null
        super.onDestroy()
    }
}
