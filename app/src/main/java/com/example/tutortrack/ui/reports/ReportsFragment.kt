package com.example.tutortrack.ui.reports

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.tutortrack.R
import com.example.tutortrack.databinding.FragmentReportsBinding
import com.example.tutortrack.ui.session.SessionViewModel
import com.example.tutortrack.ui.student.StudentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.TextView
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.LinearLayout

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var importViewModel: ImportViewModel
    
    private val startDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1) // First day of current month
    }
    
    private val endDate = Calendar.getInstance() // Today
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val defaultTemplate = """Dear [parentName],

Please pay the following outstanding payment for [studentName]:

[[SessionDate] - [classTypeName] ([sessionDuration] hrs) [sessionIncome]]

Total: [Total unpaid sessions]"""

    // Activity result launcher for file selection
    private val filePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleFileSelection(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModels()
        setupDatePickers()
        setupPresetFilters()
        loadTotalIncome()
        loadStatistics()
        setupListeners()
        loadInvoiceTemplate()
        observeImportResults()
    }
    
    private fun setupViewModels() {
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
        importViewModel = ViewModelProvider(this)[ImportViewModel::class.java]
    }
    
    private fun setupDatePickers() {
        binding.editStartDate.setText(dateFormat.format(startDate.time))
        binding.editEndDate.setText(dateFormat.format(endDate.time))
        
        binding.editStartDate.setOnClickListener {
            showDatePicker(startDate) { newDate ->
                startDate.time = newDate
                binding.editStartDate.setText(dateFormat.format(startDate.time))
                updateRangeIncome()
            }
        }
        
        binding.editEndDate.setOnClickListener {
            showDatePicker(endDate) { newDate ->
                endDate.time = newDate
                binding.editEndDate.setText(dateFormat.format(endDate.time))
                updateRangeIncome()
            }
        }
    }
    
    private fun setupPresetFilters() {
        val calendar = Calendar.getInstance()
        
        // Two months ago
        calendar.add(Calendar.MONTH, -2)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val twoMonthsAgoStart = calendar.time
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val twoMonthsAgoEnd = calendar.time
        
        // Last month
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val lastMonthStart = calendar.time
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val lastMonthEnd = calendar.time
        
        // Current month
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val currentMonthStart = calendar.time
        calendar.time = Date() // Today
        val currentMonthEnd = calendar.time
        
        // Set up preset buttons
        binding.buttonTwoMonthsAgo.setOnClickListener {
            startDate.time = twoMonthsAgoStart
            endDate.time = twoMonthsAgoEnd
            updateDateDisplay()
            updateRangeIncome()
        }
        
        binding.buttonLastMonth.setOnClickListener {
            startDate.time = lastMonthStart
            endDate.time = lastMonthEnd
            updateDateDisplay()
            updateRangeIncome()
        }
        
        binding.buttonCurrentMonth.setOnClickListener {
            startDate.time = currentMonthStart
            endDate.time = currentMonthEnd
            updateDateDisplay()
            updateRangeIncome()
        }
        
        // Set button text
        binding.buttonTwoMonthsAgo.text = monthFormat.format(twoMonthsAgoStart)
        binding.buttonLastMonth.text = monthFormat.format(lastMonthStart)
        binding.buttonCurrentMonth.text = monthFormat.format(currentMonthStart)
    }
    
    private fun updateDateDisplay() {
        binding.editStartDate.setText(dateFormat.format(startDate.time))
        binding.editEndDate.setText(dateFormat.format(endDate.time))
    }
    
    private fun showDatePicker(calendar: Calendar, onDateSelected: (Date) -> Unit) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(calendar.time)
        }, year, month, day).show()
    }
    
    private fun loadTotalIncome() {
        sessionViewModel.getTotalIncome().observe(viewLifecycleOwner) { total ->
            binding.textTotalIncome.text = String.format(Locale.getDefault(), "$%.2f", total ?: 0.0)
        }
        
        updateRangeIncome()
    }
    
    private fun updateRangeIncome() {
        sessionViewModel.getIncomeInDateRange(startDate.time, endDate.time)
            .observe(viewLifecycleOwner) { total ->
                binding.textRangeIncome.text = String.format(Locale.getDefault(), "$%.2f", total ?: 0.0)
            }
        
        // Add the unpaid amount for the selected date range
        sessionViewModel.getUnpaidIncomeInDateRange(startDate.time, endDate.time)
            .observe(viewLifecycleOwner) { unpaidAmount ->
                binding.textRangeUnpaidIncome.text = String.format(Locale.getDefault(), "$%.2f", unpaidAmount ?: 0.0)
            }
    }
    
    private fun loadStatistics() {
        studentViewModel.allStudents.observe(viewLifecycleOwner) { students ->
            binding.textTotalStudents.text = students.size.toString()
        }
        
        sessionViewModel.allSessions.observe(viewLifecycleOwner) { sessions ->
            binding.textTotalSessions.text = sessions.size.toString()
            
            val unpaidSessions = sessions.filter { !it.isPaid }
            binding.textUnpaidSessions.text = unpaidSessions.size.toString()
            
            val unpaidAmount = unpaidSessions.sumOf { it.amount }
            binding.textUnpaidAmount.text = String.format(Locale.getDefault(), "$%.2f", unpaidAmount)
        }
    }
    
    private fun setupListeners() {
        binding.buttonApply.setOnClickListener {
            updateRangeIncome()
        }

        binding.buttonEditTemplate.setOnClickListener {
            showEditTemplateDialog()
        }
        
        binding.buttonExport.setOnClickListener {
            exportToCsv()
        }
        
        binding.buttonImport.setOnClickListener {
            showImportOptions()
        }
    }

    private fun loadInvoiceTemplate() {
        val prefs = requireContext().getSharedPreferences("invoice_prefs", 0)
        val template = prefs.getString("invoice_template", defaultTemplate) ?: defaultTemplate
        binding.textTemplatePreview.text = template
    }

    private fun showEditTemplateDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_template, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editTemplate)
        val buttonRestoreDefault = dialogView.findViewById<MaterialButton>(R.id.buttonRestoreDefault)
        val buttonSave = dialogView.findViewById<MaterialButton>(R.id.buttonSave)
        val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.buttonCancel)
        
        val prefs = requireContext().getSharedPreferences("invoice_prefs", 0)
        val currentTemplate = prefs.getString("invoice_template", defaultTemplate) ?: defaultTemplate
        editText.setText(currentTemplate)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        
        buttonRestoreDefault.setOnClickListener {
            editText.setText(defaultTemplate)
        }
        
        buttonSave.setOnClickListener {
            val newTemplate = editText.text.toString()
            prefs.edit().putString("invoice_template", newTemplate).apply()
            binding.textTemplatePreview.text = newTemplate
            dialog.dismiss()
        }
        
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun exportToCsv() {
        val startDate = binding.editStartDate.text.toString()
        val endDate = binding.editEndDate.text.toString()
        
        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a date range first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val startTimestamp = dateFormat.parse(startDate)?.time ?: 0
        val endTimestamp = dateFormat.parse(endDate)?.time ?: 0
        
        Log.d("ReportsFragment", "Exporting CSV for date range: $startDate to $endDate")
        Log.d("ReportsFragment", "Timestamps: $startTimestamp to $endTimestamp")
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sessions = sessionViewModel.getSessionsInDateRange(
                    startDateTimestamp = startTimestamp,
                    endDateTimestamp = endTimestamp
                )
                
                Log.d("ReportsFragment", "Found ${sessions.size} sessions to export")
                
                if (sessions.isEmpty()) {
                    Toast.makeText(requireContext(), "No sessions found in selected date range", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Create directory if it doesn't exist
                val documentsDir = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "TutorTrack")
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs()
                }
                
                // Create the CSV file
                val fileName = "sessions_${startDate.replace(" ", "_").replace(",", "")}_to_${endDate.replace(" ", "_").replace(",", "")}.csv"
                val file = File(documentsDir, fileName)
                
                Log.d("ReportsFragment", "Writing to file: ${file.absolutePath}")
                
                // Write data to the CSV file
                FileWriter(file).use { writer ->
                    // Write headers
                    writer.append("Date,Student,Class Type,Duration (mins),Amount,Paid,Paid Date,Notes\n")
                    
                    // Write session data
                    sessions.forEach { session ->
                        writer.append(csvDateFormat.format(session.session.date)).append(",")
                        writer.append(session.student?.name ?: "Unknown").append(",")
                        writer.append(session.classType?.name ?: "Unknown").append(",")
                        writer.append(session.session.durationMinutes.toString()).append(",")
                        writer.append(session.session.amount.toString()).append(",")
                        writer.append(if (session.session.isPaid) "Yes" else "No").append(",")
                        writer.append(if (session.session.paidDate != null) 
                            csvDateFormat.format(session.session.paidDate!!) else "").append(",")
                        writer.append(session.session.notes ?: "").append("\n")
                    }
                }
                
                // Create content URI using FileProvider
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "com.example.tutortrack.fileprovider",
                    file
                )
                
                Log.d("ReportsFragment", "Content URI: $contentUri")
                
                // Create intent to share the file
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "TutorTrack Sessions Export")
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // Start the sharing activity
                startActivity(Intent.createChooser(intent, "Share CSV File"))
                
                Toast.makeText(requireContext(), "Export successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ReportsFragment", "CSV export error", e)
                e.printStackTrace()
            }
        }
    }

    private fun showImportOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import Options")
            .setMessage("Choose a file format to import sessions")
            .setPositiveButton("Excel or CSV") { dialog, _ ->
                dialog.dismiss()
                filePicker.launch("*/*")
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun handleFileSelection(uri: Uri) {
        importViewModel.previewImportFile(uri)
    }
    
    private fun observeImportResults() {
        // Observe import status
        importViewModel.isImporting.observe(viewLifecycleOwner) { isImporting ->
            binding.progressImport.visibility = if (isImporting) View.VISIBLE else View.GONE
            binding.buttonImport.isEnabled = !isImporting
        }
        
        // Observe import results
        importViewModel.importResult.observe(viewLifecycleOwner) { result ->
            Log.d("ReportsFragment", "Import result received: preview=${result.isPreview}, confirmed=${result.isConfirmed}, " +
                  "sessions=${result.totalSessionsRead}, valid=${result.validSessionsImported}, errors=${result.errors.size}")
            
            if (result.totalSessionsRead > 0) {
                // Show import dialog for both preview and confirmed imports
                showImportResultDialog(result)
            } else if (result.errors.isNotEmpty()) {
                // Only show error dialog if we have errors
                showImportErrorDialog(result.errors)
            }
        }
        
        // Observe error messages
        importViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showImportResultDialog(result: com.example.tutortrack.data.model.ImportResult) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_import_result, null)
        
        // Find views in the dialog
        val textImportSummary = dialogView.findViewById<TextView>(R.id.textImportSummary)
        val textNewStudentsHeader = dialogView.findViewById<TextView>(R.id.textNewStudentsHeader)
        val textNewStudents = dialogView.findViewById<TextView>(R.id.textNewStudents)
        val textNewClassTypesHeader = dialogView.findViewById<TextView>(R.id.textNewClassTypesHeader)
        val textNewClassTypes = dialogView.findViewById<TextView>(R.id.textNewClassTypes)
        val textIssuesHeader = dialogView.findViewById<TextView>(R.id.textIssuesHeader)
        val textIssues = dialogView.findViewById<TextView>(R.id.textIssues)
        val buttonClose = dialogView.findViewById<MaterialButton>(R.id.buttonClose)
        val buttonConfirmImport = dialogView.findViewById<MaterialButton>(R.id.buttonConfirmImport)
        
        // Set summary
        val summaryText = StringBuilder()
        summaryText.append("Total rows read: ${result.totalSessionsRead}\n")
        
        if (result.isConfirmed) {
            summaryText.append("Successfully imported: ${result.validSessionsImported}\n")
        } else {
            summaryText.append("Ready to import: ${result.pendingSessions.size}\n")
        }
        
        if (result.duplicateSessions.isNotEmpty()) {
            summaryText.append("Duplicates to skip: ${result.duplicateSessions.size}\n")
        }
        
        if (result.invalidSessions.isNotEmpty()) {
            summaryText.append("Invalid rows: ${result.invalidSessions.size}\n")
        }
        
        if (result.newStudentsCreated.isNotEmpty()) {
            summaryText.append("New students: ${result.newStudentsCreated.size}\n")
        }
        
        if (result.newClassTypesCreated.isNotEmpty()) {
            summaryText.append("New class types: ${result.newClassTypesCreated.size}\n")
        }
        
        textImportSummary.text = summaryText.toString()
        
        // Show new students if any
        if (result.newStudentsCreated.isNotEmpty()) {
            textNewStudentsHeader.visibility = View.VISIBLE
            val studentsText = result.newStudentsCreated.joinToString("\n") { "• $it" }
            textNewStudents.text = studentsText
        }
        
        // Show new class types if any
        if (result.newClassTypesCreated.isNotEmpty()) {
            textNewClassTypesHeader.visibility = View.VISIBLE
            val classTypesText = result.newClassTypesCreated.joinToString("\n") { "• $it" }
            textNewClassTypes.text = classTypesText
        }
        
        // Show issues if any
        val hasIssues = result.duplicateSessions.isNotEmpty() || 
                        result.invalidSessions.isNotEmpty() || 
                        result.errors.isNotEmpty()
        
        if (hasIssues) {
            textIssuesHeader.visibility = View.VISIBLE
            val issuesText = StringBuilder()
            
            if (result.errors.isNotEmpty()) {
                issuesText.append("Errors:\n")
                result.errors.forEach { issuesText.append("• $it\n") }
                issuesText.append("\n")
            }
            
            if (result.invalidSessions.isNotEmpty()) {
                issuesText.append("Invalid rows (missing required data):\n")
                result.invalidSessions.take(5).forEach { session ->
                    issuesText.append("• ${session.studentName} - ${session.classTypeName}\n")
                }
                
                if (result.invalidSessions.size > 5) {
                    issuesText.append("• ...and ${result.invalidSessions.size - 5} more\n")
                }
                
                issuesText.append("\n")
            }
            
            if (result.duplicateSessions.isNotEmpty()) {
                issuesText.append("Duplicate sessions (skipped):\n")
                result.duplicateSessions.take(5).forEach { session ->
                    issuesText.append("• ${session.date} - ${session.studentName} - ${session.classTypeName}\n")
                }
                
                if (result.duplicateSessions.size > 5) {
                    issuesText.append("• ...and ${result.duplicateSessions.size - 5} more\n")
                }
            }
            
            textIssues.text = issuesText.toString()
        }
        
        // Create and show the dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
            
        // Check if this is a preview or confirmed import
        if (!result.isConfirmed && result.isPreview) {
            // Show confirm button for preview
            buttonConfirmImport.visibility = View.VISIBLE
            buttonConfirmImport.setOnClickListener {
                // Confirm the import
                importViewModel.confirmImport()
                dialog.dismiss()
            }
        }
            
        // Set button click listener
        buttonClose.setOnClickListener {
            dialog.dismiss()
            // If this was just a preview and user cancels, reset the import
            if (!result.isConfirmed && result.isPreview) {
                importViewModel.cancelImport()
            }
            // Refresh data
            loadStatistics()
            updateRangeIncome()
        }
        
        dialog.show()
    }
    
    private fun showImportErrorDialog(errors: List<String>) {
        val message = StringBuilder("Import failed with errors:\n\n")
        errors.forEach { error ->
            message.append(" • $error\n")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import Error")
            .setMessage(message.toString())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 