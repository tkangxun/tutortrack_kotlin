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
} 