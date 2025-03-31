package com.example.tutortrack.data.repository

import androidx.lifecycle.LiveData
import com.example.tutortrack.data.dao.ClassTypeDao
import com.example.tutortrack.data.model.ClassType

class ClassTypeRepository(private val classTypeDao: ClassTypeDao) {

    val allClassTypes: LiveData<List<ClassType>> = classTypeDao.getAllClassTypes()

    suspend fun insertClassType(classType: ClassType): Long {
        return classTypeDao.insertClassType(classType)
    }

    suspend fun updateClassType(classType: ClassType) {
        classTypeDao.updateClassType(classType)
    }

    suspend fun deleteClassType(classType: ClassType) {
        classTypeDao.deleteClassType(classType)
    }

    fun getClassTypeById(id: Long): LiveData<ClassType> {
        return classTypeDao.getClassTypeById(id)
    }

    fun getClassTypesByStudentId(studentId: Long): LiveData<List<ClassType>> {
        return classTypeDao.getClassTypesByStudentId(studentId)
    }
    
    /**
     * Delete class types that have been removed during editing
     * @param studentId The ID of the student
     * @param keepIds The list of IDs of class types to keep
     */
    suspend fun deleteRemovedClassTypes(studentId: Long, keepIds: List<Long>) {
        classTypeDao.deleteRemovedClassTypes(studentId, keepIds)
    }
} 