package com.example.tutortrack.ui.reports

import android.app.Application
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutortrack.data.model.ImportResult
import com.example.tutortrack.data.service.CsvImportService
import com.example.tutortrack.data.service.ExcelImportService
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "ImportViewModel"

class ImportViewModel(application: Application) : AndroidViewModel(application) {
    
    private val excelImportService = ExcelImportService(application)
    private val csvImportService = CsvImportService(application)
    
    private val _importResult = MutableLiveData<ImportResult>()
    val importResult: LiveData<ImportResult> = _importResult
    
    private val _isImporting = MutableLiveData<Boolean>()
    val isImporting: LiveData<Boolean> = _isImporting
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // URI of the current import file
    private var currentImportUri: Uri? = null
    
    /**
     * Import sessions from a file (Excel or CSV) - Preview mode first
     * @param uri URI of the file to import
     */
    fun previewImportFile(uri: Uri) {
        Log.d(TAG, "Starting preview of import file: $uri")
        _isImporting.value = true
        currentImportUri = uri
        
        viewModelScope.launch {
            try {
                val result = when {
                    isCsvFile(uri) -> {
                        Log.d(TAG, "Detected CSV file, previewing...")
                        csvImportService.previewCsvFile(uri)
                    }
                    isExcelFile(uri) -> {
                        Log.d(TAG, "Detected Excel file, previewing...")
                        excelImportService.previewExcelFile(uri)
                    }
                    else -> {
                        Log.w(TAG, "Unsupported file type")
                        _errorMessage.postValue("Unsupported file type. Please use Excel (.xls) or CSV files.")
                        ImportResult(errors = listOf("Unsupported file type"))
                    }
                }
                Log.d(TAG, "Preview complete. Found ${result.totalSessionsRead} sessions")
                _importResult.postValue(result.copy(isPreview = true))
            } catch (e: Exception) {
                Log.e(TAG, "Error previewing file", e)
                _errorMessage.postValue("Error previewing file: ${e.message}")
            } finally {
                _isImporting.postValue(false)
            }
        }
    }
    
    /**
     * Confirm and proceed with the import after preview
     */
    fun confirmImport() {
        Log.d(TAG, "Confirming import...")
        if (currentImportUri == null) {
            Log.w(TAG, "No file selected for import")
            _errorMessage.postValue("No file selected for import.")
            return
        }
        
        _isImporting.value = true
        
        viewModelScope.launch {
            try {
                val uri = currentImportUri!!
                val result = when {
                    isCsvFile(uri) -> {
                        Log.d(TAG, "Importing CSV file...")
                        csvImportService.importCsvFile(uri)
                    }
                    isExcelFile(uri) -> {
                        Log.d(TAG, "Importing Excel file...")
                        excelImportService.importExcelFile(uri)
                    }
                    else -> {
                        Log.w(TAG, "Unsupported file type")
                        _errorMessage.postValue("Unsupported file type. Please use Excel (.xls) or CSV files.")
                        ImportResult(errors = listOf("Unsupported file type"))
                    }
                }
                Log.d(TAG, "Import complete. Successfully imported ${result.validSessionsImported} sessions")
                _importResult.postValue(result.copy(isConfirmed = true))
            } catch (e: Exception) {
                Log.e(TAG, "Error importing file", e)
                _errorMessage.postValue("Error importing file: ${e.message}")
            } finally {
                _isImporting.postValue(false)
                currentImportUri = null
            }
        }
    }
    
    /**
     * Cancel the import process
     */
    fun cancelImport() {
        Log.d(TAG, "Cancelling import")
        currentImportUri = null
        resetImport()
    }
    
    /**
     * Check if the URI points to a CSV file
     */
    private fun isCsvFile(uri: Uri): Boolean {
        val mimeType = getApplication<Application>().contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val uriPath = uri.path?.lowercase() ?: ""
        
        val isCSV = extension == "csv" || 
               mimeType == "text/csv" || 
               uriPath.endsWith(".csv") ||
               mimeType == "text/plain" // Some CSV files might be identified as plain text
        
        Log.d(TAG, "File check: isCsvFile=$isCSV, mimeType=$mimeType, extension=$extension, path=$uriPath")
        return isCSV
    }
    
    /**
     * Check if the URI points to an Excel file
     */
    private fun isExcelFile(uri: Uri): Boolean {
        val mimeType = getApplication<Application>().contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val uriPath = uri.path?.lowercase() ?: ""
        
        val isExcel = extension == "xls" || 
               mimeType == "application/vnd.ms-excel" || 
               uriPath.endsWith(".xls")
               
        Log.d(TAG, "File check: isExcelFile=$isExcel, mimeType=$mimeType, extension=$extension, path=$uriPath")
        return isExcel
    }
    
    /**
     * Reset import state
     */
    fun resetImport() {
        Log.d(TAG, "Resetting import state")
        _importResult.value = ImportResult()
        _errorMessage.value = null
    }
} 