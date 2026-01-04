package com.example.make.data.news

import android.util.Log
import com.example.make.data.model.InvestmentInsight
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NewsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("investment_insights")

    suspend fun getLatestInsights(): List<InvestmentInsight> {
        return try {
            val snapshot = collection
                .limit(100)
                .get()
                .await()

            snapshot.toObjects(InvestmentInsight::class.java).sortedByDescending { 
                it.meta_data.analyzed_at 
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error fetching news", e)
            emptyList()
        }
    }
    suspend fun getEconomicEvents(): List<CalendarEvent> {
        return try {
            val snapshot = db.collection("economic_calendar")
                //.orderBy("date", Query.Direction.ASCENDING) // 인덱스 문제 방지 위해 잠시 주석 처리
                .limit(200)
                .get()
                .await()

            Log.d("NewsRepository", "Fetched ${snapshot.size()} calendar events")
            snapshot.toObjects(CalendarEvent::class.java).sortedBy { it.date } // 앱에서 정렬
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error fetching calendar", e)
            emptyList()
        }
    }
}

// Data Model for Firestore
data class CalendarEvent(
    var id: String = "",
    var date: String = "",
    var time: String = "",
    var country: String = "",
    var title: String = "",
    var importance: Int = 1,
    var forecast: String? = null,
    var previous: String? = null,
    var actual: String? = null
)
