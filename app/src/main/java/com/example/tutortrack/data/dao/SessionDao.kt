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
} 