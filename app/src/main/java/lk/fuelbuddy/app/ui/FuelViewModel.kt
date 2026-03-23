package lk.fuelbuddy.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import lk.fuelbuddy.app.data.local.AppDatabase
import lk.fuelbuddy.app.data.local.FuelPrice
import lk.fuelbuddy.app.data.local.NewsArticle
import lk.fuelbuddy.app.notifications.FuelAlarmReceiver

class FuelViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.fuelDao()
    private val prefs = application.getSharedPreferences("fuel_buddy_prefs", Context.MODE_PRIVATE)

    private val _isOnboarded = MutableStateFlow(getPlateNumber().isNotEmpty())
    val isOnboarded = _isOnboarded.asStateFlow()

    val newsArticles: Flow<List<NewsArticle>> = dao.getAllNews()
    val fuelPrices: Flow<List<FuelPrice>> = dao.getPrices()

    fun updatePlateNumber(plate: String) {
        prefs.edit().putString("plate_number", plate.uppercase()).apply()
        _isOnboarded.value = true
        // Reschedule alarm when plate changes
        FuelAlarmReceiver.scheduleNextAlarm(getApplication())
    }

    fun updateLastFuelDate(date: Long) {
        prefs.edit().putLong("last_fuel_date", date).apply()
    }

    fun getPlateNumber(): String = prefs.getString("plate_number", "") ?: ""
    fun getLastFuelDate(): Long = prefs.getLong("last_fuel_date", 0L)

    init {
        // Pre-fill prices if empty (Mock data for start)
        viewModelScope.launch {
            dao.updatePrice(FuelPrice("Petrol", 350.0, System.currentTimeMillis()))
            dao.updatePrice(FuelPrice("Diesel", 310.0, System.currentTimeMillis()))
        }
    }
}
