package com.bignerdranch.android.autopark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bignerdranch.android.autopark.databinding.FragmentAddRouteBinding
import kotlinx.coroutines.launch

class AddRouteFragment : Fragment() {

    private var _binding: FragmentAddRouteBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: CarParkDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = CarParkDatabase.getDatabase(requireContext())

        binding.btnSave.setOnClickListener {
            saveRoute()
        }
    }

    private fun saveRoute() {
        val routeNumber = binding.etRouteNumber.text.toString().trim()
        val startPoint = binding.etStartPoint.text.toString().trim()
        val endPoint = binding.etEndPoint.text.toString().trim()
        val distanceStr = binding.etDistance.text.toString().trim()
        val estimatedTimeStr = binding.etEstimatedTime.text.toString().trim()

        if (routeNumber.isEmpty()) {
            binding.etRouteNumber.error = "Введите номер маршрута"
            binding.etRouteNumber.requestFocus()
            return
        }

        if (startPoint.isEmpty()) {
            binding.etStartPoint.error = "Введите начальную точку"
            binding.etStartPoint.requestFocus()
            return
        }

        if (endPoint.isEmpty()) {
            binding.etEndPoint.error = "Введите конечную точку"
            binding.etEndPoint.requestFocus()
            return
        }

        val distance = distanceStr.toDoubleOrNull()
        if (distance == null || distance <= 0) {
            binding.etDistance.error = "Введите корректное расстояние"
            binding.etDistance.requestFocus()
            return
        }

        val estimatedTime = estimatedTimeStr.toIntOrNull()
        if (estimatedTime == null || estimatedTime <= 0) {
            binding.etEstimatedTime.error = "Введите корректное время"
            binding.etEstimatedTime.requestFocus()
            return
        }

        lifecycleScope.launch {
            try {
                val existingRoutes = db.carparkdao().searchRoutes(routeNumber)
                if (existingRoutes.any { it.routeNumber == routeNumber }) {
                    activity?.runOnUiThread {
                        binding.etRouteNumber.error = "Маршрут с таким номером уже существует"
                        binding.etRouteNumber.requestFocus()
                    }
                    return@launch
                }
                val newRoute = Route(
                    routeNumber = routeNumber,
                    startPoint = startPoint,
                    endPoint = endPoint,
                    distance = distance,
                    estimatedTime = estimatedTime
                )

                db.carparkdao().insertRoute(newRoute)

                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Маршрут №$routeNumber успешно добавлен!",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigateUp()
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка при сохранении: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}