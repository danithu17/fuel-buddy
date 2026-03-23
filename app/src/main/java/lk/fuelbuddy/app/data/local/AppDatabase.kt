package lk.fuelbuddy.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM fuel_logs WHERE plateNumber = :plate ORDER BY date DESC")
    fun getLogsForVehicle(plate: String): Flow<List<FuelLog>>

    @Insert
    suspend fun insertLog(log: FuelLog)
}

@Database(entities = [Vehicle::class, FuelLog::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fuel_buddy_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
