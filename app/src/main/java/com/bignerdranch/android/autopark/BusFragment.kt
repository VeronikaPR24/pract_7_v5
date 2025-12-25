package com.bignerdranch.android.autopark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.android.autopark.databinding.FragmentBusBinding
import kotlinx.coroutines.launch

class BusFragment : Fragment() {

    private var _binding: FragmentBusBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadBuses()

        binding.fabAddBus.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("fleet_prefs", 0)
            val userRole = sharedPref.getString("user_role", "passenger") ?: "passenger"

            if (userRole == "dispatcher") {
                findNavController().navigate(R.id.action_busFragment_to_addBusFragment)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Только диспетчер может добавлять автобусы",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadBuses() {
        lifecycleScope.launch {
            try {
                val db = CarParkDatabase.getDatabase(requireContext())
                val buses = db.carparkdao().getAllBusesStatic()

                if (buses.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.containerBuses.visibility = View.GONE
                    binding.tvEmpty.text = "Нет автобусов в парке"
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.containerBuses.visibility = View.VISIBLE
                    binding.containerBuses.removeAllViews()
                    buses.forEach { bus ->
                        addBusView(bus)
                    }
                    addStatistics(buses)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.tvEmpty.text = "Ошибка загрузки"
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun addBusView(bus: Bus) {
        val busView = TextView(requireContext()).apply {
            text = """
                Автобус №${bus.busNumber}
                Модель: ${bus.model}
                Рег. номер: ${bus.registrationNumber}
                Состояние: ${bus.condition}
                Пробег: ${bus.mileage} км
                Амортизация: ${bus.depreciation}%
                Текущая стоимость: ${String.format("%.2f", bus.currentValue)} руб.
            """.trimIndent()
            textSize = 16f
            setPadding(32, 16, 32, 16)
            background = resources.getDrawable(android.R.drawable.dialog_holo_light_frame, null)
            setOnClickListener {
                Toast.makeText(context, "Автобус №${bus.busNumber}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.containerBuses.addView(busView)
    }

    private fun addStatistics(buses: List<Bus>) {
        val excellent = buses.count { it.condition == "excellent" }
        val good = buses.count { it.condition == "good" }
        val average = buses.count { it.condition == "average" }
        val poor = buses.count { it.condition == "poor" }
        val totalValue = buses.sumOf { it.currentValue }

        val statsView = TextView(requireContext()).apply {
            text = """
                Статистика автопарка
                
                Всего автобусов: ${buses.size}
                Отличное: $excellent
                Хорошее: $good
                Среднее: $average
                Плохое: $poor
                
                Общая стоимость: ${String.format("%.2f", totalValue)} руб.
            """.trimIndent()
            textSize = 14f
            setPadding(32, 32, 32, 32)
            background = resources.getDrawable(android.R.drawable.dialog_holo_dark_frame, null)
            setTextColor(resources.getColor(android.R.color.white, null))
        }

        binding.containerBuses.addView(statsView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}