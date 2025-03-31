package com.example.tutortrack.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutortrack.R
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.databinding.FragmentSessionListBinding
import com.example.tutortrack.ui.adapters.SessionAdapter
import com.example.tutortrack.ui.adapters.SessionWithDetails
import com.google.android.material.tabs.TabLayout
import java.util.Date

class SessionListFragment : Fragment() {

    private var _binding: FragmentSessionListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var adapter: SessionAdapter

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
        adapter = SessionAdapter(
            onPaymentStatusClick = { session, isPaid, paidDate ->
                // Update the session's payment status
                handlePaymentStatusChange(session, isPaid, paidDate)
            },
            onSessionDelete = { session ->
                // Delete the session
                deleteSession(session)
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
    
    private fun setupViewModel() {
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        
        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
            adapter.submitList(sessionsWithDetails)
            binding.emptyStateContainer.visibility = if (sessionsWithDetails.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // All sessions
                        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
                            adapter.submitList(sessionsWithDetails)
                            binding.emptyStateContainer.visibility = if (sessionsWithDetails.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                    1 -> { // Paid sessions
                        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
                            val paidSessions = sessionsWithDetails.filter { it.session.isPaid }
                            adapter.submitList(paidSessions)
                            binding.emptyStateContainer.visibility = if (paidSessions.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                    2 -> { // Unpaid sessions
                        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
                            val unpaidSessions = sessionsWithDetails.filter { !it.session.isPaid }
                            adapter.submitList(unpaidSessions)
                            binding.emptyStateContainer.visibility = if (unpaidSessions.isEmpty()) View.VISIBLE else View.GONE
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 