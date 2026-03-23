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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class FuelViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.fuelDao()
    private val prefs = application.getSharedPreferences("fuel_buddy_prefs", Context.MODE_PRIVATE)

    private val _isOnboarded = mutableStateOf(prefs.getString("plate_number", "")?.isNotEmpty() ?: false)
    val isOnboardedState: State<Boolean> = _isOnboarded

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
    fun isOnboarded(): Boolean = _isOnboarded.value

    init {
        viewModelScope.launch {
            // Expanded Mock Data for Ceypetco & LIOC
            val mockPrices = listOf(
                FuelPrice("Ceypetco_92", 366.0, System.currentTimeMillis()),
                FuelPrice("Ceypetco_95", 464.0, System.currentTimeMillis()),
                FuelPrice("Ceypetco_Diesel", 358.0, System.currentTimeMillis()),
                FuelPrice("Ceypetco_Super", 475.0, System.currentTimeMillis()),
                FuelPrice("LIOC_92", 372.0, System.currentTimeMillis()),
                FuelPrice("LIOC_95", 480.0, System.currentTimeMillis()),
                FuelPrice("LIOC_Diesel", 365.0, System.currentTimeMillis()),
                FuelPrice("LIOC_Super", 485.0, System.currentTimeMillis())
            )
            mockPrices.forEach { dao.updatePrice(it) }
        }
    }
}
