package com.example.rewardrecycleapp

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream

class CollectorEvidenceFragment : Fragment() {

    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
        view?.findViewById<TextView>(R.id.tvEvidencePhotoState)?.text =
            if (uri == null) "No photo selected" else "Photo selected"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collector_evidence, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pickupId = arguments?.getString(ARG_PICKUP_ID)
        if (pickupId.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvEvidenceStatus).text = "Pickup id missing"
            return
        }

        view.findViewById<Button>(R.id.btnUploadEvidencePhoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        view.findViewById<Button>(R.id.btnSaveEvidence).setOnClickListener {
            val weightInput = view.findViewById<EditText>(R.id.edtEvidenceWeight)
            val weightValue = weightInput.text.toString().trim()
            val statusView = view.findViewById<TextView>(R.id.tvEvidenceStatus)

            if (imageUri == null) {
                statusView.text = "Please upload a pickup photo"
                Toast.makeText(requireContext(), "Please upload a pickup photo", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (weightValue.isBlank()) {
                weightInput.error = "Please enter weight"
                statusView.text = "Please enter pickup weight"
                Toast.makeText(requireContext(), "Please enter weight", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val weightNumber = weightValue.toDoubleOrNull()
            if (weightNumber == null || weightNumber <= 0.0) {
                weightInput.error = "Weight must be greater than 0"
                statusView.text = "Please enter a valid pickup weight"
                Toast.makeText(requireContext(), "Please enter a valid pickup weight", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val imageFile = createTempImageFile(imageUri!!) ?: run {
                statusView.text = "Failed to process image"
                Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val saveButton = view.findViewById<Button>(R.id.btnSaveEvidence)
            saveButton.isEnabled = false
            saveButton.text = "Saving..."

            MobileBackendApi.submitCollectorPickupEvidence(pickupId, imageFile, weightValue) { success, message ->
                activity?.runOnUiThread {
                    imageFile.delete()
                    saveButton.isEnabled = true
                    saveButton.text = "Save evidence"

                    if (!success) {
                        view.findViewById<TextView>(R.id.tvEvidenceStatus).text = message ?: "Evidence upload failed"
                        return@runOnUiThread
                    }

                    view.findViewById<TextView>(R.id.tvEvidenceStatus).text = "Evidence saved. Waiting for household confirmation."
                    parentFragmentManager.popBackStack()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun createTempImageFile(uri: Uri): File? {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File.createTempFile("collector_proof_", ".jpg", requireContext().cacheDir)
            input.use { ins ->
                FileOutputStream(file).use { out -> ins.copyTo(out) }
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val ARG_PICKUP_ID = "pickup_id"

        fun newInstance(pickupId: String): CollectorEvidenceFragment {
            val frag = CollectorEvidenceFragment()
            frag.arguments = Bundle().apply { putString(ARG_PICKUP_ID, pickupId) }
            return frag
        }
    }
}
