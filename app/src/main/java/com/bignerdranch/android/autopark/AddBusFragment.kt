package com.bignerdranch.android.autopark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bignerdranch.android.autopark.databinding.FragmentAddBusBinding
import kotlinx.coroutines.launch

class AddBusFragment : Fragment() {

    private var _binding: FragmentAddBusBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: CarParkDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = CarParkDatabase.getDatabase(requireContext())
        setupConditionSpinner()

        binding.btnSave.setOnClickListener {
            saveBus()
        }
    }

    private fun setupConditionSpinner() {
        binding.spinnerCondition.visibility = View.VISIBLE
        val conditions = arrayOf(
            "Отличное",
            "Хорошее",
            "Среднее",
            "Плохое"
        )
        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            conditions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_list_item_1)
        }
        binding.spinnerCondition.adapter = adapter
        binding.spinnerCondition.setSelection(0)
    }

    private fun saveBus() {
        val busNumberStr = binding.etBusNumber.text.toString().trim()
        val model = binding.etModel.text.toString().trim()
        val purchaseDate = binding.etPurchaseDate.text.toString().trim()
        val initialPriceStr = binding.etInitialPrice.text.toString().trim()
        val mileageStr = binding.etMileage.text.toString().trim()
        val selectedCondition = binding.spinnerCondition.selectedItem.toString()
        val condition = when (selectedCondition) {
            "Отличное" -> "excellent"
            "Хорошее" -> "good"
            "Среднее" -> "average"
            "Плохое" -> "poor"
            else -> "good"
        }

        if (busNumberStr.isEmpty()) {
            Toast.makeText(requireContext(), "Введите номер автобуса", Toast.LENGTH_SHORT).show()
            binding.etBusNumber.requestFocus()
            return
        }

        val busNumber = busNumberStr.toIntOrNull()
        if (busNumber == null || busNumber <= 0) {
            Toast.makeText(requireContext(), "Введите корректный номер автобуса", Toast.LENGTH_SHORT).show()
            binding.etBusNumber.requestFocus()
            return
        }

        if (model.isEmpty()) {
            Toast.makeText(requireContext(), "Введите модель автобуса", Toast.LENGTH_SHORT).show()
            binding.etModel.requestFocus()
            return
        }

        if (purchaseDate.isEmpty()) {
            Toast.makeText(requireContext(), "Введите дату покупки", Toast.LENGTH_SHORT).show()
            binding.etPurchaseDate.requestFocus()
            return
        }

        if (!purchaseDate.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))) {
            Toast.makeText(requireContext(), "Введите дату в формате дд.мм.гггг", Toast.LENGTH_SHORT).show()
            binding.etPurchaseDate.requestFocus()
            return
        }

        val initialPrice = initialPriceStr.toDoubleOrNull()
        if (initialPrice == null || initialPrice <= 0) {
            Toast.makeText(requireContext(), "Введите корректную начальную стоимость", Toast.LENGTH_SHORT).show()
            binding.etInitialPrice.requestFocus()
            return
        }

        val mileage = mileageStr.toIntOrNull()
        if (mileage == null || mileage < 0) {
            Toast.makeText(requireContext(), "Введите корректный пробег", Toast.LENGTH_SHORT).show()
            binding.etMileage.requestFocus()
            return
        }

        lifecycleScope.launch {
            try {
                val existingBus = db.carparkdao().getBusByNumber(busNumber)
                if (existingBus != null) {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Автобус с номером $busNumber уже существует",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.etBusNumber.requestFocus()
                    }
                    return@launch
                }

                val registrationNumber = "А${String.format("%03d", busNumber)}АА77"
                val purchaseYear = purchaseDate.substring(6).toIntOrNull() ?: 2023
                val currentYear = java.time.LocalDate.now().year
                val yearsInUse = (currentYear - purchaseYear).coerceAtLeast(1)
                val depreciation = (yearsInUse * 2.0).coerceAtMost(40.0)
                val currentValue = initialPrice * (1 - depreciation / 100)

                val newBus = Bus(
                    busNumber = busNumber,
                    model = model,
                    registrationNumber = registrationNumber,
                    purchaseDate = purchaseDate,
                    initialPrice = initialPrice,
                    currentValue = currentValue,
                    depreciation = depreciation,
                    condition = condition,
                    mileage = mileage
                )

                db.carparkdao().insertBus(newBus)

                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Автобус №$busNumber успешно добавлен!",
                        Toast.LENGTH_SHORT
                    ).show()

                    clearForm()

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

    private fun clearForm() {
        binding.etBusNumber.text.clear()
        binding.etModel.text.clear()
        binding.etPurchaseDate.text.clear()
        binding.etInitialPrice.text.clear()
        binding.etMileage.text.clear()
        binding.spinnerCondition.setSelection(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}