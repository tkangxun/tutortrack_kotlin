package com.example.tutortrack.ui.student

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

class StudentDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val studentDao = database.studentDao()
    private val sessionDao = database.sessionDao()
    private val classTypeDao = database.classTypeDao()

    private val _student = MutableLiveData<Student>()
    val student: LiveData<Student> = _student

    private val _sessions = MutableLiveData<List<Session>>()
    val sessions: LiveData<List<Session>> = _sessions

    private val _classTypes = MutableLiveData<List<ClassType>>()
    val classTypes: LiveData<List<ClassType>> = _classTypes

    fun loadStudent(studentId: Long) {
        viewModelScope.launch {
            studentDao.getStudentById(studentId).observeForever { student ->
                _student.value = student
                student?.let {
                    loadSessions(it.id)
                    loadClassTypes(it.id)
                }
            }
        }
    }

    private fun loadSessions(studentId: Long) {
        viewModelScope.launch {
            sessionDao.getSessionsByStudentId(studentId).observeForever { sessionList ->
                _sessions.value = sessionList
            }
        }
    }

    private fun loadClassTypes(studentId: Long) {
        viewModelScope.launch {
            classTypeDao.getClassTypesByStudentId(studentId).observeForever { classTypeList ->
                _classTypes.value = classTypeList
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            studentDao.deleteStudent(student)
        }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            sessionDao.deleteSession(session)
        }
    }

    fun deleteClassType(classType: ClassType) {
        viewModelScope.launch {
            classTypeDao.deleteClassType(classType)
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            studentDao.updateStudent(student)
        }
    }

    fun updateSession(session: Session) {
        viewModelScope.launch {
            sessionDao.updateSession(session)
        }
    }

    fun updateClassType(classType: ClassType) {
        viewModelScope.launch {
            classTypeDao.updateClassType(classType)
        }
    }

    fun addSession(session: Session) {
        viewModelScope.launch {
            sessionDao.insertSession(session)
        }
    }

    fun addClassType(classType: ClassType) {
        viewModelScope.launch {
            classTypeDao.insertClassType(classType)
        }
    }
} 