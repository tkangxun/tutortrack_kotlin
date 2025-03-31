package com.example.tutortrack.ui.student

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutortrack.R
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.DialogInvoiceBinding
import com.example.tutortrack.databinding.FragmentStudentDetailBinding
import com.example.tutortrack.ui.adapters.SessionAdapter
import com.example.tutortrack.ui.adapters.SessionWithDetails
import com.example.tutortrack.ui.classtype.ClassTypeViewModel
import com.example.tutortrack.ui.session.SessionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentDetailFragment : Fragment() {

    private var _binding: FragmentStudentDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var classTypeViewModel: ClassTypeViewModel
    private var studentId: Long = -1L
    private lateinit var sessionAdapter: SessionAdapter
    private var currentStudent: Student? = null
    private var unpaidSessions: List<SessionWithDetails> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get studentId from arguments
        arguments?.let {
            studentId = it.getLong("studentId", -1L)
        }
        
        if (studentId == -1L) {
            Toast.makeText(requireContext(), "Invalid student ID", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        
        setupViewModels()
        setupRecyclerView()
        loadStudentData()
        setupTabLayout()
        setupListeners()
        setupMenu()
    }
    
    private fun setupViewModels() {
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        classTypeViewModel = ViewModelProvider(this)[ClassTypeViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter(
            onPaymentStatusClick = { session, isPaid, paidDate ->
                // Update the session's payment status
                handlePaymentStatusChange(session, isPaid, paidDate)
            },
            onSessionDelete = { session ->
                // Delete the session
                deleteSession(session)
            }
        )
        binding.recyclerViewSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSessions.adapter = sessionAdapter
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
    
    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_student_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete_student -> {
                        showDeleteStudentConfirmation()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    
    private fun showDeleteStudentConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to delete this student? This will also delete all associated sessions and class types, and cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteStudent()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteStudent() {
        studentViewModel.getStudentById(studentId).observe(viewLifecycleOwner) { student ->
            if (student != null) {
                // First delete all sessions for this student
                sessionViewModel.getSessionsByStudentId(studentId).observe(viewLifecycleOwner) { sessions ->
                    sessions.forEach { session ->
                        sessionViewModel.deleteSession(session)
                    }
                    
                    // Then delete the student
                    studentViewModel.deleteStudent(student)
                    
                    Toast.makeText(requireContext(), "Student deleted", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }
    
    private fun loadStudentData() {
        studentViewModel.getStudentById(studentId).observe(viewLifecycleOwner) { student ->
            currentStudent = student
            binding.apply {
                textStudentName.text = student.name
                textStudentPhone.text = student.phone
                textStudentGrade.text = student.grade
                textParentName.text = if (student.parentName.isEmpty()) "Not provided" else student.parentName
                textParentContact.text = if (student.parentContact.isEmpty()) "Not provided" else student.parentContact
                textStudentNotes.text = if (student.notes.isEmpty()) "No notes available" else student.notes
                
                collapsingToolbar.title = student.name
            }
            
            loadClassTypes()
            loadStudentSessions()
        }
    }
    
    private fun loadClassTypes() {
        classTypeViewModel.getClassTypesByStudentId(studentId).observe(viewLifecycleOwner) { classTypes ->
            if (classTypes.isNotEmpty()) {
                val classTypeNames = classTypes.joinToString(", ") { it.name }
                binding.textStudentSubjects.text = classTypeNames
            } else {
                binding.textStudentSubjects.text = "No class types defined"
            }
        }
    }
    
    private fun loadStudentSessions() {
        sessionViewModel.getSessionsWithDetailsByStudentId(studentId).observe(viewLifecycleOwner) { sessionsWithDetails ->
            sessionAdapter.submitList(sessionsWithDetails)
            
            val isSessionsEmpty = sessionsWithDetails.isEmpty()
            binding.textNoSessions.visibility = if (isSessionsEmpty && binding.recyclerViewSessions.visibility == View.VISIBLE) 
                View.VISIBLE else View.GONE
                
            // Check if there are any unpaid sessions
            unpaidSessions = sessionsWithDetails.filter { !it.session.isPaid }
            updateInvoiceButtonVisibility()
        }
    }
    
    private fun updateInvoiceButtonVisibility() {
        // Only show invoice button when Sessions tab is selected and there are unpaid sessions
        val shouldShowInvoiceButton = binding.recyclerViewSessions.visibility == View.VISIBLE && 
                                      unpaidSessions.isNotEmpty()
        binding.fabInvoice.visibility = if (shouldShowInvoiceButton) View.VISIBLE else View.GONE
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Info tab
                        binding.nestedScrollInfo.visibility = View.VISIBLE
                        binding.recyclerViewSessions.visibility = View.GONE
                        binding.fabAddSession.visibility = View.GONE
                        binding.fabInvoice.visibility = View.GONE
                        binding.textNoSessions.visibility = View.GONE
                        binding.fabEditStudent.visibility = View.VISIBLE
                    }
                    1 -> { // Sessions tab
                        binding.nestedScrollInfo.visibility = View.GONE
                        binding.recyclerViewSessions.visibility = View.VISIBLE
                        binding.fabAddSession.visibility = View.VISIBLE
                        binding.fabEditStudent.visibility = View.GONE
                        
                        val isSessionsEmpty = sessionAdapter.itemCount == 0
                        binding.textNoSessions.visibility = if (isSessionsEmpty) View.VISIBLE else View.GONE
                        
                        // Update invoice button visibility when tab is selected
                        updateInvoiceButtonVisibility()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupListeners() {
        binding.apply {
            fabAddSession.setOnClickListener {
                val bundle = Bundle().apply {
                    putLong("sessionId", -1L)
                    putLong("studentId", studentId)
                    putString("title", getString(R.string.add_session))
                }
                findNavController().navigate(R.id.addEditSessionFragment, bundle)
            }
            
            fabEditStudent.setOnClickListener {
                val bundle = Bundle().apply {
                    putLong("studentId", studentId)
                    putString("title", getString(R.string.edit_student))
                }
                findNavController().navigate(R.id.addEditStudentFragment, bundle)
            }
            
            buttonManageClassTypes.setOnClickListener {
                val bundle = Bundle().apply {
                    putLong("studentId", studentId)
                }
                findNavController().navigate(R.id.classTypeListFragment, bundle)
            }
            
            fabInvoice.setOnClickListener {
                generateInvoice()
            }
        }
    }
    
    private fun generateInvoice() {
        if (unpaidSessions.isEmpty() || currentStudent == null) {
            Toast.makeText(requireContext(), R.string.no_unpaid_sessions, Toast.LENGTH_SHORT).show()
            return
        }
        
        val prefs = requireContext().getSharedPreferences("invoice_prefs", 0)
        val templateStr = prefs.getString("invoice_template", null) ?: return
        
        val sessionDetails = buildSessionDetails()
        val totalAmount = unpaidSessions.sumOf { it.session.amount }
        
        // Replace placeholders in template
        val message = templateStr
            .replace("[parentName]", currentStudent?.parentName ?: "Parent")
            .replace("[studentName]", currentStudent?.name ?: "Student")
            .replace("[[SessionDate] - [classTypeName] ([sessionDuration] hrs) [sessionIncome]]", sessionDetails)
            .replace("[Total unpaid sessions]", String.format(Locale.getDefault(), "$%.2f", totalAmount))
            
        showInvoiceDialog(message)
    }
    
    private fun buildSessionDetails(): String {
        val sb = StringBuilder()
        
        for (sessionWithDetails in unpaidSessions) {
            val session = sessionWithDetails.session
            val classTypeName = sessionWithDetails.classType?.name ?: "Unknown Class"
            val dateStr = dateFormat.format(session.date)
            val amount = String.format(Locale.getDefault(), "$%.2f", session.amount)
            val duration = String.format(Locale.getDefault(), "%.1f", session.durationMinutes / 60.0)
            
            sb.append("$dateStr - $classTypeName ($duration hrs) $amount\n")
        }
        
        return sb.toString()
    }
    
    private fun showInvoiceDialog(message: String) {
        val dialogBinding = DialogInvoiceBinding.inflate(layoutInflater)
        dialogBinding.editInvoiceMessage.setText(message)
        dialogBinding.editInvoiceMessage.setSelection(0)
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()
            
        dialogBinding.buttonCopy.setOnClickListener {
            copyToClipboard(dialogBinding.editInvoiceMessage.text.toString())
        }
        
        dialogBinding.buttonWhatsApp.setOnClickListener {
            sendToWhatsApp(dialogBinding.editInvoiceMessage.text.toString())
        }
        
        dialogBinding.buttonCloseDialog.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Invoice Message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.invoice_copied, Toast.LENGTH_SHORT).show()
    }
    
    private fun sendToWhatsApp(text: String) {
        if (currentStudent?.parentContact.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Parent contact information is missing", Toast.LENGTH_SHORT).show()
            return
        }
        
        val originalPhone = currentStudent?.parentContact ?: ""
        Toast.makeText(requireContext(), "Sending to WhatsApp: $originalPhone", Toast.LENGTH_SHORT).show()
        
        try {
            // Strip all non-numeric characters, but keep + for proper formatting
            var phoneNumber = originalPhone.replace(Regex("[^0-9+]"), "")
            
            // Make sure the phone number has the country code
            if (!phoneNumber.startsWith("+") && !phoneNumber.startsWith("00")) {
                // Remove leading zero if present
                if (phoneNumber.startsWith("0")) {
                    phoneNumber = phoneNumber.substring(1)
                }
                // Add Singapore country code
                phoneNumber = "+65$phoneNumber"
            }
            
            // If it starts with 00, replace with +
            if (phoneNumber.startsWith("00")) {
                phoneNumber = "+" + phoneNumber.substring(2)
            }
            
            // For WhatsApp URL, we need to remove the + symbol
            val whatsappPhone = phoneNumber.replace("+", "")
            
            println("Original phone: $originalPhone")
            println("Formatted phone: $phoneNumber")
            println("WhatsApp phone: $whatsappPhone")
            
            // Create the WhatsApp URL
            val url = "https://api.whatsapp.com/send?phone=$whatsappPhone&text=${Uri.encode(text)}"
            println("WhatsApp URL: $url")
            
            // Use a try-catch block for each attempt to handle specific failures
            val packageManager = requireActivity().packageManager
            
            // Try method 1: Using direct whatsapp:// URI with package
            try {
                // Check if WhatsApp is installed
                if (isPackageInstalled("com.whatsapp", packageManager)) {
                    val uri = Uri.parse("whatsapp://send?phone=$whatsappPhone&text=${Uri.encode(text)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.whatsapp")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    println("Method 1 (whatsapp:// URI) succeeded")
                    return
                }
            } catch (e: Exception) {
                println("Method 1 failed: ${e.message}")
            }
            
            // Try method 2: Using direct whatsapp:// URI with WhatsApp Business
            try {
                if (isPackageInstalled("com.whatsapp.w4b", packageManager)) {
                    val uri = Uri.parse("whatsapp://send?phone=$whatsappPhone&text=${Uri.encode(text)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.whatsapp.w4b")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    println("Method 2 (WhatsApp Business) succeeded")
                    return
                }
            } catch (e: Exception) {
                println("Method 2 failed: ${e.message}")
            }
            
            // Try method 3: Using https://api.whatsapp.com with package
            try {
                // Try using https://api.whatsapp.com with explicit package
                val uri = Uri.parse(url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                
                if (isPackageInstalled("com.whatsapp", packageManager)) {
                    intent.setPackage("com.whatsapp")
                } else if (isPackageInstalled("com.whatsapp.w4b", packageManager)) {
                    intent.setPackage("com.whatsapp.w4b")
                }
                
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                println("Method 3 (api.whatsapp.com with package) succeeded")
                return
            } catch (e: Exception) {
                println("Method 3 failed: ${e.message}")
            }
            
            // Try method 4: Generic browser intent as last resort
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK 
                startActivity(intent)
                println("Method 4 (browser fallback) succeeded")
                return
            } catch (e: Exception) {
                println("Method 4 failed: ${e.message}")
                throw e  // Re-throw to hit the outer catch block
            }
            
        } catch (e: Exception) {
            val errorMsg = "WhatsApp could not be opened. Error: ${e.message}"
            println("WhatsApp error: $errorMsg")
            e.printStackTrace()
            Toast.makeText(requireContext(), 
                           "$errorMsg\n\nPhone: $originalPhone", 
                           Toast.LENGTH_LONG).show()
        }
    }
    
    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 