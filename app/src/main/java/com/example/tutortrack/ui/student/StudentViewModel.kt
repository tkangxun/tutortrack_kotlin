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
    val allStudents: LiveData<List<Student>>

    init {
        val studentDao = AppDatabase.getDatabase(application).studentDao()
        repository = StudentRepository(studentDao)
        allStudents = repository.allStudents
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

    fun getStudentById(id: Long): LiveData<Student> {
        return repository.getStudentById(id)
    }

    /**
     * Get the last inserted student - for use when creating a new student and needing its ID
     */
    fun getLastInsertedStudent(): LiveData<Student> {
        return repository.getLastInsertedStudent()
    }

    fun searchStudents(query: String): LiveData<List<Student>> {
        return repository.searchStudents(query)
    }
} 