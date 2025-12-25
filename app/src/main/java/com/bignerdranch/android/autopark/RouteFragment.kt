package com.bignerdranch.android.autopark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.android.autopark.databinding.FragmentRouteBinding
import kotlinx.coroutines.launch
class RouteFragment : Fragment() {

    private var _binding: FragmentRouteBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RouteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        loadRoutes()
    }

    private fun setupRecyclerView() {
        adapter = RouteAdapter()
        adapter.onItemClick = { route ->
            showRouteDetails(route)
        }

        adapter.onItemLongClick = { route ->
            showDeleteDialog(route)
        }

        binding.rvRoutes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoutes.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchRoutes(query)
            } else {
                Toast.makeText(requireContext(), "Введите номер маршрута", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabAddRoute.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("fleet_prefs", 0)
            val userRole = sharedPref.getString("user_role", "passenger") ?: "passenger"

            if (userRole == "dispatcher") {
                findNavController().navigate(R.id.action_routeFragment_to_addRouteFragment)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Только диспетчер может добавлять маршруты",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadRoutes() {
        lifecycleScope.launch {
            try {
                val db = CarParkDatabase.getDatabase(requireContext())
                val routes = db.carparkdao().getAllRoutesStatic()
                updateUI(routes, "Нет маршрутов")
            } catch (e: Exception) {
                showError("Ошибка загрузки: ${e.message}")
            }
        }
    }

    private fun searchRoutes(query: String) {
        lifecycleScope.launch {
            try {
                val db = CarParkDatabase.getDatabase(requireContext())
                val routes = db.carparkdao().searchRoutes(query)
                updateUI(routes, "Маршруты не найдены")
            } catch (e: Exception) {
                showError("Ошибка поиска: ${e.message}")
            }
        }
    }

    private fun updateUI(routes: List<Route>, emptyMessage: String) {
        if (routes.isEmpty()) {
            binding.tvEmpty.text = emptyMessage
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvRoutes.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvRoutes.visibility = View.VISIBLE
            adapter.submitList(routes)
        }
    }

    private fun showRouteDetails(route: Route) {
        lifecycleScope.launch {
            try {
                val db = CarParkDatabase.getDatabase(requireContext())
                val drivers = db.carparkdao().getDriversForRoute(route.routeId)

                val details = """
                    Маршрут №${route.routeNumber}
                    Начало: ${route.startPoint}
                    Конец: ${route.endPoint}
                    Расстояние: ${route.distance} км
                    Время: ${route.estimatedTime} мин
                    Водителей: ${drivers.size}
                """.trimIndent()

                activity?.runOnUiThread {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Информация о маршруте")
                        .setMessage(details)
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Информация о маршруте")
                        .setMessage("Маршрут №${route.routeNumber}\n${route.startPoint} → ${route.endPoint}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun showDeleteDialog(route: Route) {
        val sharedPref = requireActivity().getSharedPreferences("fleet_prefs", 0)
        val userRole = sharedPref.getString("user_role", "passenger") ?: "passenger"

        if (userRole != "dispatcher") {
            Toast.makeText(requireContext(), "Только диспетчер может удалять маршруты", Toast.LENGTH_SHORT).show()
            return
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Удалить маршрут?")
            .setMessage("Вы уверены, что хотите удалить маршрут №${route.routeNumber}?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val db = CarParkDatabase.getDatabase(requireContext())
                        db.carparkdao().deleteRoute(route)
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Маршрут удален", Toast.LENGTH_SHORT).show()
                            if (binding.etSearch.text.isNotEmpty()) {
                                searchRoutes(binding.etSearch.text.toString())
                            } else {
                                loadRoutes()
                            }
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        binding.tvEmpty.text = "Ошибка"
        binding.tvEmpty.visibility = View.VISIBLE
        binding.rvRoutes.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}