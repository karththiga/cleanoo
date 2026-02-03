package com.example.rewardrecycleapp

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import android.location.LocationManager
import android.content.Context


class RequestPickupActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var map: GoogleMap
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private var hasCenteredMap = false
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val target = LatLng(location.latitude, location.longitude)
            updateMapLocation(target, 16f)
        }
    }

    private var imageUri: Uri? = null
    private var selectedCategory = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_pickup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapPickup) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val imgUpload = findViewById<ImageView>(R.id.imgUpload)
        val edtWeight = findViewById<EditText>(R.id.edtWeight)
        val edtLocation = findViewById<EditText>(R.id.edtLocation)
        val edtDate = findViewById<EditText>(R.id.edtDate)
        val edtTime = findViewById<EditText>(R.id.edtTime)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitPickup)

        // Category buttons
        val categories = mapOf(
            R.id.catPlastic to "Plastic",
            R.id.catPaper to "Paper",
            R.id.catGlass to "Glass",
            R.id.catMetal to "Metal",
            R.id.catEwaste to "E-Waste"
        )

        categories.forEach { (id, type) ->
            findViewById<TextView>(id).setOnClickListener {
                selectedCategory = type
                highlightCategory(id, categories.keys)
            }
        }

        imgUpload.setOnClickListener { pickImage() }
        edtDate.setOnClickListener { pickDate(edtDate) }
        edtTime.setOnClickListener { pickTime(edtTime) }

        btnSubmit.setOnClickListener {
            uploadPickupRequest(
                selectedCategory,
                edtWeight.text.toString(),
                edtLocation.text.toString(),
                edtDate.text.toString(),
                edtTime.text.toString()
            )
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isMyLocationButtonEnabled = true
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        map.isMyLocationEnabled = true
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Enable location services to show your position", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        fetchLastKnownLocation()
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(LOCATION_UPDATE_MIN_INTERVAL)
            .setMaxUpdateDelayMillis(LOCATION_UPDATE_MAX_DELAY)
            .build()

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun fetchLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
                val target = LatLng(lastLocation.latitude, lastLocation.longitude)
                updateMapLocation(target, 15f)
            }
        }
    }

    private fun updateMapLocation(target: LatLng, zoom: Float) {
        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(target)
                .title("Current Location")
        )
        if (!hasCenteredMap) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom))
            hasCenteredMap = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        }
    }

    override fun onStart() {
        super.onStart()
        if (::map.isInitialized) {
            enableMyLocation()
        }
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun highlightCategory(selectedId: Int, allIds: Set<Int>) {
        allIds.forEach {
            findViewById<TextView>(it).setBackgroundResource(R.drawable.bg_card_white)
        }
        findViewById<TextView>(selectedId).setBackgroundResource(R.drawable.bg_card_green)
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)

        if (req == 101 && res == RESULT_OK) {
            imageUri = data?.data
            findViewById<ImageView>(R.id.imgUpload).setImageURI(imageUri)
        }
    }

    private fun pickDate(editText: EditText) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            editText.setText("$d-${m+1}-$y")
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime(editText: EditText) {
        val c = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            editText.setText("$h:$m")
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
    }

    private fun uploadPickupRequest(category: String, weight: String, location: String, date: String, time: String) {
        if (category.isEmpty()) {
            Toast.makeText(this, "Select category", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: return
        val id = System.currentTimeMillis().toString()

        val data = hashMapOf(
            "pickupId" to id,
            "userId" to user.uid,
            "category" to category,
            "weight" to weight,
            "location" to location,
            "date" to date,
            "time" to time,
            "status" to "Pending",
            "timestamp" to FieldValue.serverTimestamp()
        )

        if (imageUri == null) {
            saveToFirestore(id, data)
        } else {
            uploadImage(id, data)
        }
    }

    private fun uploadImage(id: String, data: HashMap<String, Any>) {
        imageUri?.let { uri ->
            val ref = storage.reference.child("pickupImages/$id.jpg")
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        data["imageUrl"] = url.toString()
                        saveToFirestore(id, data)
                    }
                }
        }
    }

    private fun saveToFirestore(id: String, data: HashMap<String, Any>) {
        db.collection("pickups").document(id).set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Pickup request submitted", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1201
        private const val LOCATION_UPDATE_INTERVAL = 2000L
        private const val LOCATION_UPDATE_MIN_INTERVAL = 1000L
        private const val LOCATION_UPDATE_MAX_DELAY = 2000L
    }
}
