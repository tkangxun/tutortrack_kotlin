package com.example.tutortrack

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tutortrack.databinding.ActivityMainBinding
import androidx.navigation.NavOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Keep track of the current top-level destination
    private var currentTopLevelDestinationId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        // Define top-level destinations
        val topLevelDestinations = setOf(
            R.id.navigation_home, 
            R.id.navigation_students, 
            R.id.navigation_sessions, 
            R.id.navigation_reports
        )
        
        val appBarConfiguration = AppBarConfiguration(topLevelDestinations)
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Custom navigation behavior for bottom nav
        navView.setOnItemSelectedListener { menuItem ->
            val itemId = menuItem.itemId
            
            if (topLevelDestinations.contains(itemId)) {
                // Check if we're currently in the same section but in a detail view
                val currentDestination = navController.currentDestination?.id
                
                if (currentDestination != itemId) {
                    // Either we're in a different section or in a detail view of the same section
                    // In both cases, navigate to the main fragment of the selected section
                    
                    // First pop back to start destination if needed
                    val builder = NavOptions.Builder()
                        .setPopUpTo(navController.graph.startDestinationId, false)
                    
                    // Then navigate to the selected destination
                    navController.navigate(itemId, null, builder.build())
                }
                return@setOnItemSelectedListener true
            }
            false
        }
        
        // Update bottom nav selection when navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // If we navigated to a top-level destination, update the bottom nav
            if (topLevelDestinations.contains(destination.id)) {
                navView.menu.findItem(destination.id)?.isChecked = true
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}