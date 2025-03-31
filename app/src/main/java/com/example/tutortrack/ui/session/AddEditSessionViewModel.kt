package com.example.tutortrack.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutortrack.data.AppDatabase
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.data.repository.ClassTypeRepository
import com.example.tutortrack.data.repository.SessionRepository
import com.example.tutortrack.data.repository.StudentRepository
import kotlinx.coroutines.launch
import java.util.Date

class AddEditSessionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val sessionDao = database.sessionDao()
    private val studentDao = database.studentDao()
    private val classTypeDao = database.classTypeDao()

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _classTypes = MutableLiveData<List<ClassType>>()
    val classTypes: LiveData<List<ClassType>> = _classTypes

    private val _session = MutableLiveData<Session>()
    val session: LiveData<Session> = _session

    init {
        loadStudents()
    }

    private fun loadStudents() {
        viewModelScope.launch {
            studentDao.getAllStudents().observeForever { studentList ->
                _students.value = studentList
            }
        }
    }

    fun loadClassTypesForStudent(studentId: Long) {
        viewModelScope.launch {
            classTypeDao.getClassTypesByStudentId(studentId).observeForever { classTypeList ->
                _classTypes.value = classTypeList
            }
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            sessionDao.getSessionById(sessionId).observeForever { session ->
                _session.value = session
                session?.let {
                    loadClassTypesForStudent(it.studentId)
                }
            }
        }
    }

    fun saveSession(
        studentId: Long,
        classTypeId: Long,
        date: Date,
        durationMinutes: Int,
        amount: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val session = _session.value?.copy(
                studentId = studentId,
                classTypeId = classTypeId,
                date = date,
                                                                                                                            durationMinutes = durationMinutes,
                amount = amount,
                notes = notes
            ) ?: Session(
                studentId = studentId,
                classTypeId = classTypeId,
                date = date,
                durationMinutes = durationMinutes,
                amount = amount,
                notes = notes
            )
            sessionDao.insertSession(session)
        }
    }
} 