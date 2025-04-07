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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.FragmentAddEditSessionBinding
import com.example.tutortrack.ui.classtype.ClassTypeViewModel
import com.example.tutortrack.ui.student.StudentViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
                    
                    // Recalculate amount when class type changes
                    calculateAmount()
                } else {
                    selectedClassTypeId = -1L
                    selectedClassTypeRate = 0.0
                    binding.editAmount.setText("0.00")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedClassTypeId = -1L
                selectedClassTypeRate = 0.0
                binding.editAmount.setText("0.00")
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
            
            // Convert duration from minutes to hours for display
            val durationHours = loadedSession.durationMinutes / 60.0
            binding.editDuration.setText(String.format(Locale.getDefault(), "%.2f", durationHours))
            binding.editAmount.setText(String.format(Locale.getDefault(), "%.2f", loadedSession.amount))
            binding.editNotes.setText(loadedSession.notes)
        }
    }
    
    private fun calculateAmount() {
        val durationText = binding.editDuration.text.toString().trim()
        if (durationText.isNotEmpty() && selectedClassTypeRate > 0) {
            try {
                val durationHours = durationText.toDouble()
                if (durationHours > 0) {
                    // Calculate amount with proper rounding
                    val amount = (durationHours * selectedClassTypeRate * 100).roundToInt() / 100.0
                    binding.editAmount.setText(String.format(Locale.getDefault(), "%.2f", amount))
                } else {
                    binding.editAmount.setText("0.00")
                }
            } catch (e: NumberFormatException) {
                binding.editAmount.setText("0.00")
            }
        } else {
            binding.editAmount.setText("0.00")
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
            // Convert hours to minutes, rounding to nearest minute
            duration = (durationHours * 60).roundToInt()
            amount = amountText.toDouble()
            
            // Validate duration
            if (duration <= 0) {
                Toast.makeText(requireContext(), "Duration must be greater than 0", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Validate amount
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show()
                return
            }
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
        
        // Check if notes are new or changed
        val shouldAppendNotes = if (isEditMode) {
            // For edit mode, only append if notes have been changed
            notes.isNotEmpty() && notes != session?.notes
        } else {
            // For new sessions, append if notes exist
            notes.isNotEmpty()
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

        // Append session notes to student notes if needed
        if (shouldAppendNotes) {
            // Disable the save button to prevent multiple submissions
            binding.buttonSave.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
            
            // Use the helper method to append notes - after updating, it will navigate back
            addNotesToStudent(notes, date, selectedStudentId, selectedClassTypeId)
        } else {
            // If no notes to append, navigate back immediately
            findNavController().navigateUp()
        }
    }
    
    private fun addNotesToStudent(sessionNotes: String, sessionDate: Date, studentId: Long, classTypeId: Long) {
        // Get class type name directly from the spinner
        val classTypeName = classTypes.find { it.id == classTypeId }?.name ?: "Unknown"
        
        // Format the day of week and date
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val day = dayFormat.format(sessionDate)
        val dateStr = dateFormat.format(sessionDate)
        
        // Format the note to append
        val formattedNote = "[${classTypeName}] ($day, $dateStr) - $sessionNotes"
        
        // Get student data once - use get directly to avoid multiple updates
        val studentLiveData = studentViewModel.getStudentById(studentId)
        var observer: androidx.lifecycle.Observer<Student>? = null
        
        observer = androidx.lifecycle.Observer<Student> { student ->
            // Remove observer immediately to prevent multiple calls
            studentLiveData.removeObserver(observer!!)
            
            if (student == null) {
                binding.progressBar.visibility = View.GONE
                binding.buttonSave.isEnabled = true
                findNavController().navigateUp()
                return@Observer
            }
            
            // Format notes with new line only if existing notes aren't blank
            val updatedNotes = if (student.notes.isBlank()) {
                formattedNote
            } else {
                "${student.notes}\n$formattedNote"
            }
            
            // Update student with new notes
            val updatedStudent = student.copy(notes = updatedNotes)
            studentViewModel.updateStudent(updatedStudent)
            
            // Show a confirmation toast
            Toast.makeText(
                requireContext(),
                "Session notes added to student profile",
                Toast.LENGTH_SHORT
            ).show()
            
            // Log for debugging
            println("Updated student notes: $updatedNotes")
            
            // Use a short delay to let the database update complete
            lifecycleScope.launch {
                delay(300)
                binding.progressBar.visibility = View.GONE
                binding.buttonSave.isEnabled = true
                findNavController().navigateUp()
            }
        }
        
        // Observe only once with the custom observer
        studentLiveData.observe(viewLifecycleOwner, observer)
    }
    
    // Keep the original methods for reference, but they won't be used directly
    private fun appendSessionNotesToStudentSync(sessionNotes: String, sessionDate: Date, studentId: Long, classTypeId: Long) {
        // Show a loading indicator
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSave.isEnabled = false
        
        // Track whether we've already processed the update
        var updateProcessed = false
        
        // Get class type name directly from the spinner
        val classTypeName = classTypes.find { it.id == classTypeId }?.name ?: "Unknown"
        
        // Format the day of week and date
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val day = dayFormat.format(sessionDate)
        val dateStr = dateFormat.format(sessionDate)
        
        // Format the note to append
        val formattedNote = "[${classTypeName}] ($day, $dateStr) - $sessionNotes"
        
        // Create a one-time observer to avoid multiple updates
        val liveData = studentViewModel.getStudentById(studentId)
        
        liveData.observe(viewLifecycleOwner) { student ->
            // Skip if we've already processed an update
            if (updateProcessed) {
                return@observe
            }
            
            // Mark as processed to prevent duplicate updates
            updateProcessed = true
            
            if (student == null) {
                // Hide loading indicator
                binding.progressBar.visibility = View.GONE
                binding.buttonSave.isEnabled = true
                // Navigate back anyway if student not found
                findNavController().navigateUp()
                return@observe
            }
            
            // If student notes are empty, don't add a leading newline
            val updatedNotes = if (student.notes.isBlank()) {
                formattedNote
            } else {
                "${student.notes}\n$formattedNote"
            }
            
            // Create new student with updated notes
            val updatedStudent = student.copy(
                notes = updatedNotes
            )
            
            // Update the student in the database
            studentViewModel.updateStudent(updatedStudent)
            
            // Show success message
            Toast.makeText(
                requireContext(), 
                "Session notes added to student profile", 
                Toast.LENGTH_SHORT
            ).show()
            
            // Log update for debugging
            println("Updated student notes: $updatedNotes")
            
            // Use a small delay to ensure the update completes before navigating
            lifecycleScope.launch {
                delay(300)
                // Hide loading indicator
                binding.progressBar.visibility = View.GONE
                binding.buttonSave.isEnabled = true
                // Navigate back after the delay
                findNavController().navigateUp()
            }
        }
    }
    
    private fun appendSessionNotesToStudent(sessionNotes: String, sessionDate: Date, studentId: Long, classTypeId: Long) {
        // Get the class type name
        classTypeViewModel.getClassTypeById(classTypeId).observe(viewLifecycleOwner) { classType ->
            if (classType != null) {
                // Format the day of week and date
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val day = dayFormat.format(sessionDate)
                val dateStr = dateFormat.format(sessionDate)
                
                // Get the student and append the note
                studentViewModel.getStudentById(studentId).observe(viewLifecycleOwner) { student ->
                    if (student != null) {
                        // Format the note to append
                        val formattedNote = "[${classType.name}] ($day, $dateStr) - $sessionNotes"
                        
                        // If student notes are empty, don't add a leading newline
                        val updatedNotes = if (student.notes.isBlank()) {
                            formattedNote
                        } else {
                            "${student.notes}\n$formattedNote"
                        }
                        
                        // Create new student with updated notes
                        val updatedStudent = student.copy(
                            notes = updatedNotes
                        )
                        
                        // Update the student in the database
                        studentViewModel.updateStudent(updatedStudent)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 