package com.bignerdranch.android.autopark

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var db: CarParkDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPref = getSharedPreferences("fleet_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            navigateToDashboard()
            return
        }
        setContentView(R.layout.activity_main)

        clearDatabase()
        db = CarParkDatabase.getDatabase(this)

        val etEmail = findViewById<android.widget.EditText>(R.id.etEmail)
        val etLogin = findViewById<android.widget.EditText>(R.id.etLogin)
        val etPassword = findViewById<android.widget.EditText>(R.id.etPassword)
        val rgRole = findViewById<android.widget.RadioGroup>(R.id.rgRole)
        val btnRegister = findViewById<android.widget.Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val login = etLogin.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role = when (rgRole.checkedRadioButtonId) {
                R.id.rbDispatcher -> "dispatcher"
                R.id.rbDriver -> "driver"
                else -> "passenger"
            }

            if (email.isEmpty()) {
                showToast("Введите email")
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                showToast("Введите корректный email (с @)")
                return@setOnClickListener
            }

            if (login.isEmpty()) {
                showToast("Введите логин")
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                showToast("Введите пароль")
                return@setOnClickListener
            }

            if (password.length < 3) {
                showToast("Пароль должен содержать минимум 3 символа")
                return@setOnClickListener
            }
            registerOrLoginUser(email, login, password, role)
        }
    }

    private fun clearDatabase() {
        try {
            deleteDatabase("fleet_database")
            deleteDatabase("fleet_database.db")
            deleteDatabase("fleet_database.db-journal")
        } catch (e: Exception) {
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun registerOrLoginUser(email: String, login: String, password: String, role: String) {
        lifecycleScope.launch {
            try {
                when (role) {
                    "dispatcher" -> {
                        if (login == "dispatcher" && password == "123456") {
                            val dispatcher = User(
                                name = "Диспетчер",
                                email = "dispatcher@fleet.com",
                                login = "dispatcher",
                                password = "123456",
                                role = "dispatcher"
                            )
                            saveUserData(dispatcher.name, dispatcher.email, "dispatcher")
                            runOnUiThread {
                                showToast("Вход как диспетчер")
                                navigateToDashboard()
                            }
                        } else {
                            runOnUiThread {
                                showToast("Для диспетчера: dispatcher/123456")
                            }
                        }
                    }
                    "driver" -> {
                        var driver = db.carparkdao().getUserByLoginAndRole(login, "driver")

                        if (driver == null) {
                            driver = User(
                                name = "Водитель $login",
                                email = email,
                                login = login,
                                password = password,
                                role = "driver",
                                salaryBonus = (1000..5000).random().toDouble()
                            )
                            db.carparkdao().insertUser(driver)
                            driver = db.carparkdao().getUserByLoginAndRole(login, "driver")
                            runOnUiThread {
                                showToast("Новый водитель зарегистрирован!")
                            }
                        } else {
                            if (driver.password != password) {
                                runOnUiThread {
                                    showToast("Неверный пароль для водителя $login")
                                }
                                return@launch
                            }
                            runOnUiThread {
                                showToast("Вход выполнен")
                            }
                        }

                        driver?.let {
                            saveUserData(it.name, it.email, "driver", it.userId)
                            runOnUiThread {
                                navigateToDashboard()
                            }
                        }
                    }
                    "passenger" -> {
                        var passenger = db.carparkdao().getUserByLoginAndRole(login, "passenger")

                        if (passenger == null) {
                            passenger = User(
                                name = "Пассажир $login",
                                email = email,
                                login = login,
                                password = password,
                                role = "passenger"
                            )
                            db.carparkdao().insertUser(passenger)
                            passenger = db.carparkdao().getUserByLoginAndRole(login, "passenger")
                        } else {
                            if (passenger.password != password) {
                                runOnUiThread {
                                    showToast("Неверный пароль для пассажира $login")
                                }
                                return@launch
                            }
                        }

                        passenger?.let {
                            saveUserData(it.name, it.email, "passenger", it.userId)
                            runOnUiThread {
                                showToast("Добро пожаловать, пассажир!")
                                navigateToDashboard()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showToast("Ошибка: ${e.message}")
                }
            }
        }
    }

    private fun saveUserData(name: String, email: String, role: String, userId: Int = 0) {
        sharedPref.edit().apply {
            putString("user_name", name)
            putString("user_email", email)
            putString("user_role", role)
            putBoolean("is_logged_in", true)
            putInt("user_id", userId)
            apply()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
