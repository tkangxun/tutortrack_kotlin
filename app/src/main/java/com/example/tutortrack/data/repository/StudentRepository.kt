package com.example.tutortrack.data.repository

import androidx.lifecycle.LiveData
import com.example.tutortrack.data.dao.StudentDao
import com.example.tutortrack.data.model.Student

class StudentRepository(private val studentDao: StudentDao) {

    // Default - only returns active students
    val allStudents: LiveData<List<Student>> = studentDao.getAllStudents()
    
    // Returns all students including archived ones
    val allStudentsIncludingArchived: LiveData<List<Student>> = studentDao.getAllStudentsIncludingArchived()
    
    // Returns only archived students
    val archivedStudents: LiveData<List<Student>> = studentDao.getArchivedStudents()

    suspend fun insertStudent(student: Student): Long {
        return studentDao.insertStudent(student)
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
    }
    
    /**
     * Archives or unarchives a student
     * @param student The student to archive or unarchive
     * @param archive True to archive, false to unarchive
     */
    suspend fun archiveStudent(student: Student, archive: Boolean) {
        val updatedStudent = student.copy(isArchived = archive)
        studentDao.updateStudent(updatedStudent)
    }

    fun getStudentById(id: Long): LiveData<Student> {
        return studentDao.getStudentById(id)
    }

    fun searchStudents(query: String): LiveData<List<Student>> {
        return studentDao.searchStudents(query)
    }
    
    fun searchArchivedStudents(query: String): LiveData<List<Student>> {
        return studentDao.searchArchivedStudents(query)
    }
    
    fun searchAllStudents(query: String): LiveData<List<Student>> {
        return studentDao.searchAllStudents(query)
    }
    
    /**
     * Get the last inserted student - for use when creating a new student and needing its ID
     */
    fun getLastInsertedStudent(): LiveData<Student> {
        return studentDao.getLastInsertedStudent()
    }
    
    /**
     * Insert a student and return its ID immediately (synchronously)
     * This blocks the current thread, so it should only be called from a background thread
     */
    fun insertStudentAndGetId(student: Student): Long {
        return kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            studentDao.insertStudent(student)
        }
    }
} 