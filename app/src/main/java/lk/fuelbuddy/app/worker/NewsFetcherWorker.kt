package lk.fuelbuddy.app.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import lk.fuelbuddy.app.data.local.AppDatabase
import lk.fuelbuddy.app.data.local.NewsArticle
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NewsFetcherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val feeds = listOf(
                "http://www.adaderana.lk/rss.php",
                "https://www.newsfirst.lk/feed/"
            )

            val keywords = listOf("fuel", "petrol", "diesel", "cpc", "lioc")
            val articles = mutableListOf<NewsArticle>()

            feeds.forEach { feedUrl ->
                val source = if (feedUrl.contains("adaderana")) "Ada Derana" else "NewsFirst"
                try {
                    val doc = Jsoup.connect(feedUrl).get()
                    val items = doc.select("item")
                    
                    items.forEach { item ->
                        val title = item.select("title").text()
                        val link = item.select("link").text()
                        val pubDateStr = item.select("pubDate").text()
                        
                        // Filtering logic
                        if (keywords.any { title.lowercase().contains(it) }) {
                            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                            val pubDate = try { sdf.parse(pubDateStr)?.time ?: 0L } catch(e:Exception) { 0L }
                            
                            articles.add(NewsArticle(
                                id = link,
                                title = title,
                                link = link,
                                pubDate = pubDate,
                                source = source
                            ))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NewsFetcherWorker", "Error fetching from $feedUrl", e)
                }
            }

            if (articles.isNotEmpty()) {
                db.fuelDao().insertNews(articles)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("NewsFetcherWorker", "Work failed", e)
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<NewsFetcherWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "FuelNewsFetcher",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
