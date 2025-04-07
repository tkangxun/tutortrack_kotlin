package com.example.tutortrack.ui.student

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutortrack.R
import com.example.tutortrack.data.model.Session
import com.example.tutortrack.data.model.Student
import com.example.tutortrack.databinding.DialogInvoiceBinding
import com.example.tutortrack.databinding.FragmentStudentDetailBinding
import com.example.tutortrack.databinding.ItemSessionBinding
import com.example.tutortrack.ui.adapters.SessionAdapter
import com.example.tutortrack.ui.adapters.SessionWithDetails
import com.example.tutortrack.ui.classtype.ClassTypeViewModel
import com.example.tutortrack.ui.session.SessionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
            },
            onSessionEdit = { session ->
                // Navigate to edit session
                editSession(session)
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
    
    private fun editSession(session: Session) {
        val bundle = Bundle().apply {
            putLong("sessionId", session.id)
            putLong("studentId", studentId)
            putString("title", getString(R.string.edit_session))
        }
        findNavController().navigate(R.id.addEditSessionFragment, bundle)
    }
    
    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_student_detail, menu)
                
                // Update the menu based on student's archived status
                currentStudent?.let { student ->
                    val archiveMenuItem = menu.findItem(R.id.action_archive_student)
                    if (student.isArchived) {
                        archiveMenuItem.setTitle(R.string.unarchive_student)
                    } else {
                        archiveMenuItem.setTitle(R.string.archive_student)
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete_student -> {
                        showDeleteStudentConfirmation()
                        true
                    }
                    R.id.action_archive_student -> {
                        currentStudent?.let { student ->
                            if (student.isArchived) {
                                showUnarchiveStudentConfirmation()
                            } else {
                                showArchiveStudentConfirmation()
                            }
                        }
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
                        binding.fabDateSearch.visibility = View.GONE
                        binding.textNoSessions.visibility = View.GONE
                        binding.fabEditStudent.visibility = View.VISIBLE
                    }
                    1 -> { // Sessions tab
                        binding.nestedScrollInfo.visibility = View.GONE
                        binding.recyclerViewSessions.visibility = View.VISIBLE
                        binding.fabAddSession.visibility = View.VISIBLE
                        binding.fabDateSearch.visibility = View.VISIBLE
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
            
            fabDateSearch.setOnClickListener {
                showDatePickerForSearch()
            }
        }
    }
    
    private fun showInvoiceDialog(message: String) {
        try {
            // Log the start of dialog creation
            Log.d("StudentDetailFragment", "Creating invoice dialog with message length: ${message.length}")
            
            // Create dialog with a more standard approach
            val dialogBinding = DialogInvoiceBinding.inflate(layoutInflater)
            dialogBinding.editInvoiceMessage.setText(message)
            dialogBinding.editInvoiceMessage.setSelection(0)
            
            // Use a more compatible dialog creation approach
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogBinding.root)
                .create()
                
            // Set up button listeners
            dialogBinding.buttonCopy.setOnClickListener {
                copyToClipboard(dialogBinding.editInvoiceMessage.text.toString())
            }
            
            dialogBinding.buttonWhatsApp.setOnClickListener {
                sendToWhatsApp(dialogBinding.editInvoiceMessage.text.toString())
            }
            
            dialogBinding.buttonCloseDialog.setOnClickListener {
                dialog.dismiss()
            }
            
            // Show dialog with error handling
            try {
                dialog.show()
                Log.d("StudentDetailFragment", "Invoice dialog shown successfully")
            } catch (e: Exception) {
                Log.e("StudentDetailFragment", "Error showing dialog: ${e.message}", e)
                // Fallback to a simpler dialog if the custom one fails
                showFallbackInvoiceDialog(message)
            }
        } catch (e: Exception) {
            Log.e("StudentDetailFragment", "Error creating invoice dialog: ${e.message}", e)
            // Show a simple toast with the invoice text as fallback
            Toast.makeText(requireContext(), "Error showing invoice dialog. Invoice text: $message", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showFallbackInvoiceDialog(message: String) {
        try {
            // Create a simple dialog as fallback
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Invoice")
                .setMessage(message)
                .setPositiveButton("Copy") { _, _ -> copyToClipboard(message) }
                .setNegativeButton("Close", null)
                .show()
        } catch (e: Exception) {
            Log.e("StudentDetailFragment", "Error showing fallback dialog: ${e.message}", e)
            Toast.makeText(requireContext(), "Error showing invoice dialog", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun generateInvoice() {
        try {
            Log.d("StudentDetailFragment", "Generating invoice. Unpaid sessions: ${unpaidSessions.size}, Current student: ${currentStudent?.name}")
            
            if (unpaidSessions.isEmpty() || currentStudent == null) {
                Toast.makeText(requireContext(), R.string.no_unpaid_sessions, Toast.LENGTH_SHORT).show()
                return
            }
            
            val prefs = requireContext().getSharedPreferences("invoice_prefs", 0)
            var templateStr = prefs.getString("invoice_template", null)
            
            // If no template exists, use default template and save it
            if (templateStr == null) {
                Log.d("StudentDetailFragment", "No invoice template found, using default template")
                templateStr = getDefaultInvoiceTemplate()
                
                // Save the default template for future use
                prefs.edit().putString("invoice_template", templateStr).apply()
                Log.d("StudentDetailFragment", "Default invoice template saved")
            }
            
            val sessionDetails = buildSessionDetails()
            val totalAmount = unpaidSessions.sumOf { it.session.amount }
            
            // Replace placeholders in template
            val message = templateStr
                .replace("[parentName]", currentStudent?.parentName ?: "Parent")
                .replace("[studentName]", currentStudent?.name ?: "Student")
                .replace("[[SessionDate] - [classTypeName] ([sessionDuration] hrs) [sessionIncome]]", sessionDetails)
                .replace("[Total unpaid sessions]", String.format(Locale.getDefault(), "$%.2f", totalAmount))
                
            Log.d("StudentDetailFragment", "Invoice message generated, length: ${message.length}")
            showInvoiceDialog(message)
        } catch (e: Exception) {
            Log.e("StudentDetailFragment", "Error generating invoice: ${e.message}", e)
            Toast.makeText(requireContext(), "Error generating invoice: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun getDefaultInvoiceTemplate(): String {
        return """
            Dear [parentName],
            
            This is an invoice for tutoring sessions for [studentName].
            
            Session Details:
            [[SessionDate] - [classTypeName] ([sessionDuration] hrs) [sessionIncome]]
            
            Total Amount Due: [Total unpaid sessions]
            
            Please make payment at your earliest convenience.
            
            Thank you for your business!
        """.trimIndent()
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

    private fun showDatePickerForSearch() {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                val selectedDate = calendar.time
                findAndHighlightSessionByDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun findAndHighlightSessionByDate(targetDate: Date) {
        // Get all sessions for the current student
        val currentSessions = sessionAdapter.currentList
        if (currentSessions.isEmpty()) {
            Toast.makeText(requireContext(), "No sessions found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Find session closest to target date
        val closestSession = currentSessions.minByOrNull { 
            abs(it.session.date.time - targetDate.time)
        }
        
        if (closestSession == null) {
            Toast.makeText(requireContext(), "No matching sessions found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Find position in the RecyclerView
        val position = currentSessions.indexOf(closestSession)
        if (position == -1) return
        
        // Show a toast with the found date
        val dateFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        Toast.makeText(
            requireContext(),
            "Found: ${dateFormatter.format(closestSession.session.date)}",
            Toast.LENGTH_SHORT
        ).show()
        
        // Make sure the toolbar is expanded first to ensure consistent positioning
        binding.appBarLayout.setExpanded(true)
        
        // Wait for the toolbar animation to complete before scrolling
        binding.appBarLayout.postDelayed({
            val layoutManager = binding.recyclerViewSessions.layoutManager as LinearLayoutManager
            
            // Get the top padding
            val topPadding = resources.getDimensionPixelSize(R.dimen.recycler_view_top_padding)
            
            // Check if this is the last or near-last item
            if (position >= currentSessions.size - 3) {
                // Special handling for last few items
                
                // First scroll to the end to make sure all content is loaded and measured
                binding.recyclerViewSessions.scrollToPosition(currentSessions.size - 1)
                
                // Then post a delayed action to ensure end has been reached
                binding.recyclerViewSessions.postDelayed({
                    // Finally scroll to our target position with appropriate offset
                    layoutManager.scrollToPositionWithOffset(position, topPadding)
                    
                    // Highlight the session
                    binding.recyclerViewSessions.postDelayed({
                        highlightSessionCard(position)
                    }, 150)
                }, 200)
            } else {
                // Normal case - non-last items
                // Two-phase approach for reliability
                binding.recyclerViewSessions.scrollToPosition(position)
                
                binding.recyclerViewSessions.post {
                    layoutManager.scrollToPositionWithOffset(position, topPadding)
                    
                    binding.recyclerViewSessions.postDelayed({
                        highlightSessionCard(position)
                    }, 150)
                }
            }
        }, 100) // Short delay for toolbar animation
    }
    
    private fun highlightSessionCard(position: Int) {
        val viewHolder = binding.recyclerViewSessions.findViewHolderForAdapterPosition(position)
        
        if (viewHolder != null) {
            val itemView = viewHolder.itemView
            
            // Save original state
            val originalBackground = itemView.background
            
            // Apply highlight color
            itemView.setBackgroundColor(resources.getColor(R.color.highlight_overlay, null))
            
            // Schedule removal of highlight after delay
            itemView.postDelayed({
                // Animate back to original
                itemView.animate()
                    .setDuration(300)
                    .withEndAction {
                        itemView.background = originalBackground
                    }
                    .start()
            }, 500) // Highlight for 0.5 seconds
        } else {
            // If ViewHolder not found, try again after a short delay
            binding.recyclerViewSessions.postDelayed({
                highlightSessionCard(position)
            }, 100)
        }
    }

    private fun showArchiveStudentConfirmation() {
        currentStudent?.let { student ->
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.archive_student))
                .setMessage(getString(R.string.archive_confirmation, student.name))
                .setPositiveButton("Archive") { _, _ ->
                    archiveStudent()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showUnarchiveStudentConfirmation() {
        currentStudent?.let { student ->
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.unarchive_student))
                .setMessage(getString(R.string.unarchive_confirmation, student.name))
                .setPositiveButton("Unarchive") { _, _ ->
                    unarchiveStudent()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun archiveStudent() {
        currentStudent?.let { student ->
            studentViewModel.archiveStudent(student)
            Toast.makeText(requireContext(), "${student.name} archived", Toast.LENGTH_SHORT).show()
            // Return to student list after archiving
            findNavController().navigateUp()
        }
    }

    private fun unarchiveStudent() {
        currentStudent?.let { student ->
            studentViewModel.unarchiveStudent(student)
            Toast.makeText(requireContext(), "${student.name} unarchived", Toast.LENGTH_SHORT).show()
            // Stay on the detail page after unarchiving, refresh the options menu
            requireActivity().invalidateOptionsMenu()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 