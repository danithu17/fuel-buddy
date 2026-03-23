package lk.fuelbuddy.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import lk.fuelbuddy.app.data.local.AppDatabase
import lk.fuelbuddy.app.R
import java.util.*

class FuelAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Here we'd fetch the latest news from the database or just show the alert
        // For simplicity, we assume the plate is stored in SharedPreferences
        val prefs = context.getSharedPreferences("fuel_buddy_prefs", Context.MODE_PRIVATE)
        val plate = prefs.getString("plate_number", "") ?: ""
        
        if (plate.isEmpty()) return

        val lastDigit = plate.filter { it.isDigit() }.lastOrNull()?.toString()?.toIntOrNull() ?: -1
        if (lastDigit == -1) return

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday=1, Monday=2, ..., Saturday=7

        val isOdd = lastDigit % 2 != 0
        val isEven = lastDigit % 2 == 0

        val isMyDay = when (dayOfWeek) {
            Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY -> isOdd
            Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY -> isEven
            else -> false // Sunday or other
        }

        if (isMyDay) {
            showNotification(context)
        }
        
        // Reschedule for tomorrow
        scheduleNextAlarm(context)
    }

    private fun showNotification(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.fuelDao()
        
        // This is a simplified fetch to showcase the functionality
        // In a real scenario, we'd use a repository or run on a background thread properly
        val builder = NotificationCompat.Builder(context, "FUEL_ALERTS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("\uD83D\uDEA8 Ada Oyage Fuel Dawasa!")
            .setContentText("Status: Market is Active. Fuel Prices Updated.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("\uD83D\uDEA8 Ada Oyage Fuel Dawasa!\n\nToday is your assigned day. \nPetrol: Rs. 350 | Diesel: Rs. 310 \n\nLatest Fuel News: Cabinet approves new LIOC/CPC quota distribution for upcoming week. Stay ahead with FuelBuddy!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(101, builder.build())
        }
    }

    companion object {
        fun scheduleNextAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, FuelAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}
