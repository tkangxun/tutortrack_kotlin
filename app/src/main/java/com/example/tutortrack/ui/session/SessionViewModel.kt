package com.example.tutortrack.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.tutortrack.data.AppDatabase
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.repository.SessionRepository
import com.example.tutortrack.ui.adapters.SessionWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import kotlinx.coroutines.withContext
import java.util.Calendar

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SessionRepository
    val allSessions: LiveData<List<Session>>
    val allSessionsWithDetails: LiveData<List<SessionWithDetails>>

    init {
        val sessionDao = AppDatabase.getDatabase(application).sessionDao()
        repository = SessionRepository(sessionDao)
        allSessions = repository.allSessions
        allSessionsWithDetails = repository.allSessionsWithDetails
    }

    fun insertSession(session: Session) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSession(session)
    }

    fun updateSession(session: Session) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateSession(session)
    }

    fun deleteSession(session: Session) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteSession(session)
    }

    fun getSessionById(id: Long): LiveData<Session> {
        return repository.getSessionById(id)
    }

    fun getSessionWithDetailsById(id: Long): LiveData<SessionWithDetails> {
        return repository.getSessionWithDetailsById(id)
    }

    fun getSessionsByStudentId(studentId: Long): LiveData<List<Session>> {
        return repository.getSessionsByStudentId(studentId)
    }

    fun getSessionsWithDetailsByStudentId(studentId: Long): LiveData<List<SessionWithDetails>> {
        return repository.getSessionsWithDetailsByStudentId(studentId)
    }

    fun getSessionsInDateRange(startDate: Date, endDate: Date): LiveData<List<Session>> {
        return repository.getSessionsInDateRange(startDate, endDate)
    }

    fun getSessionsWithDetailsInDateRange(startDate: Date, endDate: Date): LiveData<List<SessionWithDetails>> {
        return repository.getSessionsWithDetailsInDateRange(startDate, endDate)
    }

    fun getTotalIncome(): LiveData<Double> {
        return repository.getTotalIncome()
    }

    fun getIncomeInDateRange(startDate: Date, endDate: Date): LiveData<Double> {
        return repository.getIncomeInDateRange(startDate, endDate)
    }

    fun getMonthlyIncome(): LiveData<Double> {
        return repository.getMonthlyIncome()
    }

    fun getWeeklyIncome(): LiveData<Double> {
        return repository.getWeeklyIncome()
    }

    fun getYearlyIncome(): LiveData<Double> {
        return repository.getYearlyIncome()
    }

    fun getTotalUnpaidIncome(): LiveData<Double> {
        return repository.getTotalUnpaidIncome()
    }

    fun getUnpaidIncomeInDateRange(startDate: Date, endDate: Date): LiveData<Double> {
        return repository.getUnpaidIncomeInDateRange(startDate, endDate)
    }

    fun getMonthlyUnpaidIncome(): LiveData<Double> {
        return repository.getMonthlyUnpaidIncome()
    }

    fun getWeeklyUnpaidIncome(): LiveData<Double> {
        return repository.getWeeklyUnpaidIncome()
    }

    fun getYearlyUnpaidIncome(): LiveData<Double> {
        return repository.getYearlyUnpaidIncome()
    }

    /**
     * Get sessions in date range by timestamp (for export functionality)
     * @param startDateTimestamp the start date as a timestamp
     * @param endDateTimestamp the end date as a timestamp
     * @return List of SessionWithDetails in the date range
     */
    suspend fun getSessionsInDateRange(startDateTimestamp: Long, endDateTimestamp: Long): List<SessionWithDetails> {
        val startDate = Date(startDateTimestamp)
        
        // Set the end date to the end of the day (23:59:59)
        val endDateCalendar = Calendar.getInstance()
        endDateCalendar.timeInMillis = endDateTimestamp
        endDateCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endDateCalendar.set(Calendar.MINUTE, 59)
        endDateCalendar.set(Calendar.SECOND, 59)
        endDateCalendar.set(Calendar.MILLISECOND, 999)
        val endDate = endDateCalendar.time
        
        // Create synchronous version by accessing DAO directly
        val dao = AppDatabase.getDatabase(getApplication()).sessionDao()
        
        val sessionsWithStudentAndClassType = withContext(Dispatchers.IO) {
            dao.getSessionsWithDetailsInDateRangeSync(startDate, endDate)
        }
        
        // Map to SessionWithDetails
        return sessionsWithStudentAndClassType.map { it.toSessionWithDetails() }
    }
    
    /**
     * Bulk update multiple sessions as paid
     * @param sessionIds List of session IDs to mark as paid
     * @param paidDate The date to set as the payment date
     */
    fun bulkUpdateSessionsPaid(sessionIds: List<Long>, paidDate: Date) = viewModelScope.launch(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(getApplication()).sessionDao()
        sessionIds.forEach { sessionId ->
            val session = dao.getSessionByIdSync(sessionId)
            session?.let {
                val updatedSession = it.copy(isPaid = true, paidDate = paidDate)
                repository.updateSession(updatedSession)
            }
        }
    }
    
    /**
     * Force refresh data from the database
     * This is needed to ensure proper state when returning to the fragment
     */
    fun refreshData() = viewModelScope.launch(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(getApplication()).sessionDao()
        // Simply accessing the database will trigger LiveData updates
        val tempSessions = dao.getAllSessions()
        // No need to actually do anything with the result, just trigger the LiveData
    }
} 