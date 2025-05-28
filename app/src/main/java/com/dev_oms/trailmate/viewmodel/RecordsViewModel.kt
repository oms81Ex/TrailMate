package com.dev_oms.trailmate.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

data class MonthlyStat(
    val value: String,
    val label: String
)

data class HikingRecord(
    val emoji: String,
    val title: String,
    val date: String,
    val duration: String,
    val distance: String,
    val rating: String,
    val completed: Boolean
)

class RecordsViewModel : ViewModel() {
    
    private val _monthlyStats = MutableStateFlow(generateMonthlyStats())
    val monthlyStats: StateFlow<List<MonthlyStat>> = _monthlyStats.asStateFlow()
    
    private val _growthPercentage = MutableStateFlow(25)
    val growthPercentage: StateFlow<Int> = _growthPercentage.asStateFlow()
    
    private val _hikingRecords = MutableStateFlow(generateHikingRecords())
    val hikingRecords: StateFlow<List<HikingRecord>> = _hikingRecords.asStateFlow()
    
    fun refreshData() {
        _monthlyStats.value = generateMonthlyStats()
        _hikingRecords.value = generateHikingRecords()
        _growthPercentage.value = (10..35).random()
    }
    
    private fun generateMonthlyStats(): List<MonthlyStat> {
        return listOf(
            MonthlyStat("${(5..12).random()}", "등산횟수"),
            MonthlyStat("${(30..60).random()}.${(0..9).random()}km", "총거리"),
            MonthlyStat("${(800..2000).random()}m", "상승고도")
        )
    }
    
    private fun generateHikingRecords(): List<HikingRecord> {
        val mountains = listOf(
            Triple("🏔️", "북한산 백운대", "5시간 20분"),
            Triple("🌲", "남산 순환코스", "1시간 45분"),
            Triple("⛰️", "관악산 연주대", "2시간 30분"),
            Triple("🏔️", "인왕산 코스", "1시간 15분"),
            Triple("🌄", "도봉산 자운봉", "3시간 10분"),
            Triple("⛰️", "북악산 둘레길", "2시간 00분")
        )
        
        val calendar = Calendar.getInstance()
        val records = mutableListOf<HikingRecord>()
        
        for (i in 0 until (3..6).random()) {
            calendar.add(Calendar.DAY_OF_YEAR, -(1..7).random())
            val mountain = mountains.random()
            
            records.add(
                HikingRecord(
                    emoji = mountain.first,
                    title = mountain.second,
                    date = "${calendar.get(Calendar.MONTH) + 1}월 ${calendar.get(Calendar.DAY_OF_MONTH)}일",
                    duration = mountain.third,
                    distance = "${(2..8).random()}.${(0..9).random()}km",
                    rating = generateRating(),
                    completed = true
                )
            )
        }
        
        return records.sortedByDescending { it.date }
    }
    
    private fun generateRating(): String {
        val stars = (3..5).random()
        return "⭐".repeat(stars)
    }
} 