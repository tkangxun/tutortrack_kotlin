package com.example.tutortrack.ui.classtype

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.databinding.FragmentAddEditClassTypeBinding

class AddEditClassTypeFragment : Fragment() {

    private var _binding: FragmentAddEditClassTypeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var classTypeViewModel: ClassTypeViewModel
    private val args: AddEditClassTypeFragmentArgs by navArgs()
    
    private var classType: ClassType? = null
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditClassTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        classTypeViewModel = ViewModelProvider(this)[ClassTypeViewModel::class.java]
        
        isEditMode = args.classTypeId != -1L
        
        if (isEditMode) {
            loadClassTypeData()
        }
        
        binding.buttonSave.setOnClickListener {
            saveClassType()
        }
    }
    
    private fun loadClassTypeData() {
        classTypeViewModel.getClassTypeById(args.classTypeId).observe(viewLifecycleOwner) { loadedClassType ->
            classType = loadedClassType
            populateFields(loadedClassType)
        }
    }
    
    private fun populateFields(classType: ClassType) {
        binding.apply {
            editName.setText(classType.name)
            editRate.setText(classType.hourlyRate.toString())
        }
    }
    
    private fun saveClassType() {
        val name = binding.editName.text.toString().trim()
        val rateText = binding.editRate.text.toString().trim()
        
        if (name.isEmpty() || rateText.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        val hourlyRate: Double
        try {
            hourlyRate = rateText.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Please enter a valid rate", Toast.LENGTH_SHORT).show()
            return
        }
        
        val updatedClassType = if (isEditMode) {
            classType?.copy(
                name = name,
                hourlyRate = hourlyRate
            ) ?: ClassType(
                studentId = args.studentId,
                name = name,
                hourlyRate = hourlyRate
            )
        } else {
            ClassType(
                studentId = args.studentId,
                name = name,
                hourlyRate = hourlyRate
            )
        }
        
        if (isEditMode) {
            classTypeViewModel.updateClassType(updatedClassType)
        } else {
            classTypeViewModel.insertClassType(updatedClassType)
        }
        
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 