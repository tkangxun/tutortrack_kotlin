package com.example.tutortrack.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.tutortrack.data.AppDatabase
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.data.model.ExcelSessionImport
import com.example.tutortrack.data.model.ImportResult
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.Student
import jxl.Sheet
import jxl.Workbook
import jxl.read.biff.BiffException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Service for importing Excel files with session data
 */
class ExcelImportService(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val studentDao = database.studentDao()
    private val classTypeDao = database.classTypeDao()
    private val sessionDao = database.sessionDao()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Column indices for expected Excel format
    private val DATE_COL = 0
    private val STUDENT_COL = 1
    private val CLASS_TYPE_COL = 2
    private val DURATION_COL = 3
    private val AMOUNT_COL = 4
    private val PAID_COL = 5
    private val PAID_DATE_COL = 6
    private val NOTES_COL = 7
    
    // Expected header row for validation
    private val EXPECTED_HEADERS = listOf(
        "Date", "Student", "Class Type", "Duration (hours)", 
        "Amount", "Paid", "Payment Date", "Notes"
    )
    
    /**
     * Import sessions from an Excel file
     * @param uri URI of the Excel file to import
     * @return Result of the import process
     */
    suspend fun importExcelFile(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val result = ImportResult()
        val resultBuilder = ImportResultBuilder(result)
        
        try {
            // Open the file using JXL
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = Workbook.getWorkbook(inputStream)
                val sheet = workbook.getSheet(0)
                
                // Validate headers
                if (!validateHeaders(sheet)) {
                    return@withContext resultBuilder
                        .addError("Invalid Excel format. Headers do not match expected format.")
                        .build()
                }
                
                // Process data rows
                val sessions = mutableListOf<ExcelSessionImport>()
                val rowCount = sheet.rows
                
                // Skip header row (i=0)
                for (i in 1 until rowCount) {
                    val session = readSessionFromRow(sheet, i)
                    resultBuilder.incrementTotalSessions()
                    
                    if (session.isValid()) {
                        sessions.add(session)
                    } else {
                        resultBuilder.addInvalidSession(session)
                    }
                }
                
                // Process valid sessions
                processValidSessions(sessions, resultBuilder)
                
                // Close the workbook
                workbook.close()
            }
        } catch (e: BiffException) {
            Log.e("ExcelImportService", "Error importing Excel file - not a valid Excel file", e)
            resultBuilder.addError("The file is not a valid Excel file. Please ensure you're using an .xls file format.")
        } catch (e: Exception) {
            Log.e("ExcelImportService", "Error importing Excel file", e)
            resultBuilder.addError("Error reading Excel file: ${e.message}")
        }
        
        return@withContext resultBuilder.build()
    }
    
    /**
     * Preview sessions from an Excel file without actually importing
     * @param uri URI of the Excel file to preview
     * @return Result of the preview process
     */
    suspend fun previewExcelFile(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val result = ImportResult()
        val resultBuilder = ImportResultBuilder(result, isPreview = true)
        
        try {
            // Open the file using JXL
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = Workbook.getWorkbook(inputStream)
                val sheet = workbook.getSheet(0)
                
                // Validate headers
                if (!validateHeaders(sheet)) {
                    return@withContext resultBuilder
                        .addError("Invalid Excel format. Headers do not match expected format.")
                        .build()
                }
                
                // Process data rows
                val sessions = mutableListOf<ExcelSessionImport>()
                val rowCount = sheet.rows
                
                // Skip header row (i=0)
                for (i in 1 until rowCount) {
                    val session = readSessionFromRow(sheet, i)
                    resultBuilder.incrementTotalSessions()
                    
                    if (session.isValid()) {
                        sessions.add(session)
                    } else {
                        resultBuilder.addInvalidSession(session)
                    }
                }
                
                // Preview the data - don't actually save anything
                previewSessions(sessions, resultBuilder)
                
                // Close the workbook
                workbook.close()
            }
        } catch (e: BiffException) {
            Log.e("ExcelImportService", "Error previewing Excel file - not a valid Excel file", e)
            resultBuilder.addError("The file is not a valid Excel file. Please ensure you're using an .xls file format.")
        } catch (e: Exception) {
            Log.e("ExcelImportService", "Error previewing Excel file", e)
            resultBuilder.addError("Error reading Excel file: ${e.message}")
        }
        
        return@withContext resultBuilder.build()
    }
    
    /**
     * Validate that the header row matches expected format
     */
    private fun validateHeaders(sheet: Sheet): Boolean {
        if (sheet.rows == 0) return false
        
        val actualHeaders = (0 until sheet.columns).mapNotNull { col ->
            sheet.getCell(col, 0).contents?.trim()
        }
        
        // Check if all expected headers are present
        return EXPECTED_HEADERS.all { expected ->
            actualHeaders.any { actual -> 
                actual.equals(expected, ignoreCase = true)
            }
        }
    }
    
    /**
     * Read session data from a row in the Excel file
     */
    private fun readSessionFromRow(sheet: Sheet, rowIndex: Int): ExcelSessionImport {
        try {
            // Safely get cell content, returning empty string if cell is out of bounds
            fun getCellContent(col: Int, row: Int): String {
                return try {
                    sheet.getCell(col, row).contents.trim()
                } catch (e: Exception) {
                    ""
                }
            }
            
            // Read date
            val dateStr = getCellContent(DATE_COL, rowIndex)
            
            // Read student name
            val studentName = getCellContent(STUDENT_COL, rowIndex)
            
            // Read class type
            val classTypeName = getCellContent(CLASS_TYPE_COL, rowIndex)
            
            // Read duration (hours)
            val durationHours = getCellContent(DURATION_COL, rowIndex).toDoubleOrNull() ?: 0.0
            
            // Read amount
            val amountStr = getCellContent(AMOUNT_COL, rowIndex)
            val amount = amountStr.replace("$", "").toDoubleOrNull() ?: 0.0
            
            // Read paid status
            val paidStr = getCellContent(PAID_COL, rowIndex)
            val isPaid = paidStr.equals("Yes", ignoreCase = true) || paidStr.equals("true", ignoreCase = true)
            
            // Read payment date
            val paidDate = getCellContent(PAID_DATE_COL, rowIndex)
            
            // Read notes
            val notes = getCellContent(NOTES_COL, rowIndex)
            
            return ExcelSessionImport(
                date = dateStr,
                studentName = studentName,
                classTypeName = classTypeName,
                durationHours = durationHours,
                amount = amount,
                isPaid = isPaid,
                paidDate = paidDate,
                notes = notes
            )
        } catch (e: Exception) {
            Log.e("ExcelImportService", "Error reading row data", e)
            return ExcelSessionImport()
        }
    }
    
    /**
     * Process valid sessions and import them
     */
    private suspend fun processValidSessions(
        sessions: List<ExcelSessionImport>,
        resultBuilder: ImportResultBuilder
    ) {
        // Create a map to store created students during this import to prevent duplicates within the same import
        val createdStudentsMap = mutableMapOf<String, Student>() // normalized name -> Student
        
        // Process each session
        for (excelSession in sessions) {
            try {
                // Get existing students - do this for each session to catch any new students added during import
                val existingStudents = studentDao.getAllStudentsSync()
                val studentMap = existingStudents.associateBy { normalizeStudentName(it.name) }
                
                // Get or create student using transaction to prevent race conditions
                val studentName = excelSession.studentName
                val normalizedName = normalizeStudentName(studentName)
                
                // First check the map of students we already created in this import
                val student = createdStudentsMap[normalizedName] ?: findOrCreateStudent(
                    studentName, 
                    normalizedName, 
                    studentMap, 
                    resultBuilder
                )
                
                // Add to our local cache if it's a new student
                if (!createdStudentsMap.containsKey(normalizedName)) {
                    createdStudentsMap[normalizedName] = student
                }
                
                // Get or create class type
                val classTypes = classTypeDao.getClassTypesByStudentIdSync(student.id)
                val classTypeMap = classTypes.associateBy { it.name.lowercase() }
                val classType = getOrCreateClassType(
                    excelSession.classTypeName, 
                    student.id, 
                    excelSession.amount / excelSession.durationHours,
                    classTypeMap,
                    resultBuilder
                )
                
                // Parse date
                val sessionDate = parseDate(excelSession.date)
                
                // Check for existing session with same date, student and class type
                val existingSessions = sessionDao.getSessionsByStudentIdSync(student.id)
                val isDuplicate = existingSessions.any { existingSession ->
                    val sameDate = isSameDate(existingSession.date, sessionDate)
                    val sameClassType = existingSession.classTypeId == classType.id
                    val currentDurationMinutes = (excelSession.durationHours * 60).roundToInt()
                    sameDate && sameClassType && existingSession.durationMinutes == currentDurationMinutes
                }
                
                if (isDuplicate) {
                    resultBuilder.addDuplicateSession(excelSession)
                    continue
                }
                
                // Create and save session
                val session = Session(
                    studentId = student.id,
                    classTypeId = classType.id,
                    date = sessionDate,
                    durationMinutes = (excelSession.durationHours * 60).roundToInt(),
                    isPaid = excelSession.isPaid,
                    paidDate = if (excelSession.isPaid) parseDate(excelSession.paidDate) else null,
                    amount = excelSession.amount,
                    notes = excelSession.notes
                )
                
                sessionDao.insertSession(session)
                resultBuilder.incrementValidSessionsImported()
            } catch (e: Exception) {
                Log.e("ExcelImportService", "Error processing session", e)
                resultBuilder.addError("Error processing session for ${excelSession.studentName}: ${e.message}")
            }
        }
    }
    
    /**
     * Normalize a student name for comparison and storage
     */
    private fun normalizeStudentName(name: String): String {
        return name.trim().lowercase().replace(Regex("\\s+"), " ")
    }
    
    /**
     * Find an existing student or create a new one with extra checks
     * to prevent duplicates even in concurrent scenarios
     */
    private suspend fun findOrCreateStudent(
        originalName: String, 
        normalizedName: String,
        studentMap: Map<String, Student>,
        resultBuilder: ImportResultBuilder
    ): Student {
        // First check exact match by normalized name
        val exactMatch = studentMap[normalizedName]
        if (exactMatch != null) {
            return exactMatch
        }
        
        // If no exact match, try a more flexible approach:
        // Check if the normalized name is similar to any existing student name
        val similarStudent = studentMap.entries.firstOrNull { (key, _) ->
            // Check if the normalized names are very similar
            val similarity = calculateNameSimilarity(normalizedName, key)
            similarity > 0.9 // 90% similarity threshold
        }?.value
        
        if (similarStudent != null) {
            // Found a similar student, use the existing one
            Log.d("ExcelImportService", "Found similar student: '${similarStudent.name}' for '$originalName'")
            return similarStudent
        }
        
        // One more check right before creating - check if another thread created this student already
        val finalCheck = studentDao.getStudentByNameContainingSync(originalName.trim())
        if (finalCheck.isNotEmpty()) {
            Log.d("ExcelImportService", "Found student in final check: '${finalCheck[0].name}' for '$originalName'")
            return finalCheck[0]
        }
        
        // Create new student
        val newStudent = Student(
            name = originalName.trim(), // Use trimmed original name to preserve case
            grade = "Unknown", // Default value, as we don't have this in the Excel
            phone = "",
            parentName = "",
            parentContact = "",
            notes = "Created from Excel import"
        )
        
        val id = studentDao.insertStudent(newStudent)
        resultBuilder.addNewStudent(originalName.trim())
        
        return newStudent.copy(id = id)
    }
    
    /**
     * Calculate similarity between two normalized names
     * Returns a value between 0 (completely different) and 1 (identical)
     */
    private fun calculateNameSimilarity(name1: String, name2: String): Double {
        // If strings are identical, return 1.0
        if (name1 == name2) return 1.0
        
        // Split names into words
        val words1 = name1.split(" ")
        val words2 = name2.split(" ")
        
        // Quick check: if word count is different by more than 1, less likely to be the same person
        if (Math.abs(words1.size - words2.size) > 1) return 0.5
        
        // Count matching words
        var matchCount = 0
        for (word1 in words1) {
            if (words2.any { it == word1 || (it.length > 2 && word1.length > 2 && 
                (it.startsWith(word1) || word1.startsWith(it))) }) {
                matchCount++
            }
        }
        
        // Calculate similarity as percentage of matched words relative to total unique words
        val uniqueWords = (words1 + words2).toSet().size
        return matchCount.toDouble() / uniqueWords
    }
    
    /**
     * Get existing class type or create a new one
     */
    private suspend fun getOrCreateClassType(
        name: String,
        studentId: Long,
        hourlyRate: Double,
        classTypeMap: Map<String, ClassType>,
        resultBuilder: ImportResultBuilder
    ): ClassType {
        val normalizedName = name.lowercase()
        
        // Check if class type exists
        val existingClassType = classTypeMap[normalizedName]
        if (existingClassType != null) {
            return existingClassType
        }
        
        // Create new class type
        val effectiveRate = if (hourlyRate > 0) hourlyRate else 0.0
        val newClassType = ClassType(
            studentId = studentId,
            name = name,
            hourlyRate = effectiveRate
        )
        
        val id = classTypeDao.insertClassType(newClassType)
        resultBuilder.addNewClassType(name)
        
        return newClassType.copy(id = id)
    }
    
    /**
     * Parse date string to Date object
     */
    private fun parseDate(dateStr: String): Date {
        return try {
            dateFormat.parse(dateStr) ?: Calendar.getInstance().time
        } catch (e: Exception) {
            Calendar.getInstance().time
        }
    }
    
    /**
     * Check if two dates are the same (ignoring time)
     */
    private fun isSameDate(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.time = date1
        cal1.set(Calendar.HOUR_OF_DAY, 0)
        cal1.set(Calendar.MINUTE, 0)
        cal1.set(Calendar.SECOND, 0)
        cal1.set(Calendar.MILLISECOND, 0)
        
        val cal2 = Calendar.getInstance()
        cal2.time = date2
        cal2.set(Calendar.HOUR_OF_DAY, 0)
        cal2.set(Calendar.MINUTE, 0)
        cal2.set(Calendar.SECOND, 0)
        cal2.set(Calendar.MILLISECOND, 0)
        
        return cal1.timeInMillis == cal2.timeInMillis
    }
    
    /**
     * Generate preview information for sessions without saving
     */
    private suspend fun previewSessions(
        sessions: List<ExcelSessionImport>,
        resultBuilder: ImportResultBuilder
    ) {
        // Get existing students
        val existingStudents = studentDao.getAllStudentsSync()
        val studentMap = existingStudents.associateBy { normalizeStudentName(it.name) }
        
        // Collect new students and class types that would be created
        val newStudentsMap = mutableMapOf<String, String>() // normalized name -> display name
        val newClassTypesMap = mutableMapOf<String, String>() // normalized name -> display name
        
        // Process each session for preview
        for (excelSession in sessions) {
            try {
                // Check if student already exists
                val studentName = excelSession.studentName
                val normalizedStudentName = normalizeStudentName(studentName)
                
                // First try exact match
                val exactStudentMatch = studentMap[normalizedStudentName]
                
                // If no exact match, check for similar names
                val similarStudent = if (exactStudentMatch == null) {
                    studentMap.entries.firstOrNull { (key, _) ->
                        val similarity = calculateNameSimilarity(normalizedStudentName, key)
                        similarity > 0.9
                    }?.value
                } else null
                
                // Also check for loose matches in the database
                val looseMatch = if (exactStudentMatch == null && similarStudent == null) {
                    val matches = studentDao.getStudentByNameContainingSync(studentName.trim())
                    if (matches.isNotEmpty()) matches[0] else null
                } else null
                
                // Determine if we need to create a new student
                val existingStudent = exactStudentMatch ?: similarStudent ?: looseMatch
                if (existingStudent == null && !newStudentsMap.containsKey(normalizedStudentName)) {
                    // Will need to create a new student
                    newStudentsMap[normalizedStudentName] = studentName.trim()
                    resultBuilder.addNewStudent(studentName.trim())
                }
                
                if (existingStudent != null) {
                    // Check if class type exists
                    val classTypes = classTypeDao.getClassTypesByStudentIdSync(existingStudent.id)
                    val classTypeMap = classTypes.associateBy { it.name.lowercase() }
                    
                    val classTypeName = excelSession.classTypeName
                    val normalizedClassTypeName = classTypeName.lowercase()
                    
                    if (!classTypeMap.containsKey(normalizedClassTypeName)) {
                        // Will need to create a new class type for existing student
                        val key = "${normalizedStudentName}:${normalizedClassTypeName}"
                        if (!newClassTypesMap.containsKey(key)) {
                            newClassTypesMap[key] = classTypeName
                            resultBuilder.addNewClassType(classTypeName)
                        }
                    }
                    
                    // Check for duplicate sessions
                    val sessionDate = parseDate(excelSession.date)
                    val existingSessions = sessionDao.getSessionsByStudentIdSync(existingStudent.id)
                    val isDuplicate = existingSessions.any { existingSession ->
                        val classType = classTypeMap[normalizedClassTypeName]
                        val sameDate = isSameDate(existingSession.date, sessionDate)
                        val sameClassType = classType != null && existingSession.classTypeId == classType.id
                        val currentDurationMinutes = (excelSession.durationHours * 60).roundToInt()
                        sameDate && sameClassType && existingSession.durationMinutes == currentDurationMinutes
                    }
                    
                    if (isDuplicate) {
                        resultBuilder.addDuplicateSession(excelSession)
                    } else {
                        resultBuilder.addPendingSession(excelSession)
                    }
                } else {
                    // Brand new student will need class type too
                    val classTypeName = excelSession.classTypeName
                    val key = "${normalizedStudentName}:${classTypeName.lowercase()}"
                    if (!newClassTypesMap.containsKey(key)) {
                        newClassTypesMap[key] = classTypeName
                        resultBuilder.addNewClassType(classTypeName)
                    }
                    
                    // This is definitely a new session
                    resultBuilder.addPendingSession(excelSession)
                }
                
            } catch (e: Exception) {
                Log.e("ExcelImportService", "Error previewing session", e)
                resultBuilder.addError("Error processing session preview for ${excelSession.studentName}: ${e.message}")
            }
        }
    }
    
    /**
     * Helper class to build ImportResult
     */
    private class ImportResultBuilder(initialResult: ImportResult = ImportResult(), private val isPreview: Boolean = false) {
        private var totalSessionsRead = initialResult.totalSessionsRead
        private var validSessionsImported = initialResult.validSessionsImported
        private val newStudentsCreated = initialResult.newStudentsCreated.toMutableList()
        private val newClassTypesCreated = initialResult.newClassTypesCreated.toMutableList()
        private val duplicateSessions = initialResult.duplicateSessions.toMutableList()
        private val invalidSessions = initialResult.invalidSessions.toMutableList()
        private val pendingSessions = initialResult.pendingSessions.toMutableList()
        private val errors = initialResult.errors.toMutableList()
        
        fun incrementTotalSessions() = apply { totalSessionsRead++ }
        fun incrementValidSessionsImported() = apply { validSessionsImported++ }
        fun addNewStudent(name: String) = apply { newStudentsCreated.add(name) }
        fun addNewClassType(name: String) = apply { newClassTypesCreated.add(name) }
        fun addDuplicateSession(session: ExcelSessionImport) = apply { duplicateSessions.add(session) }
        fun addInvalidSession(session: ExcelSessionImport) = apply { invalidSessions.add(session) }
        fun addPendingSession(session: ExcelSessionImport) = apply { pendingSessions.add(session) }
        fun addError(error: String) = apply { errors.add(error) }
        
        fun build() = ImportResult(
            totalSessionsRead = totalSessionsRead,
            validSessionsImported = validSessionsImported,
            newStudentsCreated = newStudentsCreated,
            newClassTypesCreated = newClassTypesCreated,
            duplicateSessions = duplicateSessions,
            invalidSessions = invalidSessions,
            pendingSessions = pendingSessions,
            errors = errors,
            isPreview = isPreview
        )
    }
} 