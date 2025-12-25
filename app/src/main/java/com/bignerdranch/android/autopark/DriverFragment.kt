package com.bignerdranch.android.autopark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bignerdranch.android.autopark.databinding.FragmentDriverBinding
import kotlinx.coroutines.launch

class DriverFragment : Fragment() {

    private var _binding: FragmentDriverBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: CarParkDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDriverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = CarParkDatabase.getDatabase(requireContext())
        loadDrivers()

        binding.btnAssignBus.setOnClickListener {
            showAssignBusDialog()
        }

        binding.btnAddDriver.setOnClickListener {
            Toast.makeText(requireContext(), "Водители добавляются автоматически при регистрации", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDrivers() {
        lifecycleScope.launch {
            try {
                val drivers = db.carparkdao().getAllDriversStatic()

                if (drivers.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.containerDrivers.visibility = View.GONE
                    binding.tvEmpty.text = "Нет зарегистрированных водителей"
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.containerDrivers.visibility = View.VISIBLE
                    binding.containerDrivers.removeAllViews()

                    drivers.forEach { driver ->
                        addDriverView(driver)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.tvEmpty.text = "Ошибка загрузки"
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun addDriverView(driver: User) {
        lifecycleScope.launch {
            try {
                val bus = db.carparkdao().getBusForDriver(driver.userId)

                val driverInfo = """
                    ${driver.name}
                    ${driver.email}
                    Логин: ${driver.login}
                    Премия: ${driver.salaryBonus} руб.
                    Автобус: ${bus?.busNumber ?: "Не назначен"}
                    ${bus?.let { "Модель: ${it.model}" } ?: ""}
                    ${if (bus != null) "Состояние: ${bus.condition}" else ""}
                    
                """.trimIndent()

                val driverView = android.widget.TextView(requireContext()).apply {
                    text = driverInfo
                    textSize = 14f
                    setPadding(32, 16, 32, 16)
                    background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame, null)
                    setOnClickListener {
                        showDriverOptions(driver, bus)
                    }
                }

                binding.containerDrivers.addView(driverView)
            } catch (e: Exception) {
            }
        }
    }

    private fun showDriverOptions(driver: User, currentBus: Bus?) {
        val options = arrayOf(
            "Закрепить/сменить автобус",
            "Удалить водителя",
            "Отмена"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Действия с водителем")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAssignBusToDriverDialog(driver)
                    1 -> deleteDriver(driver)
                }
            }
            .show()
    }

    private fun showAssignBusToDriverDialog(driver: User) {
        lifecycleScope.launch {
            try {
                val buses = db.carparkdao().getAllBusesStatic()
                val freeBuses = mutableListOf<Bus>()
                buses.forEach { bus ->
                    val driverForBus = db.carparkdao().getDriverForBus(bus.busId)
                    if (driverForBus == null) {
                        freeBuses.add(bus)
                    }
                }

                if (freeBuses.isEmpty()) {
                    Toast.makeText(requireContext(), "Нет свободных автобусов", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_assign_bus, null)
                val spinnerDriver = dialogView.findViewById<Spinner>(R.id.spinnerDriver)
                val spinnerBus = dialogView.findViewById<Spinner>(R.id.spinnerBus)
                val drivers = db.carparkdao().getAllDriversStatic()
                val driverNames = drivers.map { "${it.name} (${it.login})" }
                val busNames = freeBuses.map { "№${it.busNumber} - ${it.model}" }

                spinnerDriver.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    driverNames
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                spinnerBus.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    busNames
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                val driverIndex = drivers.indexOfFirst { it.userId == driver.userId }
                if (driverIndex != -1) {
                    spinnerDriver.setSelection(driverIndex)
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Закрепить автобус за водителем")
                    .setView(dialogView)
                    .setPositiveButton("Сохранить") { _, _ ->
                        val selectedBus = freeBuses[spinnerBus.selectedItemPosition]
                        assignBusToDriver(selectedBus.busId, driver.userId)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAssignBusDialog() {
        lifecycleScope.launch {
            try {
                val drivers = db.carparkdao().getAllDriversStatic()
                val buses = db.carparkdao().getAllBusesStatic()
                val freeBuses = mutableListOf<Bus>()
                buses.forEach { bus ->
                    val driverForBus = db.carparkdao().getDriverForBus(bus.busId)
                    if (driverForBus == null) {
                        freeBuses.add(bus)
                    }
                }

                if (drivers.isEmpty()) {
                    Toast.makeText(requireContext(), "Нет зарегистрированных водителей", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (freeBuses.isEmpty()) {
                    Toast.makeText(requireContext(), "Нет свободных автобусов", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_assign_bus, null)
                val spinnerDriver = dialogView.findViewById<Spinner>(R.id.spinnerDriver)
                val spinnerBus = dialogView.findViewById<Spinner>(R.id.spinnerBus)
                val driverNames = drivers.map { "${it.name} (${it.login})" }
                val busNames = freeBuses.map { "№${it.busNumber} - ${it.model}" }

                spinnerDriver.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    driverNames
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                spinnerBus.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    busNames
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Закрепить автобус за водителем")
                    .setView(dialogView)
                    .setPositiveButton("Сохранить") { _, _ ->
                        val selectedDriver = drivers[spinnerDriver.selectedItemPosition]
                        val selectedBus = freeBuses[spinnerBus.selectedItemPosition]
                        assignBusToDriver(selectedBus.busId, selectedDriver.userId)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun assignBusToDriver(busId: Int, driverId: Int) {
        lifecycleScope.launch {
            try {
                val currentDriver = db.carparkdao().getDriverForBus(busId)
                currentDriver?.let {
                    db.carparkdao().removeBusFromDriver(BusDriverCrossRef(busId = busId, driverId = it.userId))
                }
                val crossRef = BusDriverCrossRef(busId = busId, driverId = driverId)
                db.carparkdao().assignBusToDriver(crossRef)

                Toast.makeText(requireContext(), "Автобус успешно закреплен", Toast.LENGTH_SHORT).show()
                loadDrivers()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteDriver(driver: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление водителя")
            .setMessage("Вы уверены, что хотите удалить водителя ${driver.name}?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val bus = db.carparkdao().getBusForDriver(driver.userId)
                        bus?.let {
                            db.carparkdao().removeBusFromDriver(BusDriverCrossRef(busId = it.busId, driverId = driver.userId))
                        }
                        db.carparkdao().deleteUser(driver)

                        Toast.makeText(requireContext(), "Водитель удален", Toast.LENGTH_SHORT).show()
                        loadDrivers()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}