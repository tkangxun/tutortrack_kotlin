package com.example.tutortrack.data.model

import androidx.room.Embedded
import com.example.tutortrack.ui.adapters.SessionWithDetails
import java.util.Date

data class SessionWithStudentAndClassType(
    @Embedded val session: Session,
    val studentName: String?,
    val classTypeName: String?
) {
    fun toSessionWithDetails(): SessionWithDetails {
        return SessionWithDetails(
            session = session,
            student = if (studentName != null) {
                Student(
                    id = session.studentId,
                    name = studentName,
                    phone = "",
                    grade = "",
                    notes = ""
                )
            } else null,
            classType = if (classTypeName != null) {
                ClassType(
                    id = session.classTypeId,
                    studentId = session.studentId,
                    name = classTypeName,
                    hourlyRate = 0.0  // This field isn't needed for display
                )
            } else null
        )
    }
} 