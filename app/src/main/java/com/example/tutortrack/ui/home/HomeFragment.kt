package com.example.tutortrack.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutortrack.R
import com.example.tutortrack.databinding.FragmentHomeBinding
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.ui.adapters.SessionAdapter
import com.example.tutortrack.ui.adapters.SessionWithDetails
import com.example.tutortrack.ui.session.SessionViewModel
import com.example.tutortrack.ui.student.StudentViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var sessionAdapter: SessionAdapter
    
    private val currencyFormat = NumberFormat.getCurrencyInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModels()
        setupRecyclerView()
        setupPeriodSpinner()
        loadData()
    }
    
    private fun setupViewModels() {
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter(
            onPaymentStatusClick = { session, isPaid, paidDate ->
                handlePaymentStatusChange(session, isPaid, paidDate)
            },
            onSessionDelete = { session ->
                deleteSession(session)
            }
        )
        binding.recyclerViewRecentSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRecentSessions.adapter = sessionAdapter
    }

    private fun setupPeriodSpinner() {
        val periods = arrayOf(
            getString(R.string.income_period_total),
            getString(R.string.income_period_month),
            getString(R.string.income_period_week)
        )
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            periods
        )
        
        binding.spinnerPeriod.adapter = adapter
        
        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateIncomeDisplay(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
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

    private fun updateIncomeDisplay(periodPosition: Int) {
        when (periodPosition) {
            0 -> { // Total
                binding.incomeTitle.text = getString(R.string.income_period_total)
                sessionViewModel.getTotalIncome().observe(viewLifecycleOwner) { total ->
                    binding.textTotalIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", total ?: 0.0)
                }
            }
            1 -> { // Monthly
                binding.incomeTitle.text = getString(R.string.income_period_month)
                sessionViewModel.getMonthlyIncome().observe(viewLifecycleOwner) { total ->
                    binding.textTotalIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", total ?: 0.0)
                }
            }
            2 -> { // Weekly
                binding.incomeTitle.text = getString(R.string.income_period_week)
                sessionViewModel.getWeeklyIncome().observe(viewLifecycleOwner) { total ->
                    binding.textTotalIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", total ?: 0.0)
                }
            }
        }
    }
    
    private fun loadData() {
        // Load initial income (total)
        updateIncomeDisplay(0)
        
        // Load student count
        studentViewModel.allStudents.observe(viewLifecycleOwner) { students ->
            binding.textStudentCount.text = students.size.toString()
        }
        
        // Load session count and recent sessions
        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
            // Display session count
            binding.textSessionCount.text = sessionsWithDetails.size.toString()
            
            // Calculate date 7 days ago
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val sevenDaysAgo = calendar.time
            
            // Filter and sort recent sessions (last 7 days)
            val recentSessions = sessionsWithDetails
                .filter { it.session.date >= sevenDaysAgo }
                .sortedByDescending { it.session.date }
            
            sessionAdapter.submitList(recentSessions)
            
            binding.textNoSessions.visibility = if (recentSessions.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}