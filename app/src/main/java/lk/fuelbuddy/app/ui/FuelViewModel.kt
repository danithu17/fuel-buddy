package lk.fuelbuddy.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import lk.fuelbuddy.app.data.local.AppDatabase
import lk.fuelbuddy.app.data.local.Vehicle
import lk.fuelbuddy.app.data.local.FuelLog
import lk.fuelbuddy.app.notifications.FuelAlarmReceiver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class FuelViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).fuelDao()

    val vehicles: Flow<List<Vehicle>> = dao.getAllVehicles()
    
    private val _onboarded = mutableStateOf(false)
    val isOnboardedState: State<Boolean> = _onboarded

    init {
        viewModelScope.launch {
            vehicles.collect { list ->
                _onboarded.value = list.isNotEmpty()
            }
        }
    }

    fun addVehicle(plate: String, type: String) {
        viewModelScope.launch {
            dao.insertVehicle(Vehicle(plate.uppercase(), type))
            FuelAlarmReceiver.scheduleNextAlarm(getApplication())
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            dao.deleteVehicle(vehicle)
        }
    }

    fun updateVehicleQR(plate: String, uri: String) {
        viewModelScope.launch {
            val existing = vehicles.first().find { it.plateNumber == plate }
            if (existing != null) {
                dao.insertVehicle(existing.copy(qrCodeUri = uri))
            }
        }
    }

    fun addFuelLog(plate: String, liters: Double, cost: Double) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.insertLog(FuelLog(plateNumber = plate, liters = liters, date = now, cost = cost))
            // Also update the vehicle's last pump date
            val existing = vehicles.first().find { it.plateNumber == plate }
            if (existing != null) {
                dao.insertVehicle(existing.copy(lastPumpDate = now))
            }
        }
    }

    fun getLogs(plate: String): Flow<List<FuelLog>> = dao.getLogsForVehicle(plate)

    fun getWeeklyLiters(plate: String): Flow<Double> = dao.getLogsForVehicle(plate).map { logs ->
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        logs.filter { it.date >= weekAgo }.sumOf { it.liters }
    }
}
