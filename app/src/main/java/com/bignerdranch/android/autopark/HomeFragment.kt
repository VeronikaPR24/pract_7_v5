package com.bignerdranch.android.autopark

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import com.bignerdranch.android.autopark.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPref: SharedPreferences
    private lateinit var db: CarParkDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPref = requireActivity().getSharedPreferences("fleet_prefs", 0)
        db = CarParkDatabase.getDatabase(requireContext())

        setupUI()
        loadUserSpecificData()

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun setupUI() {
        val userName = sharedPref.getString("user_name", "Пользователь") ?: "Пользователь"
        val userEmail = sharedPref.getString("user_email", "") ?: ""
        val userRole = sharedPref.getString("user_role", "passenger") ?: "passenger"

        binding.tvWelcome.text = "Добро пожаловать, $userName!"
        binding.tvEmail.text = "Email: $userEmail"
        binding.tvRole.text = when (userRole) {
            "dispatcher" -> "Роль: Диспетчер"
            "driver" -> "Роль: Водитель"
            else -> "Роль: Пассажир"
        }

        configureButtonsByRole(userRole)
        binding.btnSearchRoutes.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_routeFragment)
        }

        binding.btnViewAllRoutes.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_routeFragment)
        }

        binding.btnQuickBus.setOnClickListener {
            loadBusesByCondition()
        }

        binding.btnQuickDriver.setOnClickListener {
            loadDriversByRoute()
        }
    }

    private fun configureButtonsByRole(role: String) {
        val isDispatcher = role == "dispatcher"
        binding.btnQuickBus.visibility = if (isDispatcher) View.VISIBLE else View.GONE
        binding.btnQuickDriver.visibility = if (isDispatcher) View.VISIBLE else View.GONE
        binding.cardAdditional.visibility = if (isDispatcher) View.VISIBLE else View.GONE
    }

    private fun loadBusesByCondition() {
        lifecycleScope.launch {
            try {
                val conditions = listOf("excellent", "good", "average", "poor")
                val result = StringBuilder("Автобусы по состоянию\n\n")

                var totalBuses = 0
                conditions.forEach { condition ->
                    val buses = db.carparkdao().getBusesByCondition(condition)
                    totalBuses += buses.size
                    val conditionName = when (condition) {
                        "excellent" -> "Отличное"
                        "good" -> "Хорошее"
                        "average" -> "Среднее"
                        else -> "Плохое"
                    }
                    result.append("$conditionName: ${buses.size} автобусов\n")
                    if (buses.isNotEmpty()) {
                        buses.take(3).forEach { bus ->
                            result.append("   №${bus.busNumber} - ${bus.model}\n")
                        }
                        if (buses.size > 3) {
                            result.append("   ... и еще ${buses.size - 3}\n")
                        }
                        result.append("\n")
                    }
                }

                result.append("Всего автобусов в парке: $totalBuses")

                showInfoDialog("Состояние автопарка", result.toString())
            } catch (e: Exception) {
                showToast("Ошибка загрузки: ${e.message}")
            }
        }
    }

    private fun loadDriversByRoute() {
        lifecycleScope.launch {
            try {
                val routes = db.carparkdao().getAllRoutesStatic()
                val result = StringBuilder("Водители по маршрутам\n\n")

                if (routes.isEmpty()) {
                    result.append("Нет доступных маршрутов")
                } else {
                    routes.forEach { route ->
                        val drivers = db.carparkdao().getDriversForRoute(route.routeId)
                        result.append("Маршрут ${route.routeNumber}\n")
                        result.append("${route.startPoint} → ${route.endPoint}\n")
                        result.append("Расстояние: ${route.distance} км\n")
                        result.append("Время: ${route.estimatedTime} мин\n")

                        if (drivers.isEmpty()) {
                            result.append("Водители: нет назначенных\n")
                        } else {
                            result.append("Водители (${drivers.size}):\n")
                            drivers.forEach { driver ->
                                val bus = db.carparkdao().getBusForDriver(driver.userId)
                                result.append("   • ${driver.name}")
                                bus?.let {
                                    result.append(" (автобус №${it.busNumber})")
                                }
                                result.append("\n")
                            }
                        }
                        result.append("\n")
                    }
                }

                showInfoDialog("Водители по маршрутам", result.toString())
            } catch (e: Exception) {
                showToast("Ошибка загрузки: ${e.message}")
            }
        }
    }

    private fun loadUserSpecificData() {
        lifecycleScope.launch {
            try {
                val userRole = sharedPref.getString("user_role", "passenger") ?: "passenger"
                binding.tvDispatcherInfo.visibility = View.GONE
                binding.tvDriverInfo.visibility = View.GONE
                binding.tvPassengerInfo.visibility = View.GONE
                binding.tvAdditionalInfo.visibility = View.GONE

                when (userRole) {
                    "driver" -> {
                        val login = sharedPref.getString("user_email", "")?.split("@")?.firstOrNull() ?: ""
                        val user = db.carparkdao().getUserByLogin(login)

                        user?.let {
                            val driverInfo = StringBuilder("👤 МОИ ДАННЫЕ\n\n")
                            driverInfo.append("Имя: ${it.name}\n")
                            driverInfo.append("Email: ${it.email}\n")
                            driverInfo.append("Премия: ${it.salaryBonus} руб.\n")

                            try {
                                val bus = db.carparkdao().getBusForDriver(it.userId)
                                bus?.let { bus ->
                                    driverInfo.append("\nЗакрепленные автобусы\n")
                                    driverInfo.append("Номер: ${bus.busNumber}\n")
                                    driverInfo.append("Модель: ${bus.model}\n")
                                    driverInfo.append("Рег. номер: ${bus.registrationNumber}\n")
                                    driverInfo.append("Состояние: ${bus.condition}\n")
                                    driverInfo.append("Пробег: ${bus.mileage} км\n")
                                    driverInfo.append("Амортизация: ${String.format("%.1f", bus.depreciation)}%\n")
                                }

                                val routes = db.carparkdao().getRoutesForDriver(it.userId)
                                if (routes.isNotEmpty()) {
                                    driverInfo.append("\nМаршруты\n")
                                    routes.forEach { route ->
                                        driverInfo.append("• ${route.routeNumber}: ${route.startPoint} → ${route.endPoint}\n")
                                        driverInfo.append("  ${route.distance} км, ${route.estimatedTime} мин\n")
                                    }
                                } else {
                                    driverInfo.append("\nУ вас пока нет назначенных маршрутов\n")
                                }
                            } catch (e: Exception) {
                                driverInfo.append("\nДанные о транспорте временно недоступны\n")
                            }

                            binding.tvDriverInfo.text = driverInfo.toString()
                            binding.tvDriverInfo.visibility = View.VISIBLE
                        }
                    }
                    "dispatcher" -> {
                        try {
                            val totalBuses = db.carparkdao().getAllBusesStatic().size
                            val totalDrivers = db.carparkdao().getAllDriversStatic().size
                            val totalRoutes = db.carparkdao().getAllRoutesStatic().size

                            val stats = """
                                Статистика
                                Автобусов: $totalBuses
                                Водителей: $totalDrivers
                                Маршрутов: $totalRoutes
                            """.trimIndent()

                            binding.tvDispatcherInfo.text = stats
                            binding.tvDispatcherInfo.visibility = View.VISIBLE

                            loadDispatcherAdditionalInfo()
                        } catch (e: Exception) {
                            binding.tvDispatcherInfo.text = "Статистика временно недоступна"
                            binding.tvDispatcherInfo.visibility = View.VISIBLE
                        }
                    }
                    "passenger" -> {
                        binding.tvPassengerInfo.text = """
                            ДОБРО ПОЖАЛОВАТЬ В АВТОПАРК!
                            
                            ВЫ МОЖЕТЕ:
                            • Просматривать все маршруты
                            • Искать нужные направления
                            • Планировать свои поездки
                            
                            Нажмите "Искать маршруты" или "Все маршруты"
                            для просмотра доступных направлений.
                        """.trimIndent()
                        binding.tvPassengerInfo.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun loadDispatcherAdditionalInfo() {
        lifecycleScope.launch {
            try {
                val buses = db.carparkdao().getAllBusesStatic()
                var excellent = 0
                var good = 0
                var average = 0
                var poor = 0

                buses.forEach { bus ->
                    when (bus.condition) {
                        "excellent" -> excellent++
                        "good" -> good++
                        "average" -> average++
                        "poor" -> poor++
                    }
                }

                val additionalInfo = """
                    Состояния автопарка:
                    Отличное: $excellent
                    Хорошее: $good
                    Среднее: $average
                    Плохое: $poor
                    Рекомендации:
                    ${if (poor > 0) "• Требуется ремонт $poor автобусов\n" else ""}
                    ${if (average > 3) "• Плановый осмотр $average автобусов\n" else ""}
                    ${if (excellent + good > buses.size * 0.7) "• Парк в хорошем состоянии" else "• Требуется обновление парка"}
                """.trimIndent()

                binding.tvAdditionalInfo.text = additionalInfo
                binding.tvAdditionalInfo.visibility = View.VISIBLE
                binding.cardAdditional.visibility = View.VISIBLE
            } catch (e: Exception) {
            }
        }
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход из системы")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                logoutUser()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun logoutUser() {
        sharedPref.edit().clear().apply()
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().finish()

        Toast.makeText(requireContext(), "Вы вышли из системы", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}