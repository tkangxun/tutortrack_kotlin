package com.example.tutortrack.ui.reports

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tutortrack.R
import com.example.tutortrack.databinding.FragmentReportsBinding
import com.example.tutortrack.ui.session.SessionViewModel
import com.example.tutortrack.ui.student.StudentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
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
        editText.setSelection(0)  // Set cursor to beginning of text
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogView)
            .create()

        buttonRestoreDefault.setOnClickListener {
            editText.setText(defaultTemplate)
            editText.setSelection(0)  // Set cursor to beginning of text
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 