package app.constellationpulse.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object PulseReminderScheduler {
    const val CHANNEL_ID = "daily_pulse"
    private const val REQUEST_CODE = 2040

    fun scheduleDailyReminder(context: Context) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, PulseReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextReminderMillis(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Pulse",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Constellation Pulse daily chorus reminder"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun nextReminderMillis(): Long {
        val now = Calendar.getInstance()
        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, 19)
        target.set(Calendar.MINUTE, 55)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)

        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }
}
