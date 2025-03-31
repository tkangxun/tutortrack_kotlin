package com.example.tutortrack.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutortrack.R
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.FragmentStudentListBinding
import com.example.tutortrack.ui.adapters.StudentAdapter
import com.example.tutortrack.ui.session.SessionViewModel

class StudentListFragment : Fragment() {

    private var _binding: FragmentStudentListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var adapter: StudentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModels()
        setupRecyclerView()
        setupListeners()
        setupSearch()
    }
    
    private fun setupViewModels() {
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        
        studentViewModel.allStudents.observe(viewLifecycleOwner) { students ->
            adapter.submitList(students)
            binding.emptyStateContainer.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun setupRecyclerView() {
        adapter = StudentAdapter(
            onItemClick = { student ->
                val bundle = Bundle().apply {
                    putLong("studentId", student.id)
                }
                findNavController().navigate(R.id.studentDetailFragment, bundle)
            },
            onStudentDelete = { student ->
                deleteStudent(student)
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }
    
    private fun deleteStudent(student: Student) {
        // Delete all sessions for this student
        sessionViewModel.getSessionsByStudentId(student.id).observe(viewLifecycleOwner) { sessions ->
            sessions.forEach { session ->
                sessionViewModel.deleteSession(session)
            }
            
            // Then delete the student
            studentViewModel.deleteStudent(student)
            
            Toast.makeText(requireContext(), "Student deleted", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupListeners() {
        binding.fabAddStudent.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("studentId", -1L)
                putString("title", getString(R.string.add_student))
            }
            findNavController().navigate(R.id.addEditStudentFragment, bundle)
        }

        binding.searchCard.setOnClickListener {
            binding.searchView.isIconified = false
            binding.searchView.requestFocus()
        }
    }
    
    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    studentViewModel.searchStudents(query).observe(viewLifecycleOwner) { students ->
                        adapter.submitList(students)
                        binding.emptyStateContainer.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    studentViewModel.allStudents.observe(viewLifecycleOwner) { students ->
                        adapter.submitList(students)
                        binding.emptyStateContainer.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
                    }
                } else {
                    studentViewModel.searchStudents(newText).observe(viewLifecycleOwner) { students ->
                        adapter.submitList(students)
                        binding.emptyStateContainer.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 