package com.example.tutortrack.ui.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.ItemStudentBinding

class StudentAdapter(
    private val onItemClick: (Student) -> Unit,
    private val onStudentDelete: ((Student) -> Unit)? = null
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
                // Set student name and avatar
                textStudentName.text = student.name
                avatarText.text = student.name.firstOrNull()?.toString() ?: "?"
                
                // Set grade and phone info
                textStudentGrade.text = student.grade
                textStudentPhone.text = student.phone
                
                // Set up long click listener for delete functionality
                root.setOnLongClickListener {
                    onStudentDelete?.let { deleteHandler ->
                        showDeleteConfirmation(student, deleteHandler)
                    }
                    true
                }
            }
        }
        
        private fun showDeleteConfirmation(student: Student, onDelete: (Student) -> Unit) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Student")
                .setMessage("Are you sure you want to delete ${student.name}? This will also delete all associated sessions and cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    onDelete(student)
                }
                .setNegativeButton("Cancel", null)
                .show()
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