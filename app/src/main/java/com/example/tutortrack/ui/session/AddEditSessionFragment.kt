package com.example.tutortrack.ui.session

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.FragmentAddEditSessionBinding
import com.example.tutortrack.ui.classtype.ClassTypeViewModel
import com.example.tutortrack.ui.student.StudentViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddEditSessionFragment : Fragment() {

    private var _binding: FragmentAddEditSessionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var classTypeViewModel: ClassTypeViewModel
    
    private var sessionId: Long = -1L
    private var preSelectedStudentId: Long = -1L
    private var title: String = ""
    
    private var session: Session? = null
    private var isEditMode = false
    private var selectedDate = Calendar.getInstance()
    private var selectedPaidDate = Calendar.getInstance()
    private val students = mutableListOf<Student>()
    private val classTypes = mutableListOf<ClassType>()
    private var selectedStudentId: Long = -1L
    private var selectedClassTypeId: Long = -1L
    private var selectedClassTypeRate: Double = 0.0
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var wasPreviouslyPaid = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get arguments from bundle
        arguments?.let {
            sessionId = it.getLong("sessionId", -1L)
            preSelectedStudentId = it.getLong("studentId", -1L)
            title = it.getString("title", "")
        }
        
        setupViewModels()
        setupDatePicker()
        setupPaidDatePicker()
        setupCheckboxListener()
        setupStudentSpinner()
        setupClassTypeSpinner()
        
        isEditMode = sessionId != -1L
        
        if (isEditMode) {
            loadSessionData()
        } else if (preSelectedStudentId != -1L) {
            // Pre-select the student if coming from student detail
            selectedStudentId = preSelectedStudentId
        }
        
        binding.buttonSave.setOnClickListener {
            saveSession()
        }
        
        binding.editDuration.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                calculateAmount()
            }
        }
    }
    
    private fun setupViewModels() {
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
        classTypeViewModel = ViewModelProvider(this)[ClassTypeViewModel::class.java]
    }
    
    private fun setupDatePicker() {
        binding.editDate.setText(dateFormat.format(selectedDate.time))
        
        binding.editDate.setOnClickListener {
            val year = selectedDate.get(Calendar.YEAR)
            val month = selectedDate.get(Calendar.MONTH)
            val day = selectedDate.get(Calendar.DAY_OF_MONTH)
            
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                binding.editDate.setText(dateFormat.format(selectedDate.time))
            }, year, month, day).show()
        }
    }
    
    private fun setupPaidDatePicker() {
        binding.editPaidDate.setText(dateFormat.format(selectedPaidDate.time))
        
        binding.editPaidDate.setOnClickListener {
            val year = selectedPaidDate.get(Calendar.YEAR)
            val month = selectedPaidDate.get(Calendar.MONTH)
            val day = selectedPaidDate.get(Calendar.DAY_OF_MONTH)
            
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                selectedPaidDate.set(selectedYear, selectedMonth, selectedDay)
                binding.editPaidDate.setText(dateFormat.format(selectedPaidDate.time))
            }, year, month, day).show()
        }
    }
    
    private fun setupCheckboxListener() {
        binding.checkboxPaid.setOnCheckedChangeListener { buttonView, isChecked ->
            if (wasPreviouslyPaid && !isChecked) {
                // Show confirmation dialog when trying to uncheck "Paid" checkbox
                AlertDialog.Builder(requireContext())
                    .setTitle("Mark as Unpaid")
                    .setMessage("Are you sure you want to mark this session as unpaid? This will remove the payment date information.")
                    .setPositiveButton("Yes") { _, _ ->
                        // User confirmed, update UI
                        binding.layoutPaidDate.visibility = View.GONE
                        wasPreviouslyPaid = false
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        // User cancelled, revert checkbox state
                        buttonView.isChecked = true
                    }
                    .show()
            } else {
                // Normal behavior when checking the box or when it was not previously paid
                binding.layoutPaidDate.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (isChecked) {
                    wasPreviouslyPaid = true
                }
            }
        }
    }
    
    private fun setupStudentSpinner() {
        studentViewModel.allStudents.observe(viewLifecycleOwner) { studentList ->
            students.clear()
            students.addAll(studentList)
            
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                students.map { it.name }
            )
            
            binding.spinnerStudent.adapter = adapter
            
            if (selectedStudentId != -1L) {
                val position = students.indexOfFirst { it.id == selectedStudentId }
                if (position != -1) {
                    binding.spinnerStudent.setSelection(position)
                }
            }
        }
        
        binding.spinnerStudent.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (students.isNotEmpty() && position < students.size) {
                    val student = students[position]
                    selectedStudentId = student.id
                    
                    // Load class types for this student
                    loadClassTypesByStudent(student.id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedStudentId = -1L
            }
        }
    }
    
    private fun setupClassTypeSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            classTypes.map { it.name }
        )
        
        binding.spinnerClassType.adapter = adapter
        
        binding.spinnerClassType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (classTypes.isNotEmpty() && position < classTypes.size) {
                    val classType = classTypes[position]
                    selectedClassTypeId = classType.id
                    selectedClassTypeRate = classType.hourlyRate
                    calculateAmount()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedClassTypeId = -1L
                selectedClassTypeRate = 0.0
            }
        }
    }
    
    private fun loadClassTypesByStudent(studentId: Long) {
        classTypeViewModel.getClassTypesByStudentId(studentId).observe(viewLifecycleOwner) { classTypeList ->
            classTypes.clear()
            classTypes.addAll(classTypeList)
            
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                classTypes.map { it.name }
            )
            
            binding.spinnerClassType.adapter = adapter
            
            // If we have class types, select the first one by default or the one from the session
            if (classTypes.isNotEmpty()) {
                if (session != null && isEditMode) {
                    // For edit mode, try to select the class type from the session
                    val position = classTypes.indexOfFirst { it.id == session?.classTypeId }
                    if (position != -1) {
                        binding.spinnerClassType.setSelection(position)
                    } else {
                        // If not found, select the first class type
                        binding.spinnerClassType.setSelection(0)
                        selectedClassTypeId = classTypes[0].id
                        selectedClassTypeRate = classTypes[0].hourlyRate
                    }
                } else {
                    // For new sessions, select the first class type
                    binding.spinnerClassType.setSelection(0)
                    selectedClassTypeId = classTypes[0].id
                    selectedClassTypeRate = classTypes[0].hourlyRate
                }
                calculateAmount()
            } else {
                // No class types available
                selectedClassTypeId = -1L
                selectedClassTypeRate = 0.0
                Toast.makeText(requireContext(), "Please add class types for this student first", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun loadSessionData() {
        sessionViewModel.getSessionById(sessionId).observe(viewLifecycleOwner) { loadedSession ->
            session = loadedSession
            selectedStudentId = loadedSession.studentId
            selectedClassTypeId = loadedSession.classTypeId
            
            // Find student position in spinner
            val position = students.indexOfFirst { it.id == selectedStudentId }
            if (position != -1) {
                binding.spinnerStudent.setSelection(position)
            }
            
            // Set session date
            selectedDate.time = loadedSession.date
            binding.editDate.setText(dateFormat.format(selectedDate.time))
            
            // Set paid status and date
            binding.checkboxPaid.isChecked = loadedSession.isPaid
            wasPreviouslyPaid = loadedSession.isPaid
            binding.layoutPaidDate.visibility = if (loadedSession.isPaid) View.VISIBLE else View.GONE
            
            // Set paid date if available
            loadedSession.paidDate?.let {
                selectedPaidDate.time = it
                binding.editPaidDate.setText(dateFormat.format(it))
            }
            
            // Populate other fields
            binding.editDuration.setText(loadedSession.durationMinutes.toString())
            binding.editAmount.setText(String.format(Locale.getDefault(), "%.2f", loadedSession.amount))
            binding.editNotes.setText(loadedSession.notes)
        }
    }
    
    private fun calculateAmount() {
        val durationText = binding.editDuration.text.toString().trim()
        if (durationText.isNotEmpty() && selectedClassTypeRate > 0) {
            try {
                val durationHours = durationText.toDouble()
                val amount = durationHours * selectedClassTypeRate
                binding.editAmount.setText(String.format(Locale.getDefault(), "%.2f", amount))
            } catch (e: NumberFormatException) {
                // Invalid duration, do nothing
            }
        }
    }
    
    private fun saveSession() {
        // Validate student selection
        if (selectedStudentId == -1L) {
            Toast.makeText(requireContext(), "Please select a student", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate class type selection
        if (selectedClassTypeId == -1L) {
            Toast.makeText(requireContext(), "Please select a class type", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get form data
        val date = selectedDate.time
        val durationText = binding.editDuration.text.toString().trim()
        val amountText = binding.editAmount.text.toString().trim()
        val isPaid = binding.checkboxPaid.isChecked
        val notes = binding.editNotes.text.toString().trim()
        
        // Validate required fields
        if (durationText.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Parse numeric values
        val duration: Int
        val amount: Double
        
        try {
            val durationHours = durationText.toDouble()
            duration = (durationHours * 60).toInt() // Convert hours to minutes
            amount = amountText.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Determine paid date
        val paidDate = if (isPaid) {
            // If paid checkbox is checked, use the selected paid date
            selectedPaidDate.time
        } else {
            // If not paid, set to null
            null
        }
        
        // Create or update session
        val updatedSession = if (isEditMode) {
            session?.copy(
                studentId = selectedStudentId,
                classTypeId = selectedClassTypeId,
                date = date,
                durationMinutes = duration,
                isPaid = isPaid,
                paidDate = paidDate,
                amount = amount,
                notes = notes
            ) ?: Session(
                studentId = selectedStudentId,
                classTypeId = selectedClassTypeId,
                date = date,
                durationMinutes = duration,
                isPaid = isPaid,
                paidDate = paidDate,
                amount = amount,
                notes = notes
            )
        } else {
            Session(
                studentId = selectedStudentId,
                classTypeId = selectedClassTypeId,
                date = date,
                durationMinutes = duration,
                isPaid = isPaid,
                paidDate = paidDate,
                amount = amount,
                notes = notes
            )
        }
        
        // Save to database
        if (isEditMode) {
            sessionViewModel.updateSession(updatedSession)
        } else {
            sessionViewModel.insertSession(updatedSession)
        }
        
        // Navigate back
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 