package com.example.tutortrack.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.FragmentAddEditStudentBinding
import com.example.tutortrack.ui.classtype.ClassTypeViewModel
import com.example.tutortrack.ui.classtype.InitialClassTypeAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditStudentFragment : Fragment() {

    private var _binding: FragmentAddEditStudentBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var classTypeViewModel: ClassTypeViewModel
    private val args: AddEditStudentFragmentArgs by navArgs()
    
    private var student: Student? = null
    private var isEditMode = false
    private lateinit var classTypeAdapter: InitialClassTypeAdapter
    private val initialClassTypes = mutableListOf<InitialClassType>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModels()
        setupRecyclerView()
        
        isEditMode = args.studentId != -1L
        
        if (isEditMode) {
            loadStudentData()
        } else {
            // Add one empty class type by default
            addEmptyClassType()
        }
        
        binding.buttonAddClassType.setOnClickListener {
            addEmptyClassType()
        }
        
        binding.buttonSave.setOnClickListener {
            saveStudent()
        }
    }
    
    private fun setupViewModels() {
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
        classTypeViewModel = ViewModelProvider(this)[ClassTypeViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        classTypeAdapter = InitialClassTypeAdapter(
            initialClassTypes,
            onRemoveClick = { position -> 
                if (initialClassTypes.size > 1) {
                    initialClassTypes.removeAt(position)
                    classTypeAdapter.notifyItemRemoved(position)
                    classTypeAdapter.notifyItemRangeChanged(position, initialClassTypes.size - position)
                } else {
                    Toast.makeText(requireContext(), "At least one class type is required", Toast.LENGTH_SHORT).show()
                }
            }
        )
        
        binding.recyclerViewClassTypes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = classTypeAdapter
        }
    }
    
    private fun addEmptyClassType() {
        initialClassTypes.add(InitialClassType("", 0.0))
        classTypeAdapter.notifyItemInserted(initialClassTypes.size - 1)
    }
    
    private fun loadStudentData() {
        studentViewModel.getStudentById(args.studentId).observe(viewLifecycleOwner) { loadedStudent ->
            student = loadedStudent
            populateFields(loadedStudent)
            
            // Load class types for this student
            classTypeViewModel.getClassTypesByStudentId(loadedStudent.id).observe(viewLifecycleOwner) { classTypes ->
                initialClassTypes.clear()
                if (classTypes.isNotEmpty()) {
                    classTypes.forEach { classType ->
                        initialClassTypes.add(InitialClassType(
                            name = classType.name,
                            hourlyRate = classType.hourlyRate,
                            existingId = classType.id
                        ))
                    }
                } else {
                    // Add one empty class type if none exist
                    addEmptyClassType()
                }
                classTypeAdapter.notifyDataSetChanged()
            }
        }
    }
    
    private fun populateFields(student: Student) {
        binding.apply {
            editName.setText(student.name)
            editPhone.setText(student.phone)
            editGrade.setText(student.grade)
            editParentName.setText(student.parentName)
            editParentContact.setText(student.parentContact)
            editNotes.setText(student.notes)
        }
    }
    
    private fun validateRequiredFields(): Boolean {
        var isValid = true
        
        // Validate name
        val name = binding.editName.text.toString().trim()
        if (name.isEmpty()) {
            binding.layoutName.error = getString(com.example.tutortrack.R.string.field_required)
            isValid = false
        } else {
            binding.layoutName.error = null
        }
        
        // Phone is now optional, no validation needed
        binding.layoutPhone.error = null
        
        return isValid
    }
    
    private fun validateClassTypes(): Boolean {
        var isValid = true
        var hasAtLeastOneValidClassType = false
        
        // Check if we have at least one valid class type
        for (classType in initialClassTypes) {
            if (classType.name.isNotEmpty() && classType.hourlyRate > 0) {
                hasAtLeastOneValidClassType = true
                break
            }
        }
        
        // If we don't have any valid class types, mark all empty ones as errors
        if (!hasAtLeastOneValidClassType) {
            val recyclerView = binding.recyclerViewClassTypes
            for (i in 0 until recyclerView.childCount) {
                val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as? InitialClassTypeAdapter.ClassTypeViewHolder
                if (viewHolder != null) {
                    val position = viewHolder.adapterPosition
                    if (position != -1 && position < initialClassTypes.size) {
                        val name = initialClassTypes[position].name
                        val rate = initialClassTypes[position].hourlyRate
                        
                        if (name.isEmpty()) {
                            viewHolder.binding.layoutClassName.error = getString(com.example.tutortrack.R.string.field_required)
                            isValid = false
                        } else {
                            viewHolder.binding.layoutClassName.error = null
                        }
                        
                        if (rate <= 0) {
                            viewHolder.binding.layoutClassRate.error = getString(com.example.tutortrack.R.string.enter_valid_rate)
                            isValid = false
                        } else {
                            viewHolder.binding.layoutClassRate.error = null
                        }
                    }
                }
            }
            Toast.makeText(requireContext(), "At least one class type with a name and valid rate is required", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        return isValid
    }
    
    private fun saveStudent() {
        val name = binding.editName.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val grade = binding.editGrade.text.toString().trim()
        val parentName = binding.editParentName.text.toString().trim()
        val parentContact = binding.editParentContact.text.toString().trim()
        val notes = binding.editNotes.text.toString().trim()
        
        // Validate required fields
        if (!validateRequiredFields()) {
            Toast.makeText(requireContext(), "Please fill all required fields (marked with *)", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Make sure to update class type values from UI before validation
        updateClassTypesFromUI()
        
        // Validate class types
        if (!validateClassTypes()) {
            Toast.makeText(requireContext(), "Please correct errors in class types", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Only save valid class types (with name and valid rate)
        val validClassTypes = initialClassTypes.filter { it.name.isNotEmpty() && it.hourlyRate > 0 }
        
        if (validClassTypes.isEmpty()) {
            Toast.makeText(requireContext(), "At least one valid class type is required", Toast.LENGTH_SHORT).show()
            return
        }
        
        val updatedStudent = if (isEditMode) {
            student?.copy(
                name = name,
                phone = if (phone.isNotEmpty()) phone else null,
                grade = grade,
                parentName = parentName,
                parentContact = parentContact,
                notes = notes
            ) ?: Student(
                name = name,
                phone = if (phone.isNotEmpty()) phone else null,
                grade = grade,
                parentName = parentName,
                parentContact = parentContact,
                notes = notes
            )
        } else {
            Student(
                name = name,
                phone = if (phone.isNotEmpty()) phone else null,
                grade = grade,
                parentName = parentName,
                parentContact = parentContact,
                notes = notes
            )
        }
        
        // Disable the save button to prevent multiple submissions
        binding.buttonSave.isEnabled = false
        
        if (isEditMode) {
            // For existing students, update in a background thread
            lifecycleScope.launch {
                try {
                    // Update student
                    withContext(Dispatchers.IO) {
                        studentViewModel.updateStudent(updatedStudent)
                    }
                    
                    // Then update class types
                    val classTypeSuccess = withContext(Dispatchers.IO) {
                        saveClassTypes(updatedStudent.id)
                    }
                    
                    if (classTypeSuccess) {
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save class types", Toast.LENGTH_SHORT).show()
                        binding.buttonSave.isEnabled = true
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error updating student: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.buttonSave.isEnabled = true
                }
            }
        } else {
            // For new students, create a student synchronously
            try {
                // This will run on a background thread
                lifecycleScope.launch {
                    try {
                        // Insert student and get ID
                        val studentId = withContext(Dispatchers.IO) {
                            studentViewModel.insertStudentAndGetId(updatedStudent)
                        }
                        
                        // Try to save class types
                        val classTypeSuccess = withContext(Dispatchers.IO) {
                            saveClassTypes(studentId)
                        }
                        
                        if (classTypeSuccess) {
                            // Navigate back if successful
                            findNavController().navigateUp()
                        } else {
                            // Delete student if class types failed
                            withContext(Dispatchers.IO) {
                                studentViewModel.deleteStudent(updatedStudent.copy(id = studentId))
                            }
                            Toast.makeText(requireContext(), "Failed to save class types", Toast.LENGTH_SHORT).show()
                            binding.buttonSave.isEnabled = true
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.buttonSave.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save student: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.buttonSave.isEnabled = true
            }
        }
    }
    
    // Helper method to update initialClassTypes from UI
    private fun updateClassTypesFromUI() {
        val recyclerView = binding.recyclerViewClassTypes
        
        // Force update all ViewHolders to ensure their current values are captured
        for (i in 0 until recyclerView.childCount) {
            val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as? InitialClassTypeAdapter.ClassTypeViewHolder
            if (viewHolder != null) {
                val position = viewHolder.adapterPosition
                if (position != -1 && position < initialClassTypes.size) {
                    val name = viewHolder.binding.editClassName.text.toString().trim()
                    val rateText = viewHolder.binding.editClassRate.text.toString().trim()
                    val rate = if (rateText.isNotEmpty()) {
                        try { rateText.toDouble() } catch (e: NumberFormatException) { 0.0 }
                    } else {
                        0.0
                    }
                    
                    // Update the model with current values
                    initialClassTypes[position] = initialClassTypes[position].copy(
                        name = name,
                        hourlyRate = rate
                    )
                }
            }
        }
    }
    
    private suspend fun saveClassTypes(studentId: Long): Boolean {
        // Only save valid class types (with name and valid rate)
        val validClassTypes = initialClassTypes.filter { it.name.isNotEmpty() && it.hourlyRate > 0 }
        
        if (validClassTypes.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "No valid class types to save", Toast.LENGTH_SHORT).show()
            }
            return false
        }
        
        return try {
            // Delete any existing class types that were removed
            if (isEditMode) {
                val existingIds = validClassTypes.mapNotNull { it.existingId }
                classTypeViewModel.deleteRemovedClassTypes(studentId, existingIds)
            }
            
            // Save new or update existing class types
            var success = true
            validClassTypes.forEach { initialClassType ->
                try {
                    val classType = if (initialClassType.existingId != null) {
                        // Update existing class type
                        ClassType(
                            id = initialClassType.existingId,
                            studentId = studentId,
                            name = initialClassType.name,
                            hourlyRate = initialClassType.hourlyRate
                        )
                    } else {
                        // Create new class type
                        ClassType(
                            studentId = studentId,
                            name = initialClassType.name,
                            hourlyRate = initialClassType.hourlyRate
                        )
                    }
                    
                    if (initialClassType.existingId != null) {
                        classTypeViewModel.updateClassType(classType)
                    } else {
                        classTypeViewModel.insertClassType(classType)
                    }
                } catch (e: Exception) {
                    success = false
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to save class type: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            success
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to save class types: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Data class for holding initial class type information
    data class InitialClassType(
        val name: String,
        val hourlyRate: Double,
        val existingId: Long? = null
    )
} 