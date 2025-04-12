package com.example.tutortrack.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.SessionWithStudentAndClassType
import java.util.Date

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): LiveData<List<Session>>

    @Query("SELECT * FROM sessions WHERE studentId = :studentId ORDER BY date DESC")
    fun getSessionsByStudentId(studentId: Long): LiveData<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionById(sessionId: Long): LiveData<Session>

    @Query("SELECT * FROM sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSessionsInDateRange(startDate: Date, endDate: Date): LiveData<List<Session>>

    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 1")
    fun getTotalIncome(): LiveData<Double>

    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 1 AND date BETWEEN :startDate AND :endDate")
    fun getIncomeInDateRange(startDate: Date, endDate: Date): LiveData<Double>

    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 1 AND strftime('%Y-%m', date/1000, 'unixepoch') = strftime('%Y-%m', 'now')")
    fun getMonthlyIncome(): LiveData<Double>

    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 1 AND date >= strftime('%s', 'now', '-7 days') * 1000")
    fun getWeeklyIncome(): LiveData<Double>

    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 1 AND strftime('%Y', date/1000, 'unixepoch') = strftime('%Y', 'now')")
    fun getYearlyIncome(): LiveData<Double>
    
    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 0")
    fun getTotalUnpaidIncome(): LiveData<Double>
    
    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 0 AND date BETWEEN :startDate AND :endDate")
    fun getUnpaidIncomeInDateRange(startDate: Date, endDate: Date): LiveData<Double>
    
    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 0 AND strftime('%Y-%m', date/1000, 'unixepoch') = strftime('%Y-%m', 'now')")
    fun getMonthlyUnpaidIncome(): LiveData<Double>
    
    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 0 AND date >= strftime('%s', 'now', '-7 days') * 1000")
    fun getWeeklyUnpaidIncome(): LiveData<Double>
    
    @Query("SELECT SUM(amount) FROM sessions WHERE isPaid = 0 AND strftime('%Y', date/1000, 'unixepoch') = strftime('%Y', 'now')")
    fun getYearlyUnpaidIncome(): LiveData<Double>
    
    @Transaction
    @Query("SELECT sessions.*, students.name as studentName, class_types.name as classTypeName FROM sessions " +
           "LEFT JOIN students ON sessions.studentId = students.id " +
           "LEFT JOIN class_types ON sessions.classTypeId = class_types.id " +
           "ORDER BY sessions.date DESC")
    fun getSessionsWithDetails(): LiveData<List<SessionWithStudentAndClassType>>
    
    @Transaction
    @Query("SELECT sessions.*, students.name as studentName, class_types.name as classTypeName FROM sessions " +
           "LEFT JOIN students ON sessions.studentId = students.id " +
           "LEFT JOIN class_types ON sessions.classTypeId = class_types.id " +
           "WHERE sessions.studentId = :studentId " +
           "ORDER BY sessions.date DESC")
    fun getSessionsWithDetailsByStudentId(studentId: Long): LiveData<List<SessionWithStudentAndClassType>>
    
    @Transaction
    @Query("SELECT sessions.*, students.name as studentName, class_types.name as classTypeName FROM sessions " +
           "LEFT JOIN students ON sessions.studentId = students.id " +
           "LEFT JOIN class_types ON sessions.classTypeId = class_types.id " +
           "WHERE sessions.id = :sessionId")
    fun getSessionWithDetailsById(sessionId: Long): LiveData<SessionWithStudentAndClassType>

    @Transaction
    @Query("SELECT sessions.*, students.name as studentName, class_types.name as classTypeName FROM sessions " +
           "LEFT JOIN students ON sessions.studentId = students.id " +
           "LEFT JOIN class_types ON sessions.classTypeId = class_types.id " +
           "WHERE sessions.date BETWEEN :startDate AND :endDate " +
           "ORDER BY sessions.date DESC")
    fun getSessionsWithDetailsInDateRange(startDate: Date, endDate: Date): LiveData<List<SessionWithStudentAndClassType>>

    /**
     * Get sessions by student ID - synchronous version for import functionality
     */
    @Query("SELECT * FROM sessions WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun getSessionsByStudentIdSync(studentId: Long): List<Session>
    
    /**
     * Get sessions in date range - synchronous version for import functionality
     */
    @Query("SELECT * FROM sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getSessionsInDateRangeSync(startDate: Date, endDate: Date): List<Session>
    
    /**
     * Get sessions with details in date range - synchronous version for export functionality
     */
    @Transaction
    @Query("SELECT sessions.*, students.name as studentName, class_types.name as classTypeName FROM sessions " +
           "LEFT JOIN students ON sessions.studentId = students.id " +
           "LEFT JOIN class_types ON sessions.classTypeId = class_types.id " +
           "WHERE sessions.date BETWEEN :startDate AND :endDate " +
           "ORDER BY sessions.date DESC")
    suspend fun getSessionsWithDetailsInDateRangeSync(startDate: Date, endDate: Date): List<SessionWithStudentAndClassType>
    
    /**
     * Get session by ID - synchronous version
     */
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionByIdSync(sessionId: Long): Session?
} 