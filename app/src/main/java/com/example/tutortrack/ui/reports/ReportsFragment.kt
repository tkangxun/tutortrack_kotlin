package com.example.tutortrack.ui.reports

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var studentViewModel: StudentViewModel
    
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
    }
    
    private fun setupViewModels() {
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
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
        // Get sessions in the selected date range with details
        sessionViewModel.getSessionsWithDetailsInDateRange(startDate.time, endDate.time)
            .observe(viewLifecycleOwner) { sessionsWithDetails ->
                if (sessionsWithDetails.isEmpty()) {
                    Toast.makeText(requireContext(), "No sessions found in the selected date range", Toast.LENGTH_SHORT).show()
                    return@observe
                }
                
                try {
                    // Create CSV file
                    val fileName = "tutor_track_export_${csvDateFormat.format(startDate.time)}_to_${csvDateFormat.format(endDate.time)}.csv"
                    val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                    
                    FileWriter(file).use { writer ->
                        // Write header
                        writer.append("Date,Student,Class Type,Duration (hours),Amount,Paid,Payment Date,Notes\n")
                        
                        // Write data
                        sessionsWithDetails.forEach { sessionWithDetails ->
                            val session = sessionWithDetails.session
                            val studentName = sessionWithDetails.student?.name ?: "Unknown"
                            val classTypeName = sessionWithDetails.classType?.name ?: "Unknown"
                            
                            // Format duration in hours
                            val durationHours = session.durationMinutes / 60.0
                            
                            // Format payment date
                            val paymentDate = session.paidDate?.let { csvDateFormat.format(it) } ?: ""
                            
                            // Write row
                            writer.append("${csvDateFormat.format(session.date)},")
                            writer.append("${studentName.replace(",", ";")},")
                            writer.append("${classTypeName.replace(",", ";")},")
                            writer.append("${String.format("%.2f", durationHours)},")
                            writer.append("${String.format("%.2f", session.amount)},")
                            writer.append("${if (session.isPaid) "Yes" else "No"},")
                            writer.append("${paymentDate},")
                            writer.append("${session.notes.replace(",", ";").replace("\n", " ")}\n")
                        }
                    }
                    
                    // Share the file
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.provider",
                        file
                    )
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    startActivity(Intent.createChooser(intent, "Share CSV File"))
                    
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error exporting data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 