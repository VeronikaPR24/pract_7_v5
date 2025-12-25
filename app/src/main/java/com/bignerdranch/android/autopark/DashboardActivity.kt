package com.bignerdranch.android.autopark

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bignerdranch.android.autopark.databinding.ActivityDashboardBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var navController: NavController
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences("fleet_prefs", MODE_PRIVATE)

        if (!sharedPref.getBoolean("is_logged_in", false)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val userName = sharedPref.getString("user_name", "Пользователь")
        val userRole = sharedPref.getString("user_role", "passenger")

        Toast.makeText(this, "Добро пожаловать, $userName!", Toast.LENGTH_SHORT).show()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        setupMenuForRole(userRole)
    }

    private fun setupMenuForRole(role: String?) {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val menu = bottomNavigationView.menu
        menu.findItem(R.id.busFragment).isVisible = false
        menu.findItem(R.id.routeFragment).isVisible = false
        menu.findItem(R.id.driverFragment).isVisible = false

        when (role) {
            "dispatcher" -> {
                menu.findItem(R.id.busFragment).isVisible = true
                menu.findItem(R.id.routeFragment).isVisible = true
                menu.findItem(R.id.driverFragment).isVisible = true
                menu.findItem(R.id.driverFragment).title = "Водители"
            }
            "driver" -> {
                menu.findItem(R.id.routeFragment).isVisible = true
                menu.findItem(R.id.driverFragment).isVisible = true
                menu.findItem(R.id.driverFragment).title = "Мои данные"
            }
            "passenger" -> {
                menu.findItem(R.id.routeFragment).isVisible = true
            }
        }
    }
}