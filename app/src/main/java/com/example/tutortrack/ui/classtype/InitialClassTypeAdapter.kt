package com.example.tutortrack.ui.classtype

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tutortrack.databinding.ItemEditClassTypeBinding
import com.example.tutortrack.ui.student.AddEditStudentFragment.InitialClassType

/**
 * Adapter for handling initial class types when creating or editing a student
 */
class InitialClassTypeAdapter(
    private val classTypes: List<InitialClassType>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<InitialClassTypeAdapter.ClassTypeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassTypeViewHolder {
        val binding = ItemEditClassTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClassTypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassTypeViewHolder, position: Int) {
        val classType = classTypes[position]
        holder.bind(classType)
    }

    override fun getItemCount(): Int = classTypes.size

    inner class ClassTypeViewHolder(val binding: ItemEditClassTypeBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.buttonRemove.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onRemoveClick(adapterPosition)
                }
            }
        }
        
        fun bind(classType: InitialClassType) {
            binding.editClassName.setText(classType.name)
            binding.editClassRate.setText(
                if (classType.hourlyRate > 0) classType.hourlyRate.toString() else ""
            )
        }
    }
} 