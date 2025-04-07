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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutortrack.R
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.databinding.FragmentClassTypeListBinding
import com.example.tutortrack.ui.adapters.ClassTypeAdapter
import com.example.tutortrack.ui.student.StudentViewModel

class ClassTypeListFragment : Fragment() {

    private var _binding: FragmentClassTypeListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var classTypeViewModel: ClassTypeViewModel
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var adapter: ClassTypeAdapter
    
    private val args: ClassTypeListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClassTypeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModels()
        setupRecyclerView()
        loadStudentData()
        setupListeners()
    }
    
    private fun setupViewModels() {
        classTypeViewModel = ViewModelProvider(this)[ClassTypeViewModel::class.java]
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        adapter = ClassTypeAdapter(
            onItemClick = { classType ->
                val bundle = Bundle().apply {
                    putLong("classTypeId", classType.id)
                    putLong("studentId", args.studentId)
                    putString("title", "Edit Class Type")
                }
                findNavController().navigate(R.id.addEditClassTypeFragment, bundle)
            },
            onClassTypeDelete = { classType ->
                deleteClassType(classType)
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }
    
    private fun loadStudentData() {
        studentViewModel.getStudentById(args.studentId).observe(viewLifecycleOwner) { student ->
            binding.textStudentName.text = student.name
            
            // Load class types for this student
            classTypeViewModel.getClassTypesByStudentId(args.studentId).observe(viewLifecycleOwner) { classTypes ->
                adapter.submitList(classTypes)
                binding.emptyView.visibility = if (classTypes.isEmpty()) View.VISIBLE else View.GONE
                
                // Store the current number of class types for deletion check
                currentClassTypeCount = classTypes.size
            }
        }
    }
    
    // Track the current number of class types
    private var currentClassTypeCount = 0
    
    private fun deleteClassType(classType: ClassType) {
        // Check if this is the last class type
        if (currentClassTypeCount <= 1) {
            Toast.makeText(requireContext(), "Cannot delete the last class type", Toast.LENGTH_SHORT).show()
            return
        }
        
        classTypeViewModel.deleteClassType(classType)
        Toast.makeText(requireContext(), "Class type deleted", Toast.LENGTH_SHORT).show()
    }
    
    private fun setupListeners() {
        binding.fabAddClassType.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("classTypeId", -1L)
                putLong("studentId", args.studentId)
                putString("title", "Add Class Type")
            }
            findNavController().navigate(R.id.addEditClassTypeFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 