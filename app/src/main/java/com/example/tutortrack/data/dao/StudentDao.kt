package com.example.tutortrack.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tutortrack.data.model.Student

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): LiveData<List<Student>>

    @Query("SELECT * FROM students WHERE id = :studentId")
    fun getStudentById(studentId: Long): LiveData<Student>

    @Query("SELECT * FROM students WHERE name LIKE '%' || :searchQuery || '%'")
    fun searchStudents(searchQuery: String): LiveData<List<Student>>

    /**
     * Get the last inserted student by retrieving the one with highest ID
     */
    @Query("SELECT * FROM students ORDER BY id DESC LIMIT 1")
    fun getLastInsertedStudent(): LiveData<Student>
} 