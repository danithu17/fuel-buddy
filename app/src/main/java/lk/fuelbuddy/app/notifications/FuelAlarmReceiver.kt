package lk.fuelbuddy.app.notifications

import android.app.NotificationManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import lk.fuelbuddy.app.data.local.AppDatabase
import lk.fuelbuddy.app.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*

class FuelAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val db = AppDatabase.getDatabase(context)
        val fuelDao = db.fuelDao()

        CoroutineScope(Dispatchers.IO).launch {
            val vehicles = fuelDao.getAllVehicles().first()
            if (vehicles.isEmpty()) {
                // If no vehicles are registered, we can still check for a plate in SharedPreferences
                // This handles the initial setup where a user might have only set a plate number
                // without adding a full vehicle.
                val prefs = context.getSharedPreferences("fuel_buddy_prefs", Context.MODE_PRIVATE)
                val plate = prefs.getString("plate_number", "") ?: ""

                if (plate.isNotEmpty()) {
                    val lastDigit = plate.filter { it.isDigit() }.lastOrNull()?.toString()?.toIntOrNull() ?: -1
                    if (lastDigit != -1) {
                        val calendar = Calendar.getInstance()
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                        val isOdd = lastDigit % 2 != 0
                        val isFuelDay = when (dayOfWeek) {
                            Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY -> isOdd
                            Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY -> !isOdd
                            else -> false
                        }

                        if (isFuelDay) {
                            showNotification(context, plate)
                        }
                    }
                }
                scheduleNextAlarm(context)
                return@launch
            }

            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            vehicles.forEach { vehicle ->
                val lastDigit = vehicle.plateNumber.filter { it.isDigit() }.lastOrNull()?.toString()?.toIntOrNull() ?: -1
                if (lastDigit != -1) {
                    val isOdd = lastDigit % 2 != 0
                    val isFuelDay = when (today) {
                        Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY -> isOdd
                        Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY -> !isOdd
                        else -> false
                    }

                    if (isFuelDay) {
                        showNotification(context, vehicle.plateNumber)
                    }
                }
            }
            scheduleNextAlarm(context)
        }
    }

    private fun showNotification(context: Context, plate: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, "fuel_reminders")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("\uD83D\uDEA8 Fuel Day Alert!")
            .setContentText("Ada Oyage $plate ekata Fuel gahana dawasa!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(plate.hashCode(), builder.build())
    }

    companion object {
        fun scheduleNextAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, FuelAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
