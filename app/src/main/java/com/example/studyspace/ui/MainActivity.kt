package com.example.studyspace.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.studyspace.R
import com.example.studyspace.databinding.ActivityMainBinding

/**
 * MainActivity — Main Application Shell
 *
 * Tutorial Reference: T02 — Layout (Bottom Navigation, Fragment hosting)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // T02 — Inflate layout with View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Navigation Component
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Link Bottom Navigation with NavController
        binding.bottomNavigation.setupWithNavController(navController)
    }
}
