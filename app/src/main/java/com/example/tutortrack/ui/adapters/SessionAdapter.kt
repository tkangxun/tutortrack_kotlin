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
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Data class for holding session with related student and class type information
data class SessionWithDetails(
    val session: Session,
    val student: Student? = null,
    val classType: ClassType? = null
)

class SessionAdapter(
    private val onPaymentStatusClick: ((Session, Boolean, Date?) -> Unit)? = null,
    private val onSessionDelete: ((Session) -> Unit)? = null
) : ListAdapter<SessionWithDetails, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val sessionWithDetails = getItem(position)
        holder.bind(sessionWithDetails)
    }

    inner class SessionViewHolder(private val binding: ItemSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        private val dayFormat = SimpleDateFormat("dd", Locale.getDefault())

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
                
                // Set full date
                textSessionDate.text = dateFormat.format(session.date)
                textSessionDuration.text = String.format(Locale.getDefault(), "%.1f hours", session.durationMinutes / 60.0)
                textSessionAmount.text = String.format(Locale.getDefault(), "SGD $%.2f", session.amount)
                
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
                    student?.let { student ->
                        val bundle = Bundle().apply {
                            putLong("studentId", student.id)
                        }
                        itemView.findNavController().navigate(R.id.studentDetailFragment, bundle)
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
                
                // Handle paid date display
                if (session.isPaid && session.paidDate != null) {
                    textPaidDate.visibility = View.VISIBLE
                    textPaidDate.text = "Paid on: ${dateFormat.format(session.paidDate)}"
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

    private class SessionDiffCallback : DiffUtil.ItemCallback<SessionWithDetails>() {
        override fun areItemsTheSame(oldItem: SessionWithDetails, newItem: SessionWithDetails): Boolean {
            return oldItem.session.id == newItem.session.id
        }

        override fun areContentsTheSame(oldItem: SessionWithDetails, newItem: SessionWithDetails): Boolean {
            return oldItem == newItem
        }
    }
} 