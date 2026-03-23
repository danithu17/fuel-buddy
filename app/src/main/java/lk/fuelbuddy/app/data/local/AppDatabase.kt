package lk.fuelbuddy.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM news_articles ORDER BY pubDate DESC")
    fun getAllNews(): Flow<List<NewsArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(articles: List<NewsArticle>)

    @Query("SELECT * FROM fuel_prices")
    fun getPrices(): Flow<List<FuelPrice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePrice(price: FuelPrice)
}

@Database(entities = [NewsArticle::class, FuelPrice::class], version = 1, exportSchema = false)
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
