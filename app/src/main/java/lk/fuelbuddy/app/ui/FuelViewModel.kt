package lk.fuelbuddy.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import lk.fuelbuddy.app.data.local.AppDatabase
import lk.fuelbuddy.app.data.local.FuelPrice
import lk.fuelbuddy.app.data.local.NewsArticle
import lk.fuelbuddy.app.notifications.FuelAlarmReceiver

class FuelViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.fuelDao()
    private val prefs = application.getSharedPreferences("fuel_buddy_prefs", Context.MODE_PRIVATE)

    val newsArticles: Flow<List<NewsArticle>> = dao.getAllNews()
    val fuelPrices: Flow<List<FuelPrice>> = dao.getPrices()

    fun updatePlateNumber(plate: String) {
        prefs.edit().putString("plate_number", plate.uppercase()).apply()
        // Reschedule alarm when plate changes
        FuelAlarmReceiver.scheduleNextAlarm(getApplication())
    }

    fun getPlateNumber(): String = prefs.getString("plate_number", "") ?: ""

    init {
        // Pre-fill prices if empty (Mock data for start)
        viewModelScope.launch {
            dao.updatePrice(FuelPrice("Petrol", 350.0, System.currentTimeMillis()))
            dao.updatePrice(FuelPrice("Diesel", 310.0, System.currentTimeMillis()))
        }
    }
}
