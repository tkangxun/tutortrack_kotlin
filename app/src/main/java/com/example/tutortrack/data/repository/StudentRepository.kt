package com.example.tutortrack.data.repository

import androidx.lifecycle.LiveData
import com.example.tutortrack.data.dao.StudentDao
import com.example.tutortrack.data.model.Student

class StudentRepository(private val studentDao: StudentDao) {

    val allStudents: LiveData<List<Student>> = studentDao.getAllStudents()

    suspend fun insertStudent(student: Student): Long {
        return studentDao.insertStudent(student)
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
    }

    fun getStudentById(id: Long): LiveData<Student> {
        return studentDao.getStudentById(id)
    }

    fun searchStudents(query: String): LiveData<List<Student>> {
        return studentDao.searchStudents(query)
    }
    
    /**
     * Get the last inserted student - for use when creating a new student and needing its ID
     */
    fun getLastInsertedStudent(): LiveData<Student> {
        return studentDao.getLastInsertedStudent()
    }
} 