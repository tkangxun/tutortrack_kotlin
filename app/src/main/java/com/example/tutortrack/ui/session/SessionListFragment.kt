package com.example.tutortrack.ui.session

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.TextView
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutortrack.R
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.databinding.FragmentSessionListBinding
import com.example.tutortrack.databinding.DialogBulkPaymentBinding
import com.example.tutortrack.databinding.ItemSessionCheckboxBinding
import com.example.tutortrack.ui.adapters.SessionGroupAdapter
import com.example.tutortrack.ui.adapters.SessionListItem
import com.example.tutortrack.ui.adapters.SessionWithDetails
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.text.NumberFormat
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
        // If marking as unpaid, update visibility immediately
        if (!isPaid) {
            binding.fabBulkPayment.visibility = View.VISIBLE
        }
        
        val updatedSession = session.copy(
            isPaid = isPaid,
            paidDate = if (isPaid) paidDate else null
        )
        sessionViewModel.updateSession(updatedSession)
        
        // If marking as paid, check if we need to hide the button (requires checking all sessions)
        if (isPaid) {
            // Check if there are any other unpaid sessions left
            val currentSessions = sessionViewModel.allSessionsWithDetails.value ?: emptyList()
            val otherUnpaidSessions = currentSessions.filter { it.session.id != session.id && !it.session.isPaid }
            
            if (otherUnpaidSessions.isEmpty()) {
                binding.fabBulkPayment.visibility = View.GONE
            }
        }
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
            
            // Ensure proper FAB visibility whenever the data changes
            val tabPosition = binding.tabLayout.selectedTabPosition
            when (tabPosition) {
                0 -> { // All sessions tab
                    val hasUnpaidSessions = sessionsWithDetails.any { !it.session.isPaid }
                    binding.fabBulkPayment.visibility = if (hasUnpaidSessions) View.VISIBLE else View.GONE
                }
                1 -> { // Paid sessions tab
                    binding.fabBulkPayment.visibility = View.GONE
                }
                2 -> { // Unpaid sessions tab
                    val unpaidSessions = sessionsWithDetails.filter { !it.session.isPaid }
                    binding.fabBulkPayment.visibility = if (unpaidSessions.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }
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
                        // Clear previous observers
                        sessionViewModel.allSessionsWithDetails.removeObservers(viewLifecycleOwner)
                        
                        // Set new observer
                        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
                            updateSessionList(sessionsWithDetails)
                            
                            // Directly check if there are any unpaid sessions
                            val hasUnpaidSessions = sessionsWithDetails.any { !it.session.isPaid }
                            binding.fabBulkPayment.visibility = if (hasUnpaidSessions) View.VISIBLE else View.GONE
                        }
                    }
                    1 -> { // Paid sessions
                        // Clear previous observers
                        sessionViewModel.allSessionsWithDetails.removeObservers(viewLifecycleOwner)
                        
                        // Set new observer
                        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
                            val paidSessions = sessionsWithDetails.filter { it.session.isPaid }
                            updateSessionList(paidSessions)
                            // No need to show bulk payment button in paid sessions tab
                            binding.fabBulkPayment.visibility = View.GONE
                        }
                    }
                    2 -> { // Unpaid sessions
                        // Clear previous observers
                        sessionViewModel.allSessionsWithDetails.removeObservers(viewLifecycleOwner)
                        
                        // Set new observer
                        sessionViewModel.allSessionsWithDetails.observe(viewLifecycleOwner) { sessionsWithDetails ->
                            val unpaidSessions = sessionsWithDetails.filter { !it.session.isPaid }
                            updateSessionList(unpaidSessions)
                            // Always show bulk payment button in unpaid tab if there are sessions
                            binding.fabBulkPayment.visibility = if (unpaidSessions.isNotEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun updateBulkPaymentButtonVisibility() {
        // Get current tab position
        val tabPosition = binding.tabLayout.selectedTabPosition
        
        // Get the current sessions data directly from the view model
        val allSessions = sessionViewModel.allSessionsWithDetails.value ?: emptyList()
        val hasUnpaidSessions = allSessions.any { !it.session.isPaid }
        
        // Only show the bulk payment button if in "All" or "Unpaid" tab and there are unpaid sessions
        when (tabPosition) {
            0 -> { // All sessions tab
                binding.fabBulkPayment.visibility = if (hasUnpaidSessions) View.VISIBLE else View.GONE
            }
            1 -> { // Paid sessions tab - never show bulk payment button
                binding.fabBulkPayment.visibility = View.GONE
            }
            2 -> { // Unpaid sessions tab - always show if there are any sessions
                binding.fabBulkPayment.visibility = if (hasUnpaidSessions) View.VISIBLE else View.GONE
            }
        }
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
        
        binding.fabBulkPayment.setOnClickListener {
            showBulkPaymentDialog()
        }
        
        binding.buttonDateSearch.setOnClickListener {
            showDatePickerForSearch()
        }
    }
    
    private fun showBulkPaymentDialog() {
        // Get all unpaid sessions
        val unpaidSessions = getAllUnpaidSessions()
        
        if (unpaidSessions.isEmpty()) {
            Toast.makeText(requireContext(), "No unpaid sessions found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bulk_payment, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
        
        val dialog = dialogBuilder.create()
        dialog.show()
        
        // Get references to the dialog views
        val checkboxContainer = dialogView.findViewById<ViewGroup>(R.id.sessionCheckboxContainer)
        val checkboxSelectAll = dialogView.findViewById<CheckBox>(R.id.checkboxSelectAll)
        val textTotalAmount = dialogView.findViewById<TextView>(R.id.textTotalAmount)
        val editPaymentDate = dialogView.findViewById<EditText>(R.id.editPaymentDate)
        val buttonCancel = dialogView.findViewById<View>(R.id.buttonCancel)
        val buttonMarkAsPaid = dialogView.findViewById<View>(R.id.buttonMarkAsPaid)
        
        // Set current date as the default payment date
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val currentDate = calendar.time
        editPaymentDate.setText(dateFormat.format(currentDate))
        
        // Add click listener to payment date field to show date picker
        editPaymentDate.setOnClickListener {
            showDatePickerDialog(editPaymentDate, calendar)
        }
        
        // Add session checkboxes to the container
        val sessionCheckboxMap = mutableMapOf<Long, Boolean>()
        val sessionViewMap = mutableMapOf<Long, View>()
        
        for (sessionWithDetails in unpaidSessions) {
            val session = sessionWithDetails.session
            val student = sessionWithDetails.student
            val itemBinding = ItemSessionCheckboxBinding.inflate(LayoutInflater.from(requireContext()), checkboxContainer, false)
            
            // Format the session text with date, class type, and duration
            val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val studentName = student?.name ?: "Unknown Student"
            val classTypeName = sessionWithDetails.classType?.name ?: "Unknown"
            val durationHours = session.durationMinutes / 60.0
            val durationText = if (durationHours == 1.0) "1 hr" else "$durationHours hrs"
            
            itemBinding.checkboxSession.text = "${dateFormatter.format(session.date)} - $studentName - $classTypeName ($durationText)"
            
            // Format amount with US currency
            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
            itemBinding.textSessionAmount.text = currencyFormatter.format(session.amount)
            
            // Set initial checked state and update map
            itemBinding.checkboxSession.isChecked = true
            sessionCheckboxMap[session.id] = true
            
            // Add change listener to update the map when checkbox state changes
            itemBinding.checkboxSession.setOnCheckedChangeListener { _, isChecked ->
                sessionCheckboxMap[session.id] = isChecked
                updateTotalAmount(sessionCheckboxMap, unpaidSessions, textTotalAmount)
                
                // Update the select all checkbox state
                val allChecked = sessionCheckboxMap.values.all { it }
                val anyChecked = sessionCheckboxMap.values.any { it }
                
                checkboxSelectAll.isChecked = allChecked
                // Use indeterminate state if some but not all are checked
                if (anyChecked && !allChecked) {
                    // We can't set indeterminate state programmatically
                    // But we can at least make sure it's not checked
                    checkboxSelectAll.isChecked = false
                }
            }
            
            // Store the view for later reference
            sessionViewMap[session.id] = itemBinding.root
            
            // Add the checkbox item to the container
            checkboxContainer.addView(itemBinding.root)
        }
        
        // Set up select all checkbox listener
        checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
            // Update all checkboxes to match the select all state
            for (sessionId in sessionCheckboxMap.keys) {
                sessionCheckboxMap[sessionId] = isChecked
                
                // Also update the actual checkbox UI
                val view = sessionViewMap[sessionId]
                view?.findViewById<CheckBox>(R.id.checkboxSession)?.isChecked = isChecked
            }
            
            updateTotalAmount(sessionCheckboxMap, unpaidSessions, textTotalAmount)
        }
        
        // Initial update of total amount
        updateTotalAmount(sessionCheckboxMap, unpaidSessions, textTotalAmount)
        
        // Set up button listeners
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        buttonMarkAsPaid.setOnClickListener {
            // Get selected session IDs
            val selectedSessionIds = sessionCheckboxMap.filter { it.value }.map { it.key }
            
            if (selectedSessionIds.isEmpty()) {
                Toast.makeText(requireContext(), "No sessions selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Process the bulk payment
            val paymentDate = calendar.time
            processBulkPayment(selectedSessionIds, paymentDate)
            dialog.dismiss()
        }
    }
    
    private fun getAllUnpaidSessions(): List<SessionWithDetails> {
        return allSessionItems.filterIsInstance<SessionListItem.SessionItem>()
            .map { it.sessionWithDetails }
            .filter { !it.session.isPaid }
    }
    
    private fun updateTotalAmount(
        sessionCheckboxMap: Map<Long, Boolean>,
        unpaidSessions: List<SessionWithDetails>,
        textTotalAmount: TextView
    ) {
        // Calculate total amount for selected sessions
        val totalAmount = unpaidSessions
            .filter { sessionCheckboxMap[it.session.id] == true }
            .sumByDouble { it.session.amount }
        
        // Format and display the total amount using US currency
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        textTotalAmount.text = "Total: ${currencyFormatter.format(totalAmount)}"
    }
    
    private fun showDatePickerDialog(editText: EditText, calendar: Calendar) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDate = calendar.time
                
                // Update the text field with the formatted date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                editText.setText(dateFormat.format(selectedDate))
            },
            year, month, day
        )
        
        // Limit the date picker to today or earlier
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        
        datePickerDialog.show()
    }
    
    private fun processBulkPayment(sessionIds: List<Long>, paymentDate: Date) {
        if (sessionIds.isEmpty()) return
        
        // Check if we're marking all unpaid sessions as paid
        val allSessions = sessionViewModel.allSessionsWithDetails.value ?: emptyList()
        val unpaidSessionIds = allSessions.filter { !it.session.isPaid }.map { it.session.id }
        val markingAllAsPaid = sessionIds.containsAll(unpaidSessionIds) && unpaidSessionIds.isNotEmpty()
        
        // If marking all as paid, hide the button immediately for better UX
        if (markingAllAsPaid) {
            binding.fabBulkPayment.visibility = View.GONE
        }
        
        // Update all selected sessions to paid with the same payment date
        sessionViewModel.bulkUpdateSessionsPaid(sessionIds, paymentDate)
        
        // Show success message
        val sessionText = if (sessionIds.size == 1) "session" else "sessions"
        Toast.makeText(
            requireContext(),
            "${sessionIds.size} $sessionText marked as paid",
            Toast.LENGTH_SHORT
        ).show()
        
        // Update visibility based on the current tab
        updateBulkPaymentButtonVisibility()
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

    override fun onResume() {
        super.onResume()
        
        // Instead of just calling updateBulkPaymentButtonVisibility, we need to force
        // a proper check for the current tab since the LiveData might not have triggered yet
        val tabPosition = binding.tabLayout.selectedTabPosition
        
        // Immediately check for unpaid sessions to update FAB visibility
        sessionViewModel.allSessionsWithDetails.value?.let { sessions ->
            when (tabPosition) {
                0 -> { // All sessions
                    val hasUnpaidSessions = sessions.any { !it.session.isPaid }
                    binding.fabBulkPayment.visibility = if (hasUnpaidSessions) View.VISIBLE else View.GONE
                }
                1 -> { // Paid sessions
                    binding.fabBulkPayment.visibility = View.GONE
                }
                2 -> { // Unpaid sessions
                    val unpaidSessions = sessions.filter { !it.session.isPaid }
                    binding.fabBulkPayment.visibility = if (unpaidSessions.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        
        // Force refresh data when returning to the fragment
        sessionViewModel.refreshData()
    }
} 