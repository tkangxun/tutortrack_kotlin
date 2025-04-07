package com.example.tutortrack.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutortrack.R
import com.example.tutortrack.databinding.FragmentHomeBinding
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.ui.adapters.SessionGroupAdapter
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
    private lateinit var sessionAdapter: SessionGroupAdapter
    
    private val currencyFormat = NumberFormat.getCurrencyInstance()
    private val prefs by lazy { requireContext().getSharedPreferences("income_prefs", 0) }
    private val KEY_SELECTED_PERIOD = "selected_income_period"
    private val KEY_SELECTED_UNPAID_PERIOD = "selected_unpaid_period"

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
        setupUnpaidPeriodSpinner()
        loadData()
    }
    
    private fun setupViewModels() {
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        sessionAdapter = SessionGroupAdapter(
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
            getString(R.string.income_period_year),
            getString(R.string.income_period_month),
            getString(R.string.income_period_week)
        )
        
        // Create custom adapter with a specific layout for the spinner header
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.item_spinner_header,
            R.id.text1,
            periods
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return super.getDropDownView(position, convertView, parent).apply {
                    // Change background color for dropdown items to match the surface color
                    setBackgroundColor(resources.getColor(R.color.surface, null))
                    findViewById<TextView>(R.id.text1).setTextColor(resources.getColor(R.color.text_primary, null))
                }
            }
        }
        
        binding.spinnerPeriod.adapter = adapter
        
        // Restore previously selected period
        val savedPosition = prefs.getInt(KEY_SELECTED_PERIOD, 0)
        binding.spinnerPeriod.setSelection(savedPosition)
        
        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Make selected text color white for better visibility on blue background
                view?.findViewById<TextView>(R.id.text1)?.setTextColor(resources.getColor(R.color.white, null))
                
                // Save selected position
                prefs.edit().putInt(KEY_SELECTED_PERIOD, position).apply()
                updateIncomeDisplay(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun setupUnpaidPeriodSpinner() {
        val periods = arrayOf(
            getString(R.string.unpaid_period_total),
            getString(R.string.unpaid_period_year),
            getString(R.string.unpaid_period_month),
            getString(R.string.unpaid_period_week)
        )
        
        // Create custom adapter with a specific layout for the spinner header
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.item_spinner_header,
            R.id.text1,
            periods
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return super.getDropDownView(position, convertView, parent).apply {
                    // Change background color for dropdown items to match the surface color
                    setBackgroundColor(resources.getColor(R.color.surface, null))
                    findViewById<TextView>(R.id.text1).setTextColor(resources.getColor(R.color.text_primary, null))
                }
            }
        }
        
        binding.spinnerUnpaidPeriod.adapter = adapter
        
        // Restore previously selected period
        val savedPosition = prefs.getInt(KEY_SELECTED_UNPAID_PERIOD, 0)
        binding.spinnerUnpaidPeriod.setSelection(savedPosition)
        
        binding.spinnerUnpaidPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Make selected text color black for better visibility on yellow background
                view?.findViewById<TextView>(R.id.text1)?.setTextColor(resources.getColor(R.color.black, null))
                
                // Save selected position
                prefs.edit().putInt(KEY_SELECTED_UNPAID_PERIOD, position).apply()
                updateUnpaidIncomeDisplay(position)
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
                sessionViewModel.getTotalIncome().observe(viewLifecycleOwner) { total ->
                    binding.textTotalIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", total ?: 0.0)
                }
            }
            1 -> { // Yearly
                sessionViewModel.getYearlyIncome().observe(viewLifecycleOwner) { total ->
                    binding.textTotalIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", total ?: 0.0)
                }
            }
            2 -> { // Monthly
                sessionViewModel.getMonthlyIncome().observe(viewLifecycleOwner) { total ->
                    binding.textTotalIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", total ?: 0.0)
                }
            }
            3 -> { // Weekly
                sessionViewModel.getWeeklyIncome().observe(viewLifecycleOwner) { total ->
                    binding.textTotalIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", total ?: 0.0)
                }
            }
        }
    }
    
    private fun updateUnpaidIncomeDisplay(periodPosition: Int) {
        when (periodPosition) {
            0 -> { // Total
                sessionViewModel.getTotalUnpaidIncome().observe(viewLifecycleOwner) { unpaid ->
                    binding.textUnpaidIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", unpaid ?: 0.0)
                }
            }
            1 -> { // Yearly
                sessionViewModel.getYearlyUnpaidIncome().observe(viewLifecycleOwner) { unpaid ->
                    binding.textUnpaidIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", unpaid ?: 0.0)
                }
            }
            2 -> { // Monthly
                sessionViewModel.getMonthlyUnpaidIncome().observe(viewLifecycleOwner) { unpaid ->
                    binding.textUnpaidIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", unpaid ?: 0.0)
                }
            }
            3 -> { // Weekly
                sessionViewModel.getWeeklyUnpaidIncome().observe(viewLifecycleOwner) { unpaid ->
                    binding.textUnpaidIncome.text = String.format(Locale.getDefault(), "SGD $%.2f", unpaid ?: 0.0)
                }
            }
        }
    }
    
    private fun loadData() {
        // Load income based on saved period
        val savedPosition = prefs.getInt(KEY_SELECTED_PERIOD, 0)
        updateIncomeDisplay(savedPosition)
        
        // Load unpaid income based on saved period
        val savedUnpaidPosition = prefs.getInt(KEY_SELECTED_UNPAID_PERIOD, 0)
        updateUnpaidIncomeDisplay(savedUnpaidPosition)
        
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
            
            // Group the sessions by day
            val groupedItems = SessionGroupAdapter.groupSessionsByDay(recentSessions)
            sessionAdapter.submitList(groupedItems)
            
            binding.textNoSessions.visibility = if (recentSessions.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}