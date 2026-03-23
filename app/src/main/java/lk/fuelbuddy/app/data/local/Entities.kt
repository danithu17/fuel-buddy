package lk.fuelbuddy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey val plateNumber: String,
    val fuelType: String, // e.g., "92 Petrol"
    val lastPumpDate: Long = 0L,
    val qrCodeUri: String? = null,
    val weeklyQuota: Double = 20.0
)

@Entity(tableName = "fuel_logs")
data class FuelLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val plateNumber: String,
    val liters: Double,
    val date: Long,
    val cost: Double = 0.0
)
