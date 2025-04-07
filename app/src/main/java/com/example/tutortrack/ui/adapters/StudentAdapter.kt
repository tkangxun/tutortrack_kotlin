package com.example.tutortrack.ui.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.ItemStudentBinding

class StudentAdapter(
    private val onItemClick: (Student) -> Unit,
    private val onStudentDelete: ((Student) -> Unit)? = null,
    private val onStudentArchive: ((Student) -> Unit)? = null,
    private val onStudentUnarchive: ((Student) -> Unit)? = null
) : ListAdapter<Student, StudentAdapter.StudentViewHolder>(StudentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = getItem(position)
        holder.bind(student)
        holder.itemView.setOnClickListener { onItemClick(student) }
    }

    inner class StudentViewHolder(private val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(student: Student) {
            binding.apply {
                // Set avatar text to first letter of name
                avatarText.text = student.name.firstOrNull()?.toString() ?: ""

                // Set student name
                textStudentName.text = student.name

                // Set grade
                textStudentGrade.text = student.grade

                // Handle phone number visibility in the new layout
                if (student.phone != null && student.phone.isNotEmpty()) {
                    phoneSpacing.visibility = View.VISIBLE
                    iconPhone.visibility = View.VISIBLE
                    textStudentPhone.visibility = View.VISIBLE
                    textStudentPhone.text = student.phone
                } else {
                    phoneSpacing.visibility = View.GONE
                    iconPhone.visibility = View.GONE
                    textStudentPhone.visibility = View.GONE
                }
                
                // Set up long click listener for actions (delete/archive)
                root.setOnLongClickListener {
                    showActionDialog(student)
                    true
                }
            }
        }
        
        private fun showActionDialog(student: Student) {
            val options = if (student.isArchived) {
                arrayOf("Unarchive Student", "Delete Student")
            } else {
                arrayOf("Archive Student", "Delete Student")
            }
            
            AlertDialog.Builder(itemView.context)
                .setTitle("Student Actions")
                .setItems(options) { _, which ->
                    when {
                        // Archive or Unarchive option
                        which == 0 && !student.isArchived -> {
                            showArchiveConfirmation(student)
                        }
                        which == 0 && student.isArchived -> {
                            showUnarchiveConfirmation(student)
                        }
                        // Delete option (always the last option)
                        else -> {
                            showDeleteConfirmation(student)
                        }
                    }
                }
                .show()
        }
        
        private fun showArchiveConfirmation(student: Student) {
            onStudentArchive?.let { archiveHandler ->
                AlertDialog.Builder(itemView.context)
                    .setTitle("Archive Student")
                    .setMessage("Are you sure you want to archive ${student.name}? Archived students won't appear in the student list or when creating sessions, but their past sessions will be preserved.")
                    .setPositiveButton("Archive") { _, _ ->
                        archiveHandler(student)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        
        private fun showUnarchiveConfirmation(student: Student) {
            onStudentUnarchive?.let { unarchiveHandler ->
                AlertDialog.Builder(itemView.context)
                    .setTitle("Unarchive Student")
                    .setMessage("Do you want to unarchive ${student.name}? This will make the student active again.")
                    .setPositiveButton("Unarchive") { _, _ ->
                        unarchiveHandler(student)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        
        private fun showDeleteConfirmation(student: Student) {
            onStudentDelete?.let { deleteHandler ->
                AlertDialog.Builder(itemView.context)
                    .setTitle("Delete Student")
                    .setMessage("Are you sure you want to delete ${student.name}? This will also delete all associated sessions and cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteHandler(student)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private class StudentDiffCallback : DiffUtil.ItemCallback<Student>() {
        override fun areItemsTheSame(oldItem: Student, newItem: Student): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Student, newItem: Student): Boolean {
            return oldItem == newItem
        }
    }
} 