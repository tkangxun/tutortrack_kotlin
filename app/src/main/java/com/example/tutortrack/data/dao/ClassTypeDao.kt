package com.example.tutortrack.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tutortrack.data.model.ClassType

@Dao
interface ClassTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassType(classType: ClassType): Long

    @Update
    suspend fun updateClassType(classType: ClassType)

    @Delete
    suspend fun deleteClassType(classType: ClassType)

    @Query("SELECT * FROM class_types ORDER BY name ASC")
    fun getAllClassTypes(): LiveData<List<ClassType>>

    @Query("SELECT * FROM class_types WHERE id = :classTypeId")
    fun getClassTypeById(classTypeId: Long): LiveData<ClassType>

    @Query("SELECT * FROM class_types WHERE studentId = :studentId ORDER BY name ASC")
    fun getClassTypesByStudentId(studentId: Long): LiveData<List<ClassType>>
    
    /**
     * Delete class types that have been removed during editing
     * Keeps only the class types with IDs in the keepIds list for a specific student
     */
    @Query("DELETE FROM class_types WHERE studentId = :studentId AND id NOT IN (:keepIds)")
    suspend fun deleteRemovedClassTypes(studentId: Long, keepIds: List<Long>)
} 