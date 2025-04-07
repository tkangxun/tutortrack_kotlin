package com.example.tutortrack.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.tutortrack.data.dao.SessionDao
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.SessionWithStudentAndClassType
import com.example.tutortrack.ui.adapters.SessionWithDetails
import java.util.Date

class SessionRepository(private val sessionDao: SessionDao) {

    val allSessions: LiveData<List<Session>> = sessionDao.getAllSessions()
    
    val allSessionsWithDetails: LiveData<List<SessionWithDetails>> = 
        sessionDao.getSessionsWithDetails().map { list ->
            list.map { it.toSessionWithDetails() }
        }

    suspend fun insertSession(session: Session): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session)
    }

    suspend fun deleteSession(session: Session) {
        sessionDao.deleteSession(session)
    }

    fun getSessionById(id: Long): LiveData<Session> {
        return sessionDao.getSessionById(id)
    }
    
    fun getSessionWithDetailsById(id: Long): LiveData<SessionWithDetails> {
        return sessionDao.getSessionWithDetailsById(id).map { it.toSessionWithDetails() }
    }

    fun getSessionsByStudentId(studentId: Long): LiveData<List<Session>> {
        return sessionDao.getSessionsByStudentId(studentId)
    }
    
    fun getSessionsWithDetailsByStudentId(studentId: Long): LiveData<List<SessionWithDetails>> {
        return sessionDao.getSessionsWithDetailsByStudentId(studentId).map { list ->
            list.map { it.toSessionWithDetails() }
        }
    }

    fun getSessionsInDateRange(startDate: Date, endDate: Date): LiveData<List<Session>> {
        return sessionDao.getSessionsInDateRange(startDate, endDate)
    }

    fun getSessionsWithDetailsInDateRange(startDate: Date, endDate: Date): LiveData<List<SessionWithDetails>> {
        return sessionDao.getSessionsWithDetailsInDateRange(startDate, endDate).map { list ->
            list.map { it.toSessionWithDetails() }
        }
    }

    fun getTotalIncome(): LiveData<Double> {
        return sessionDao.getTotalIncome()
    }

    fun getIncomeInDateRange(startDate: Date, endDate: Date): LiveData<Double> {
        return sessionDao.getIncomeInDateRange(startDate, endDate)
    }

    fun getMonthlyIncome(): LiveData<Double> {
        return sessionDao.getMonthlyIncome()
    }

    fun getWeeklyIncome(): LiveData<Double> {
        return sessionDao.getWeeklyIncome()
    }

    fun getYearlyIncome(): LiveData<Double> {
        return sessionDao.getYearlyIncome()
    }

    fun getTotalUnpaidIncome(): LiveData<Double> {
        return sessionDao.getTotalUnpaidIncome()
    }
    
    fun getUnpaidIncomeInDateRange(startDate: Date, endDate: Date): LiveData<Double> {
        return sessionDao.getUnpaidIncomeInDateRange(startDate, endDate)
    }
    
    fun getMonthlyUnpaidIncome(): LiveData<Double> {
        return sessionDao.getMonthlyUnpaidIncome()
    }
    
    fun getWeeklyUnpaidIncome(): LiveData<Double> {
        return sessionDao.getWeeklyUnpaidIncome()
    }
    
    fun getYearlyUnpaidIncome(): LiveData<Double> {
        return sessionDao.getYearlyUnpaidIncome()
    }
} 