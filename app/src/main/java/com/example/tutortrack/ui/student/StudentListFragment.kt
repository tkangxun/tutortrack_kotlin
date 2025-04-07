package com.example.tutortrack.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
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
    
    // Flag to track if we're showing archived students
    private var showingArchived = false

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
        setupMenu()
    }
    
    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_student_list, menu)
                
                // Update menu item text based on current mode
                val archiveMenuItem = menu.findItem(R.id.action_toggle_archive)
                updateArchiveMenuTitle(archiveMenuItem)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_toggle_archive -> {
                        toggleArchivedView()
                        updateArchiveMenuTitle(menuItem)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    
    private fun updateArchiveMenuTitle(menuItem: MenuItem) {
        menuItem.title = if (showingArchived) {
            getString(R.string.show_active_students)
        } else {
            getString(R.string.show_archived_students)
        }
    }
    
    private fun toggleArchivedView() {
        showingArchived = !showingArchived
        
        // Clear search view when toggling
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()
        
        // Update UI based on mode
        binding.fabAddStudent.visibility = if (showingArchived) View.GONE else View.VISIBLE
        
        // Update title
        updateTitle()
        
        // Reload student list
        loadStudentList()
    }
    
    private fun updateTitle() {
        activity?.title = if (showingArchived) "Archived Students" else "Students"
    }
    
    private fun setupViewModels() {
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        
        loadStudentList()
    }
    
    private fun loadStudentList() {
        if (showingArchived) {
            studentViewModel.archivedStudents.observe(viewLifecycleOwner) { students ->
                adapter.submitList(students)
                updateEmptyState(students)
            }
        } else {
            studentViewModel.allStudents.observe(viewLifecycleOwner) { students ->
                adapter.submitList(students)
                updateEmptyState(students)
            }
        }
    }
    
    private fun updateEmptyState(students: List<Student>) {
        if (students.isEmpty()) {
            binding.emptyStateContainer.visibility = View.VISIBLE
            binding.emptyView.text = if (showingArchived) {
                getString(R.string.no_archived_students)
            } else {
                getString(R.string.no_students)
            }
        } else {
            binding.emptyStateContainer.visibility = View.GONE
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
            },
            onStudentArchive = { student ->
                archiveStudent(student)
            },
            onStudentUnarchive = { student ->
                unarchiveStudent(student)
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
    
    private fun archiveStudent(student: Student) {
        studentViewModel.archiveStudent(student)
        Toast.makeText(requireContext(), "${student.name} archived", Toast.LENGTH_SHORT).show()
    }
    
    private fun unarchiveStudent(student: Student) {
        studentViewModel.unarchiveStudent(student)
        Toast.makeText(requireContext(), "${student.name} unarchived", Toast.LENGTH_SHORT).show()
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
                    performSearch(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    loadStudentList()
                } else {
                    performSearch(newText)
                }
                return true
            }
        })
    }
    
    private fun performSearch(query: String) {
        val searchObservable = if (showingArchived) {
            studentViewModel.searchArchivedStudents(query)
        } else {
            studentViewModel.searchStudents(query)
        }
        
        searchObservable.observe(viewLifecycleOwner) { students ->
            adapter.submitList(students)
            updateEmptyState(students)
        }
    }

    override fun onResume() {
        super.onResume()
        updateTitle()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 