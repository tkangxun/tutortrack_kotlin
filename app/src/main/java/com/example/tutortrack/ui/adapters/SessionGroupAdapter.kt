package com.example.tutortrack.ui.adapters

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutortrack.R
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.databinding.ItemSessionBinding
import com.example.tutortrack.databinding.ItemSessionDayHeaderBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Sealed class for item types
sealed class SessionListItem {
    // The header includes a flag to track if the group is expanded
    data class SessionHeader(
        val date: Date, 
        val dayString: String,
        var isExpanded: Boolean = true,
        val originalIndex: Int // To track position in original sorted list
    ) : SessionListItem()
    
    data class SessionItem(val sessionWithDetails: SessionWithDetails) : SessionListItem()
}

class SessionGroupAdapter(
    private val onPaymentStatusClick: ((Session, Boolean, Date?) -> Unit)? = null,
    private val onSessionDelete: ((Session) -> Unit)? = null,
    private val onSessionEdit: ((Session) -> Unit)? = null
) : ListAdapter<SessionListItem, RecyclerView.ViewHolder>(SessionDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SESSION = 1

        // Helper function to transform a list of sessions into a grouped list with headers
        fun groupSessionsByDay(sessions: List<SessionWithDetails>): List<SessionListItem> {
            if (sessions.isEmpty()) return emptyList()

            // Sort sessions by date (newest first)
            val sortedSessions = sessions.sortedByDescending { it.session.date }
            
            // First pass: Create headers and record session indices
            val dayHeaders = mutableMapOf<String, SessionListItem.SessionHeader>()
            val sessionGroups = mutableMapOf<String, MutableList<SessionWithDetails>>()
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            
            sortedSessions.forEachIndexed { index, sessionWithDetails ->
                val session = sessionWithDetails.session
                val dayString = dayFormat.format(session.date)
                
                // Create header if it doesn't exist
                if (!dayHeaders.containsKey(dayString)) {
                    val headerText = dateFormat.format(session.date)
                    dayHeaders[dayString] = SessionListItem.SessionHeader(
                        date = session.date,
                        dayString = headerText,
                        isExpanded = true,
                        originalIndex = index
                    )
                    sessionGroups[dayString] = mutableListOf()
                }
                
                // Add session to its group
                sessionGroups[dayString]?.add(sessionWithDetails)
            }
            
            // Combine into final list with all groups expanded by default
            val result = mutableListOf<SessionListItem>()
            
            // Sort headers by date (newest first) using originalIndex
            val sortedHeaders = dayHeaders.values.sortedBy { it.originalIndex }
            
            for (header in sortedHeaders) {
                val dayKey = dayFormat.format(header.date)
                result.add(header)
                
                // Add sessions if group is expanded
                if (header.isExpanded) {
                    sessionGroups[dayKey]?.forEach { session ->
                        result.add(SessionListItem.SessionItem(session))
                    }
                }
            }
            
            return result
        }
    }
    
    // Keep track of the original full list and expanded state of groups
    private val allItems = mutableListOf<SessionListItem>()
    private val expandedState = mutableMapOf<String, Boolean>()
    private val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    
    // Override submitList to store the full data set
    override fun submitList(list: List<SessionListItem>?) {
        list?.let {
            // Store the full list
            allItems.clear()
            allItems.addAll(it)
            
            // Remember expanded state for each header
            val headers = it.filterIsInstance<SessionListItem.SessionHeader>()
            headers.forEach { header ->
                val dayKey = dayFormat.format(header.date)
                if (!expandedState.containsKey(dayKey)) {
                    expandedState[dayKey] = header.isExpanded
                }
                // Apply remembered state
                header.isExpanded = expandedState[dayKey] ?: true
            }
        }
        
        // Submit the adjusted list with proper expanded/collapsed state
        super.submitList(list)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SessionListItem.SessionHeader -> TYPE_HEADER
            is SessionListItem.SessionItem -> TYPE_SESSION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemSessionDayHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }
            TYPE_SESSION -> {
                val binding = ItemSessionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SessionViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SessionListItem.SessionHeader -> (holder as HeaderViewHolder).bind(item)
            is SessionListItem.SessionItem -> (holder as SessionViewHolder).bind(item.sessionWithDetails)
        }
    }

    // Method to toggle group expansion - also accessible from fragment
    fun toggleGroupExpansion(header: SessionListItem.SessionHeader) {
        val dayKey = dayFormat.format(header.date)
        
        // Toggle and save expansion state
        header.isExpanded = !header.isExpanded
        expandedState[dayKey] = header.isExpanded
        
        // Recreate the list with the new expansion state
        val newList = mutableListOf<SessionListItem>()
        
        // First add all headers
        val headers = allItems.filterIsInstance<SessionListItem.SessionHeader>()
        val sessionItems = allItems.filterIsInstance<SessionListItem.SessionItem>()
        
        headers.forEach { h ->
            val hDayKey = dayFormat.format(h.date)
            // Apply the current expansion state
            h.isExpanded = expandedState[hDayKey] ?: true
            newList.add(h)
            
            // Only add sessions for this header if it's expanded
            if (h.isExpanded) {
                // Find sessions for this day
                sessionItems
                    .filter { dayFormat.format(it.sessionWithDetails.session.date) == hDayKey }
                    .forEach { newList.add(it) }
            }
        }
        
        // Update the adapter with the new list
        super.submitList(newList)
    }
    
    // Find header at position
    fun getHeaderAtPosition(position: Int): SessionListItem.SessionHeader? {
        return if (position >= 0 && position < currentList.size) {
            val item = currentList[position]
            if (item is SessionListItem.SessionHeader) item else null
        } else null
    }
    
    // Helper function to find closest header to a date
    fun findClosestHeaderToDate(targetDate: Date): SessionListItem.SessionHeader? {
        val headers = currentList.filterIsInstance<SessionListItem.SessionHeader>()
        if (headers.isEmpty()) return null
        
        var closestHeader = headers.first()
        var minDifference = Long.MAX_VALUE
        
        headers.forEach { header ->
            val difference = Math.abs(header.date.time - targetDate.time)
            if (difference < minDifference) {
                minDifference = difference
                closestHeader = header
            }
        }
        
        return closestHeader
    }

    // ViewHolder for day headers
    inner class HeaderViewHolder(private val binding: ItemSessionDayHeaderBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(header: SessionListItem.SessionHeader) {
            binding.textDayHeader.text = header.dayString
            
            // Set the collapse/expand icon based on current state
            updateExpandCollapseIcon(header.isExpanded)
            
            // Set click listener for the whole header
            binding.root.setOnClickListener {
                toggleGroupExpansion(header)
                // Update the icon immediately after toggling
                updateExpandCollapseIcon(!header.isExpanded)
            }
        }
        
        private fun updateExpandCollapseIcon(isExpanded: Boolean) {
            binding.imageCollapseExpand.setImageResource(
                if (isExpanded) R.drawable.ic_collapse 
                else R.drawable.ic_expand
            )
        }
    }

    // ViewHolder for session items - mostly copied from original SessionAdapter
    inner class SessionViewHolder(private val binding: ItemSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        private val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        private val combinedDayDateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())

        fun bind(sessionWithDetails: SessionWithDetails) {
            val session = sessionWithDetails.session
            val student = sessionWithDetails.student
            val classType = sessionWithDetails.classType
            
            binding.apply {
                // Show student name if available
                textStudentName.text = student?.name ?: "Unknown Student"
                
                // Show class type if available
                textClassType.text = classType?.name ?: "Unknown Class Type"
                
                // Set up the calendar date display
                textDateMonth.text = monthFormat.format(session.date).uppercase()
                textDateDay.text = dayFormat.format(session.date)
                
                // Day of week is now only shown in the header, not in individual cards


                // Set session duration and amount
                textSessionDuration.text = String.format(Locale.getDefault(), "%.1f hrs", session.durationMinutes / 60.0)
                textSessionAmount.text = String.format(Locale.getDefault(), "$%.2f", session.amount)
                
                updatePaymentStatus(session)
                
                // Set up click listener on the payment status chip
                chipPaidStatus.setOnClickListener {
                    // Toggle payment status
                    val newPaidStatus = !session.isPaid
                    
                    if (newPaidStatus) {
                        // If marking as paid, show a date picker to select the paid date
                        showPaidDatePicker(session)
                    } else if (session.isPaid) {
                        // If marking as unpaid and it was previously paid, show confirmation dialog
                        showUnpaidConfirmationDialog(session)
                    } else {
                        // If it was already unpaid, do nothing
                        onPaymentStatusClick?.invoke(session, false, null)
                    }
                }
                
                // Set up click listener for navigation to student detail
                root.setOnClickListener {
                    // If onSessionEdit is provided, use it to edit the session
                    if (onSessionEdit != null) {
                        onSessionEdit.invoke(session)
                    } else {
                        // Otherwise, navigate to student detail (original behavior)
                        student?.let { student ->
                            val bundle = Bundle().apply {
                                putLong("studentId", student.id)
                            }
                            itemView.findNavController().navigate(R.id.studentDetailFragment, bundle)
                        }
                    }
                }
                
                // Set up long click listener for delete functionality
                root.setOnLongClickListener {
                    showDeleteConfirmation(session)
                    true
                }
            }
        }
        
        private fun updatePaymentStatus(session: Session) {
            binding.apply {
                chipPaidStatus.text = if (session.isPaid) "Paid" else "Unpaid"
                chipPaidStatus.chipBackgroundColor = ContextCompat.getColorStateList(
                    itemView.context,
                    if (session.isPaid) R.color.success else R.color.warning
                )
                
                // Handle paid date display with shorter format
                if (session.isPaid && session.paidDate != null) {
                    textPaidDate.visibility = View.VISIBLE
                    // Use a more compact date format
                    val shortDateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    textPaidDate.text = "Paid: ${shortDateFormat.format(session.paidDate)}"
                } else {
                    textPaidDate.visibility = View.GONE
                }
            }
        }
        
        private fun showPaidDatePicker(session: Session) {
            // Show date picker dialog to select paid date
            val calendar = Calendar.getInstance()
            
            val datePickerDialog = DatePickerDialog(
                itemView.context,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val paidDate = calendar.time
                    
                    // Notify the fragment about the payment status change with the selected date
                    onPaymentStatusClick?.invoke(session, true, paidDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            
            // Set max date to today
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            
            datePickerDialog.show()
        }
        
        private fun showUnpaidConfirmationDialog(session: Session) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Mark as Unpaid")
                .setMessage("Are you sure you want to mark this session as unpaid? This will remove the payment date information.")
                .setPositiveButton("Yes") { _, _ ->
                    onPaymentStatusClick?.invoke(session, false, null)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        private fun showDeleteConfirmation(session: Session) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Session")
                .setMessage("Are you sure you want to delete this session? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    onSessionDelete?.invoke(session)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // DiffUtil callback for comparing items
    class SessionDiffCallback : DiffUtil.ItemCallback<SessionListItem>() {
        override fun areItemsTheSame(oldItem: SessionListItem, newItem: SessionListItem): Boolean {
            return when {
                oldItem is SessionListItem.SessionHeader && newItem is SessionListItem.SessionHeader ->
                    oldItem.dayString == newItem.dayString
                oldItem is SessionListItem.SessionItem && newItem is SessionListItem.SessionItem ->
                    oldItem.sessionWithDetails.session.id == newItem.sessionWithDetails.session.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: SessionListItem, newItem: SessionListItem): Boolean {
            return when {
                oldItem is SessionListItem.SessionHeader && newItem is SessionListItem.SessionHeader ->
                    oldItem.dayString == newItem.dayString && oldItem.isExpanded == newItem.isExpanded
                oldItem is SessionListItem.SessionItem && newItem is SessionListItem.SessionItem ->
                    oldItem.sessionWithDetails == newItem.sessionWithDetails
                else -> false
            }
        }
    }
} 