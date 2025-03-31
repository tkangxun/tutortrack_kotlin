package com.example.tutortrack.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.FragmentAddEditStudentBinding
import com.example.tutortrack.ui.classtype.ClassTypeViewModel
import com.example.tutortrack.ui.classtype.InitialClassTypeAdapter

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
        
        // Validate phone
        val phone = binding.editPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.layoutPhone.error = getString(com.example.tutortrack.R.string.field_required)
            isValid = false
        } else {
            binding.layoutPhone.error = null
        }
        
        return isValid
    }
    
    private fun validateClassTypes(): Boolean {
        var isValid = true
        var hasAtLeastOneValidClassType = false
        
        for (i in initialClassTypes.indices) {
            val viewHolder = binding.recyclerViewClassTypes.findViewHolderForAdapterPosition(i) as? InitialClassTypeAdapter.ClassTypeViewHolder
            
            if (viewHolder != null) {
                val name = viewHolder.binding.editClassName.text.toString().trim()
                val rateText = viewHolder.binding.editClassRate.text.toString().trim()
                
                // Only show error for required class types if it's the only class type
                val isClassTypeRequired = initialClassTypes.size == 1
                
                if (name.isEmpty() && isClassTypeRequired) {
                    viewHolder.binding.layoutClassName.error = getString(com.example.tutortrack.R.string.field_required)
                    isValid = false
                } else {
                    viewHolder.binding.layoutClassName.error = null
                    if (name.isNotEmpty()) {
                        hasAtLeastOneValidClassType = true
                    }
                }
                
                if (rateText.isEmpty() && isClassTypeRequired) {
                    viewHolder.binding.layoutClassRate.error = getString(com.example.tutortrack.R.string.field_required)
                    isValid = false
                } else if (rateText.isNotEmpty()) {
                    try {
                        rateText.toDouble()
                        viewHolder.binding.layoutClassRate.error = null
                    } catch (e: NumberFormatException) {
                        viewHolder.binding.layoutClassRate.error = getString(com.example.tutortrack.R.string.enter_valid_rate)
                        isValid = false
                    }
                } else {
                    viewHolder.binding.layoutClassRate.error = null
                }
                
                // Update the model with current values
                initialClassTypes[i] = initialClassTypes[i].copy(
                    name = name,
                    hourlyRate = if (rateText.isNotEmpty()) {
                        try { rateText.toDouble() } catch (e: NumberFormatException) { 0.0 }
                    } else {
                        0.0
                    }
                )
            }
        }
        
        // Ensure at least one valid class type
        if (!hasAtLeastOneValidClassType) {
            Toast.makeText(requireContext(), "At least one class type with a name is required", Toast.LENGTH_SHORT).show()
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
        
        // Validate class types
        if (!validateClassTypes()) {
            Toast.makeText(requireContext(), "Please correct errors in class types", Toast.LENGTH_SHORT).show()
            return
        }
        
        val updatedStudent = if (isEditMode) {
            student?.copy(
                name = name,
                phone = phone,
                grade = grade,
                parentName = parentName,
                parentContact = parentContact,
                notes = notes
            ) ?: Student(
                name = name,
                phone = phone,
                grade = grade,
                parentName = parentName,
                parentContact = parentContact,
                notes = notes
            )
        } else {
            Student(
                name = name,
                phone = phone,
                grade = grade,
                parentName = parentName,
                parentContact = parentContact,
                notes = notes
            )
        }
        
        // First save/update the student to get the ID
        if (isEditMode) {
            studentViewModel.updateStudent(updatedStudent)
            // For existing students, we already have the ID
            saveClassTypes(updatedStudent.id)
            findNavController().navigateUp()
        } else {
            // For new students, we need to wait for the student ID to be returned
            studentViewModel.insertStudent(updatedStudent)
            studentViewModel.getLastInsertedStudent().observe(viewLifecycleOwner) { newStudent ->
                if (newStudent != null) {
                    saveClassTypes(newStudent.id)
                    findNavController().navigateUp()
                }
            }
        }
    }
    
    private fun saveClassTypes(studentId: Long) {
        // Only save valid class types (with name)
        val validClassTypes = initialClassTypes.filter { it.name.isNotEmpty() }
        
        // Delete any existing class types that were removed
        if (isEditMode) {
            val existingIds = validClassTypes.mapNotNull { it.existingId }
            classTypeViewModel.deleteRemovedClassTypes(studentId, existingIds)
        }
        
        // Save new or update existing class types
        validClassTypes.forEach { initialClassType ->
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