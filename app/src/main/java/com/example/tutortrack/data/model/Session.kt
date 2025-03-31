package com.example.tutortrack.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassType::class,
            parentColumns = ["id"],
            childColumns = ["classTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val classTypeId: Long,
    val date: Date,
    val durationMinutes: Int,
    val isPaid: Boolean = false,
    val paidDate: Date? = null,
    val amount: Double,
    val notes: String = ""
) : Parcelable 