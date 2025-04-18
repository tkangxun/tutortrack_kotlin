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

    // Method to get all students regardless of archive status
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudentsIncludingArchived(): LiveData<List<Student>>
    
    // Default method - only returns active (non-archived) students
    @Query("SELECT * FROM students WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllStudents(): LiveData<List<Student>>
    
    // Method to get only archived students
    @Query("SELECT * FROM students WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedStudents(): LiveData<List<Student>>

    @Query("SELECT * FROM students WHERE id = :studentId")
    fun getStudentById(studentId: Long): LiveData<Student>

    // Updated search to only include active students
    @Query("SELECT * FROM students WHERE name LIKE '%' || :searchQuery || '%' AND isArchived = 0")
    fun searchStudents(searchQuery: String): LiveData<List<Student>>
    
    // Search that includes archived students
    @Query("SELECT * FROM students WHERE name LIKE '%' || :searchQuery || '%'")
    fun searchAllStudents(searchQuery: String): LiveData<List<Student>>
    
    // Search in archived students only
    @Query("SELECT * FROM students WHERE name LIKE '%' || :searchQuery || '%' AND isArchived = 1")
    fun searchArchivedStudents(searchQuery: String): LiveData<List<Student>>

    /**
     * Get the last inserted student by retrieving the one with highest ID
     */
    @Query("SELECT * FROM students ORDER BY id DESC LIMIT 1")
    fun getLastInsertedStudent(): LiveData<Student>

    /**
     * Get all students - synchronous version for import functionality
     */
    @Query("SELECT * FROM students WHERE isArchived = 0 ORDER BY name ASC")
    suspend fun getAllStudentsSync(): List<Student>
    
    /**
     * Get all students including archived - synchronous version for import functionality
     */
    @Query("SELECT * FROM students ORDER BY name ASC")
    suspend fun getAllStudentsIncludingArchivedSync(): List<Student>
    
    /**
     * Search for students containing the given text in their name - synchronous version for import
     */
    @Query("SELECT * FROM students WHERE name LIKE '%' || :searchText || '%' ORDER BY name ASC")
    suspend fun getStudentByNameContainingSync(searchText: String): List<Student>
} 