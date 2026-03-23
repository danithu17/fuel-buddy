package lk.fuelbuddy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey val id: String,
    val title: String,
    val link: String,
    val pubDate: Long,
    val source: String,
    val content: String? = null
)

@Entity(tableName = "fuel_prices")
data class FuelPrice(
    @PrimaryKey val fuelType: String, // Petrol, Diesel
    val price: Double,
    val lastUpdated: Long
)
