package com.example.tutortrack.ui.classtype

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.tutortrack.data.AppDatabase
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.data.repository.ClassTypeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClassTypeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClassTypeRepository
    val allClassTypes: LiveData<List<ClassType>>

    init {
        val classTypeDao = AppDatabase.getDatabase(application).classTypeDao()
        repository = ClassTypeRepository(classTypeDao)
        allClassTypes = repository.allClassTypes
    }

    fun insertClassType(classType: ClassType) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertClassType(classType)
    }

    fun updateClassType(classType: ClassType) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateClassType(classType)
    }

    fun deleteClassType(classType: ClassType) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteClassType(classType)
    }

    fun getClassTypeById(id: Long): LiveData<ClassType> {
        return repository.getClassTypeById(id)
    }

    fun getClassTypesByStudentId(studentId: Long): LiveData<List<ClassType>> {
        return repository.getClassTypesByStudentId(studentId)
    }
    
    /**
     * Delete class types that have been removed during editing
     * @param studentId The ID of the student
     * @param keepIds The list of IDs of class types to keep
     */
    fun deleteRemovedClassTypes(studentId: Long, keepIds: List<Long>) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteRemovedClassTypes(studentId, keepIds)
    }
} 