package lk.fuelbuddy.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _plates = MutableStateFlow(getPlatesFromPrefs())
    val platesState: StateFlow<List<String>> = _plates.asStateFlow()

    private val _isOnboarded = mutableStateOf(getPlatesFromPrefs().isNotEmpty())
    val isOnboardedState: State<Boolean> = _isOnboarded

    val newsArticles: Flow<List<NewsArticle>> = dao.getAllNews()
    val fuelPrices: Flow<List<FuelPrice>> = dao.getPrices()

    fun addPlate(plate: String) {
        val current = getPlatesFromPrefs().toMutableSet()
        current.add(plate.uppercase())
        prefs.edit().putString("plate_numbers", current.joinToString(",")).apply()
        _plates.value = current.toList()
        _isOnboarded.value = true
        FuelAlarmReceiver.scheduleNextAlarm(getApplication())
    }

    fun removePlate(plate: String) {
        val current = getPlatesFromPrefs().toMutableSet()
        current.remove(plate.uppercase())
        prefs.edit().putString("plate_numbers", current.joinToString(",")).apply()
        _plates.value = current.toList()
        if (current.isEmpty()) _isOnboarded.value = false
        FuelAlarmReceiver.scheduleNextAlarm(getApplication())
    }

    private fun getPlatesFromPrefs(): List<String> = prefs.getString("plate_numbers", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    fun getLastFuelDate(): Long = prefs.getLong("last_fuel_date", 0L)
    fun isOnboarded(): Boolean = _isOnboarded.value

    init {
        viewModelScope.launch {
            // Market-Accurate Pricing (Parity)
            val mockPrices = listOf(
                FuelPrice("Ceypetco_92", 371.0, System.currentTimeMillis()),
                FuelPrice("Ceypetco_95", 456.0, System.currentTimeMillis()),
                FuelPrice("Ceypetco_Diesel", 363.0, System.currentTimeMillis()),
                FuelPrice("Ceypetco_Super", 468.0, System.currentTimeMillis()),
                FuelPrice("LIOC_92", 371.0, System.currentTimeMillis()),
                FuelPrice("LIOC_95", 456.0, System.currentTimeMillis()),
                FuelPrice("LIOC_Diesel", 363.0, System.currentTimeMillis()),
                FuelPrice("LIOC_Super", 468.0, System.currentTimeMillis())
            )
            mockPrices.forEach { dao.updatePrice(it) }
        }
    }
}
