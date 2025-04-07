package com.example.tutortrack.ui.session

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutortrack.R
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.databinding.FragmentSessionListBinding
import com.example.tutortrack.ui.adapters.SessionGroupAdapter
import com.example.tutortrack.ui.adapters.SessionListItem
import com.example.tutortrack.ui.adapters.SessionWithDetails
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SessionListFragment : Fragment() {

    private var _binding: FragmentSessionListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var adapter: SessionGroupAdapter
    private var allSessionItems = listOf<SessionListItem>()
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupViewModel()
        setupTabLayout()
        setupListeners()
    }
    
    private fun setupRecyclerView() {
        adapter = SessionGroupAdapter(
            onPaymentStatusClick = { session, isPaid, paidDate ->
                // Update the session's payment status
                handlePaymentStatusChange(session, isPaid, paidDate)
            },
            onSessionDelete = { session ->
                // Delete the session
                deleteSession(session)
            },
            onSessionEdit = { session ->
                // Navigate to edit session
                editSession(session)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }
    
    private fun handlePaymentStatusChange(session: Session, isPaid: Boolean, paidDate: Date?) {
        val updatedSession = session.copy(
            isPaid = isPaid,
            paidDate = if (isPaid) paidDate else null
        )
        sessionViewModel.updateSession(updatedSession)
    }
    
    private fun deleteSession(session: Session) {
        sessionViewModel.deleteSession(session)
        Toast.makeText(requireContext(), "Session deleted", Toast.LENGTH_SHORT).show()
    }
    
    private fun editSession(session: Session) {
        val bundle = Bundle().apply {
            putLong("sessionId", session.id)
            putLong("studentId", session.studentId)
            putString("title", getString(R.string.edit_session))
        }
        findNavController().navigate(R.id.addEditSessionFragment, bundle)
    }
    
    private fun setupViewModel() {
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        
        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
            updateSessionList(sessionsWithDetails)
        }
    }
    
    private fun updateSessionList(sessionsWithDetails: List<SessionWithDetails>) {
        val groupedItems = SessionGroupAdapter.groupSessionsByDay(sessionsWithDetails)
        allSessionItems = groupedItems // Store for searching
        adapter.submitList(groupedItems)
        binding.emptyStateContainer.visibility = if (sessionsWithDetails.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // All sessions
                        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
                            updateSessionList(sessionsWithDetails)
                        }
                    }
                    1 -> { // Paid sessions
                        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
                            val paidSessions = sessionsWithDetails.filter { it.session.isPaid }
                            updateSessionList(paidSessions)
                        }
                    }
                    2 -> { // Unpaid sessions
                        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
                            val unpaidSessions = sessionsWithDetails.filter { !it.session.isPaid }
                            updateSessionList(unpaidSessions)
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupListeners() {
        binding.fabAddSession.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("sessionId", -1L)
                putLong("studentId", -1L)
                putString("title", getString(R.string.add_session))
            }
            findNavController().navigate(R.id.addEditSessionFragment, bundle)
        }
        
        binding.buttonDateSearch.setOnClickListener {
            showDatePickerForSearch()
        }
    }
    
    private fun showDatePickerForSearch() {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                val selectedDate = calendar.time
                scrollToDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun scrollToDate(targetDate: Date) {
        // Find closest header to target date
        val closestHeader = adapter.findClosestHeaderToDate(targetDate)
        
        if (closestHeader == null) {
            Toast.makeText(requireContext(), "No sessions found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Find the position of this header in the list
        val position = adapter.currentList.indexOf(closestHeader)
        if (position == -1) return
        
        // Make sure the group is expanded
        if (!closestHeader.isExpanded) {
            adapter.toggleGroupExpansion(closestHeader)
        }
        
        // Show a toast with the found date
        val dateFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        Toast.makeText(
            requireContext(),
            "Found: ${dateFormatter.format(closestHeader.date)}",
            Toast.LENGTH_SHORT
        ).show()
        
        // Directly jump to position first (no animation)
        (binding.recyclerView.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(position, 0)
            
        // Apply highlight immediately after scroll
        binding.recyclerView.post {
            highlightHeaderAtPosition(position)
        }
    }
    
    private fun highlightHeaderAtPosition(position: Int) {
        // Try to find the ViewHolder
        val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(position)
        
        if (viewHolder != null) {
            // We found the ViewHolder, apply highlight
            val itemView = viewHolder.itemView
            
            // Save original state
            val colorFrom = resources.getColor(R.color.header_background, null)
            val colorTo = resources.getColor(R.color.highlight_overlay, null)
            
            // Apply subtle highlight color
            itemView.setBackgroundColor(colorTo)
            
            // Find the text view to ensure it remains visible
            val textView = itemView.findViewById<TextView>(R.id.textDayHeader)
            textView?.setTextColor(resources.getColor(R.color.primary_dark, null))
            
            // Schedule removal of highlight after delay
            itemView.postDelayed({
                // Animate back to original
                itemView.animate()
                    .setDuration(300)
                    .withEndAction {
                        itemView.setBackgroundColor(colorFrom)
                        textView?.setTextColor(resources.getColor(R.color.primary, null))
                    }
                    .start()
            }, 500) // Highlight for 0.5 seconds
        } else {
            // If ViewHolder not found, try again after a short delay
            binding.recyclerView.postDelayed({
                highlightHeaderAtPosition(position)
            }, 100)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 