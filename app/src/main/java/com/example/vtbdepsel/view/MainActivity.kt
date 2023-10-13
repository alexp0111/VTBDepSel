package com.example.vtbdepsel.view

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.example.vtbdepsel.R
import com.example.vtbdepsel.utils.UiState
import com.example.vtbdepsel.viewmodel.MainViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    private lateinit var placemarksCollection: MapObjectCollection
    private lateinit var mapView: MapView
    private lateinit var map: Map

    private var picker = mutableMapOf<ConstraintLayout, ImageView>()
    private lateinit var pickerCircle: ConstraintLayout
    private lateinit var searchFAB: FloatingActionButton
    private lateinit var plusFAB: FloatingActionButton
    private lateinit var minusFAB: FloatingActionButton
    private lateinit var findMeFAB: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(this)
        DirectionsFactory.initialize(this)
        setContentView(R.layout.activity_main)

        // Init views
        initViews()
        setUpClickListeners()

        // Set up viewModel observers
        observers()

        mapView = findViewById(R.id.mapview)
        map = mapView.mapWindow.map
        placemarksCollection = map.mapObjects.addCollection()

        resetCameraToUser()

        viewModel.updateDepartments()
    }

    private fun resetCameraToUser() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val userPoint = Point(location.latitude, location.longitude)
                setCameraToPosition(userPoint, ZOOM_BASE)
                drawUserLocationPoint(userPoint)
            }
        }
    }

    private fun initViews() {
        searchFAB = findViewById(R.id.btn_search)
        plusFAB = findViewById(R.id.btn_plus)
        minusFAB = findViewById(R.id.btn_minus)
        findMeFAB = findViewById(R.id.btn_nav)
        pickerCircle = findViewById(R.id.picker_circle)
        picker[findViewById(R.id.picker_1)] = findViewById(R.id.img_picker_1)
        picker[findViewById(R.id.picker_2)] = findViewById(R.id.img_picker_2)
        picker[findViewById(R.id.picker_3)] = findViewById(R.id.img_picker_3)

        PickerAnimator.animate(
            resources, this, picker, pickerCircle
        )
    }

    private fun setUpClickListeners() {
        searchFAB.setOnClickListener {
            viewModel.ping()
        }
        plusFAB.setOnClickListener {
            setCameraToPosition(zoom = map.cameraPosition.zoom * ZOOM_IN_MULT)
        }
        minusFAB.setOnClickListener {
            setCameraToPosition(zoom = map.cameraPosition.zoom * ZOOM_OUT_MULT)
        }
        findMeFAB.setOnClickListener {
            resetCameraToUser()
        }
    }

    private fun drawUserLocationPoint(point: Point) {
        val imageProvider = ImageProvider.fromResource(this, R.drawable.my_point)
        placemarksCollection.addPlacemark(
            point,
            imageProvider,
            IconStyle().apply {
                scale = 0.3f
                zIndex = 1f
            }
        )
    }

    private fun setCameraToPosition(
        point: Point = map.cameraPosition.target,
        zoom: Float = map.cameraPosition.zoom,
    ) {
        map.move(
            CameraPosition(
                point,
                zoom,
                map.cameraPosition.azimuth,
                map.cameraPosition.tilt
            )
        )
    }

    private fun observers() {
        viewModel.depList.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // show progressbar
                }
                is UiState.Failure -> {
                    // toast(state.error)
                }

                is UiState.Success -> {
                    // show points
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    companion object {
        private const val ZOOM_IN_MULT = 1.1f
        private const val ZOOM_OUT_MULT = 0.9f
        private const val ZOOM_BASE = 14.0f
    }

}