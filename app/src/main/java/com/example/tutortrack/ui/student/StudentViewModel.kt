package com.example.tutortrack.ui.student

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.tutortrack.data.AppDatabase
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.data.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudentRepository
    
    // Active students only (default)
    val allStudents: LiveData<List<Student>>
    
    // All students including archived
    val allStudentsIncludingArchived: LiveData<List<Student>>
    
    // Archived students only
    val archivedStudents: LiveData<List<Student>>

    init {
        val studentDao = AppDatabase.getDatabase(application).studentDao()
        repository = StudentRepository(studentDao)
        allStudents = repository.allStudents
        allStudentsIncludingArchived = repository.allStudentsIncludingArchived
        archivedStudents = repository.archivedStudents
    }

    fun insertStudent(student: Student) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertStudent(student)
    }

    fun updateStudent(student: Student) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateStudent(student)
    }

    fun deleteStudent(student: Student) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteStudent(student)
    }
    
    /**
     * Archive a student
     * @param student The student to archive
     */
    fun archiveStudent(student: Student) = viewModelScope.launch(Dispatchers.IO) {
        repository.archiveStudent(student, true)
    }
    
    /**
     * Unarchive a student
     * @param student The student to unarchive
     */
    fun unarchiveStudent(student: Student) = viewModelScope.launch(Dispatchers.IO) {
        repository.archiveStudent(student, false)
    }

    fun getStudentById(id: Long): LiveData<Student> {
        return repository.getStudentById(id)
    }

    /**
     * Get the last inserted student - for use when creating a new student and needing its ID
     */
    fun getLastInsertedStudent(): LiveData<Student> {
        return repository.getLastInsertedStudent()
    }

    /**
     * Insert a student and return its ID immediately (synchronously)
     */
    fun insertStudentAndGetId(student: Student): Long {
        return repository.insertStudentAndGetId(student)
    }

    /**
     * Search active (non-archived) students
     */
    fun searchStudents(query: String): LiveData<List<Student>> {
        return repository.searchStudents(query)
    }
    
    /**
     * Search archived students only
     */
    fun searchArchivedStudents(query: String): LiveData<List<Student>> {
        return repository.searchArchivedStudents(query)
    }
    
    /**
     * Search all students regardless of archive status
     */
    fun searchAllStudents(query: String): LiveData<List<Student>> {
        return repository.searchAllStudents(query)
    }
} 