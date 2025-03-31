package com.example.tutortrack.ui.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutortrack.data.model.ClassType
import com.example.tutortrack.databinding.ItemClassTypeBinding
import java.util.Locale

class ClassTypeAdapter(
    private val onItemClick: (ClassType) -> Unit,
    private val onClassTypeDelete: ((ClassType) -> Unit)? = null
) : ListAdapter<ClassType, ClassTypeAdapter.ClassTypeViewHolder>(ClassTypeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassTypeViewHolder {
        val binding = ItemClassTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClassTypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassTypeViewHolder, position: Int) {
        val classType = getItem(position)
        holder.bind(classType)
        holder.itemView.setOnClickListener { onItemClick(classType) }
    }

    inner class ClassTypeViewHolder(private val binding: ItemClassTypeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(classType: ClassType) {
            binding.apply {
                textClassName.text = classType.name
                textClassRate.text = String.format(Locale.getDefault(), "SGD $%.2f/hr", classType.hourlyRate)
                
                // Set up long click listener for delete functionality
                root.setOnLongClickListener {
                    onClassTypeDelete?.let { deleteHandler ->
                        showDeleteConfirmation(classType, deleteHandler)
                    }
                    true
                }
            }
        }
        
        private fun showDeleteConfirmation(classType: ClassType, onDelete: (ClassType) -> Unit) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Class Type")
                .setMessage("Are you sure you want to delete ${classType.name}? This may affect existing sessions.")
                .setPositiveButton("Delete") { _, _ ->
                    onDelete(classType)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private class ClassTypeDiffCallback : DiffUtil.ItemCallback<ClassType>() {
        override fun areItemsTheSame(oldItem: ClassType, newItem: ClassType): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ClassType, newItem: ClassType): Boolean {
            return oldItem == newItem
        }
    }
} 