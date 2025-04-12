package com.example.tutortrack.data.model

import kotlinx.serialization.Serializable
import java.util.Date

/**
 * Data class representing a session row from an Excel import.
 * This is used to store data from Excel before converting to actual Session objects.
 */
@Serializable
data class ExcelSessionImport(
    val date: String = "",
    val studentName: String = "",
    val classTypeName: String = "",
    val durationHours: Double = 0.0,
    val amount: Double = 0.0,
    val isPaid: Boolean = false,
    val paidDate: String = "",
    val notes: String = ""
) {
    // Validate if this row has the minimum required data
    fun isValid(): Boolean {
        return date.isNotBlank() && 
               studentName.isNotBlank() && 
               classTypeName.isNotBlank() && 
               durationHours > 0.0
    }
}

/**
 * Container for import results to track what was imported
 */
@Serializable
data class ImportResult(
    val totalSessionsRead: Int = 0,
    val validSessionsImported: Int = 0,
    val newStudentsCreated: List<String> = emptyList(),
    val newClassTypesCreated: List<String> = emptyList(),
    val duplicateSessions: List<ExcelSessionImport> = emptyList(),
    val invalidSessions: List<ExcelSessionImport> = emptyList(),
    val errors: List<String> = emptyList(),
    val pendingSessions: List<ExcelSessionImport> = emptyList(),
    val isPreview: Boolean = false,
    val isConfirmed: Boolean = false
) 