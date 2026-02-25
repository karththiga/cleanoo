package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class RewardFragment : Fragment() {

    private lateinit var etWeight: EditText
    private lateinit var spWasteType: Spinner
    private lateinit var btnCalculateRewards: Button
    private lateinit var tvEarnedPoints: TextView

    private val wasteTypes = listOf(
        "Plastic",
        "Paper",
        "Glass",
        "Metal",
        "General Waste",
        "Sanitary Waste",
        "Hazardous Waste"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_reward, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etWeight = view.findViewById(R.id.etWeight)
        spWasteType = view.findViewById(R.id.spWasteType)
        btnCalculateRewards = view.findViewById(R.id.btnCalculateRewards)
        tvEarnedPoints = view.findViewById(R.id.tvEarnedPoints)

        setupWasteTypeDropdown()
        setupCalculateButton()
    }

    private fun setupWasteTypeDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            wasteTypes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spWasteType.adapter = adapter
    }

    private fun setupCalculateButton() {
        btnCalculateRewards.setOnClickListener {
            val weightText = etWeight.text.toString().trim()
            val weightInKg = weightText.toDoubleOrNull()

            if (weightInKg == null || weightInKg <= 0.0) {
                Toast.makeText(requireContext(), "Please enter a valid weight", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val selectedType = spWasteType.selectedItem.toString()
            val points = getEarnedPoints(weightInKg, selectedType)
            tvEarnedPoints.text = "Earned Points: $points"
        }
    }

    fun getEarnedPoints(weightInKg: Double, wasteType: String): Int {
        return RewardCalculator.calculateRewardPoints(weightInKg, wasteType)
    }
}
