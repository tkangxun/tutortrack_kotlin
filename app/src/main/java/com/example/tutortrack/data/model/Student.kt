package com.example.tutortrack.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val grade: String,
    val parentName: String = "",
    val parentContact: String = "",
    val notes: String = "",
    val isArchived: Boolean = false
) : Parcelable 