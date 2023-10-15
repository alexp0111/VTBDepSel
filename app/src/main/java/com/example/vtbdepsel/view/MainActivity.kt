package com.example.vtbdepsel.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.provider.CalendarContract.Events
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.db.williamchart.view.LineChartView
import com.example.vtbdepsel.R
import com.example.vtbdepsel.model.api.data.ApiATMItem
import com.example.vtbdepsel.model.api.data.ApiBranchItem
import com.example.vtbdepsel.utils.ClusterATMView
import com.example.vtbdepsel.utils.ClusterBranchView
import com.example.vtbdepsel.utils.PlacemarkATM
import com.example.vtbdepsel.utils.PlacemarkBranch
import com.example.vtbdepsel.utils.PlacemarkType
import com.example.vtbdepsel.utils.UiState
import com.example.vtbdepsel.utils.showToast
import com.example.vtbdepsel.utils.styleAlternativeRoute
import com.example.vtbdepsel.utils.styleMainRoute
import com.example.vtbdepsel.viewmodel.MainViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.ui_view.ViewProvider
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

/**
 * Key UI component of the application.
 * It holds all UI logic due to achieve simple context workflow in MVP
 * */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener {
    private val viewModel: MainViewModel by viewModels()

    // userpoint collection
    private lateinit var placemarksCollection: MapObjectCollection

    // routes collection with metadata
    private var myRoutesPolylineCollection =
        mutableListOf<Triple<PolylineMapObject, String, String>>()
    private lateinit var mapView: MapView
    private lateinit var map: Map

    // base ui components
    private var picker = mutableMapOf<ConstraintLayout, ImageView>()
    private lateinit var pickerCircle: ConstraintLayout
    private lateinit var searchFAB: FloatingActionButton
    private lateinit var plusFAB: FloatingActionButton
    private lateinit var minusFAB: FloatingActionButton
    private lateinit var findMeFAB: FloatingActionButton

    // Current user info
    private var userPoint = Point()
    private var addressToVisit = ""

    // Map of deps & atms data
    private var branchMap = mutableMapOf<Int, ApiBranchItem>()
    private var atmMap = mutableMapOf<Int, ApiATMItem>()


    // Listener for Department collection config
    private val clusterBranchListener = ClusterListener { cluster ->
        val placemarkTypes = cluster.placemarks.map {
            (it.userData as PlacemarkBranch).type
        }

        cluster.appearance.setView(
            ViewProvider(
                ClusterBranchView(this).apply {
                    setData(placemarkTypes)
                }
            )
        )

        cluster.appearance.zIndex = 100f
    }

    // Listener for ATM collection config
    private val clusterATMListener = ClusterListener { cluster ->
        val placemarkTypes = cluster.placemarks.map {
            (it.userData as PlacemarkATM).type
        }

        cluster.appearance.setView(
            ViewProvider(
                ClusterATMView(this).apply {
                    setData(placemarkTypes)
                }
            )
        )

        cluster.appearance.zIndex = 100f
    }

    // Listener for routes
    private val drivingRouteListener = object : DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
            routes = drivingRoutes

            drivingRoutes.forEach {
                findViewById<TextView>(R.id.total_time).text = it.metadata.weight.time.text
                findViewById<TextView>(R.id.total_dist).text = it.metadata.weight.distance.text
            }
        }

        override fun onDrivingRoutesError(error: Error) {
            when (error) {
                is NetworkError -> showToast("Routes request error due network issues")
                else -> showToast("Routes request unknown error")
            }
        }
    }

    // Listener for Dep touch event
    private val onBranchPointListener =
        MapObjectTapListener { mapObject, _ ->
            branchMap[(mapObject.userData as PlacemarkBranch).id]?.let {
                showDepDialog(it)
            }
            true
        }

    // Listener for ATM touch event
    private val onATMPointListener = MapObjectTapListener { mapObject, _ ->
        atmMap[(mapObject.userData as PlacemarkATM).id]?.let {
            showATMDialog(it)
        }
        true
    }

    // Route tap listener
    private val onMapObjectTapListener =
        MapObjectTapListener { mapObject, _ ->
            try {
                myRoutesPolylineCollection.forEach {
                    it.first.styleAlternativeRoute(this@MainActivity)
                }
                (mapObject as PolylineMapObject).styleMainRoute(this@MainActivity)
                for (i in myRoutesPolylineCollection) {
                    if (i.first.hashCode() == mapObject.hashCode()) {
                        findViewById<TextView>(R.id.total_time).text = i.second
                        findViewById<TextView>(R.id.total_dist).text = i.third
                    }
                }
            } catch (e: Exception) {
                Log.d("MAIN_ACTIVITY", "Something went wrong")
            }
            true
        }

    private var routePoints = emptyList<Point>()
        set(value) {
            field = value
            onRoutePointsUpdated()
        }


    //Listener for route points updates
    private fun onRoutePointsUpdated() {
        myRoutesPolylineCollection.clear()
        val requestPoints = buildList {
            add(RequestPoint(userPoint, RequestPointType.WAYPOINT, null))
            add(RequestPoint(routePoints.first(), RequestPointType.WAYPOINT, null))
        }

        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()

        drivingSession = drivingRouter.requestRoutes(
            requestPoints,
            drivingOptions,
            vehicleOptions,
            drivingRouteListener
        )
    }


    private var routes = emptyList<DrivingRoute>()
        set(value) {
            field = value
            onRoutesUpdated()
        }

    //Listener for route updates
    private fun onRoutesUpdated() {
        routesCollection.clear()
        if (routes.isEmpty()) return

        routes.forEachIndexed { index, route ->
            routesCollection.addPolyline(route.geometry).apply {
                if (index == 0) styleMainRoute(this@MainActivity) else styleAlternativeRoute(this@MainActivity)
                myRoutesPolylineCollection.add(
                    Triple(
                        this,
                        route.metadata.weight.time.text,
                        route.metadata.weight.distance.text
                    )
                )
                addTapListener(onMapObjectTapListener)
            }
        }
    }


    private lateinit var drivingRouter: DrivingRouter
    private var drivingSession: DrivingSession? = null
    private lateinit var clasterizedBranchCollection: ClusterizedPlacemarkCollection
    private lateinit var clasterizedATMCollection: ClusterizedPlacemarkCollection
    private lateinit var routesCollection: MapObjectCollection

    private var placemarkBranchTypeToImageProvider = mapOf<PlacemarkType, ImageProvider>()
    private var placemarkATMTypeToImageProvider = mapOf<PlacemarkType, ImageProvider>()

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

        // Init Mapkit components
        mapView = findViewById(R.id.mapview)
        map = mapView.mapWindow.map
        routesCollection = map.mapObjects.addCollection()
        placemarksCollection = map.mapObjects.addCollection()
        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()

        // Cluster for branch & ATM
        clasterizedBranchCollection =
            map.mapObjects.addClusterizedPlacemarkCollection(clusterBranchListener)

        clasterizedATMCollection =
            map.mapObjects.addClusterizedPlacemarkCollection(clusterATMListener)

        placemarkBranchTypeToImageProvider = mapOf(
            PlacemarkType.LOW to ImageProvider.fromResource(this, R.drawable.bank_low),
            PlacemarkType.MEDIUM to ImageProvider.fromResource(this, R.drawable.bank_medium),
            PlacemarkType.HIGH to ImageProvider.fromResource(this, R.drawable.bank_high),
            PlacemarkType.PREMIUM to ImageProvider.fromResource(this, R.drawable.bank_premium),
        )

        placemarkATMTypeToImageProvider = mapOf(
            PlacemarkType.LOW to ImageProvider.fromResource(this, R.drawable.atm_low),
            PlacemarkType.MEDIUM to ImageProvider.fromResource(this, R.drawable.atm_medium),
            PlacemarkType.HIGH to ImageProvider.fromResource(this, R.drawable.atm_high),
        )

        clasterizedBranchCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)
        clasterizedBranchCollection.addTapListener(onBranchPointListener)

        clasterizedATMCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)
        clasterizedATMCollection.addTapListener(onATMPointListener)

        // Find user

        resetCameraToUser {
            viewModel.updateDepartments(it.latitude, it.longitude)
            viewModel.updateAtms(it.latitude, it.longitude)
            routePoints = routePoints + it
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
            resources, picker, pickerCircle
        )
    }

    private fun setUpClickListeners() {
        searchFAB.setOnClickListener {
            showFilterDialog()
        }
        plusFAB.setOnClickListener {
            setCameraToPosition(zoom = map.cameraPosition.zoom * ZOOM_IN_MULT)
        }
        minusFAB.setOnClickListener {
            setCameraToPosition(zoom = map.cameraPosition.zoom * ZOOM_OUT_MULT)
        }
        findMeFAB.setOnClickListener {
            resetCameraToUser {}
        }
    }

    // Set up livedata observers
    private fun observers() {
        viewModel.depList.observe(this) { state ->
            when (state) {
                is UiState.Failure -> {
                    Log.d("MAIN_ACTIVITY", state.error.toString())
                }

                is UiState.Success -> {
                    branchMap.clear()
                    state.data.forEach {
                        branchMap[it.id] = it
                        Log.d("MAIN", it.capacity.toString())
                        val type = when (it.capacity) {
                            in (0..20) -> PlacemarkType.LOW
                            in (21..70) -> PlacemarkType.MEDIUM
                            else -> PlacemarkType.HIGH
                        }
                        val imageProvider = placemarkBranchTypeToImageProvider[type]!!
                        clasterizedBranchCollection.addPlacemark(
                            Point(it.latitude ?: 0.0, it.longitude ?: 0.0),
                            imageProvider,
                            IconStyle().apply {
                                anchor = PointF(0.5f, 1.0f)
                                scale = 0.1f
                            }
                        )
                            .apply {
                                userData = PlacemarkBranch(it.id, type)
                            }
                    }
                    clasterizedBranchCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)
                }

                else -> {}
            }
        }

        viewModel.atmList.observe(this) { state ->
            when (state) {
                is UiState.Failure -> {
                    Log.d("MAIN_ACT", state.error.toString())
                }

                is UiState.Success -> {
                    atmMap.clear()
                    state.data.forEach {
                        atmMap[it.id] = it
                        val type = listOf(
                            PlacemarkType.LOW,
                            PlacemarkType.MEDIUM,
                            PlacemarkType.HIGH
                        ).random()
                        val imageProvider = placemarkATMTypeToImageProvider[type]!!
                        clasterizedATMCollection.addPlacemark(
                            Point(it.latitude, it.longitude),
                            imageProvider,
                            IconStyle().apply {
                                anchor = PointF(0.5f, 1.0f)
                                scale = 0.1f
                            }
                        )
                            .apply {
                                userData = PlacemarkATM(it.id, type)
                            }
                    }
                    clasterizedATMCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)
                }
                else -> {}
            }
        }
    }


    /**
     * Info dialog for ATM
     * */
    private fun showATMDialog(atm: ApiATMItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_atm)

        // init
        val close = dialog.findViewById<ImageView>(R.id.imageView)
        val schedule = dialog.findViewById<LineChartView>(R.id.line_chart)
        val address = dialog.findViewById<TextView>(R.id.atm_txt_address)
        val isAllDay = dialog.findViewById<ImageView>(R.id.atm_img_is_all_day)
        val buildRoute = dialog.findViewById<TextView>(R.id.btn_find_route)

        address.text = atm.address
        if (atm.allday) {
            isAllDay.setImageResource(R.drawable.approved)
        } else {
            isAllDay.setImageResource(R.drawable.not_approved)
        }

        close.setOnClickListener {
            dialog.hide()
        }

        buildRoute.setOnClickListener {
            routePoints = listOf(Point(atm.latitude, atm.longitude))
            dialog.hide()
        }


        schedule.gradientFillColors =
            intArrayOf(
                ContextCompat.getColor(this, R.color.md_theme_light_onPrimary),
                Color.TRANSPARENT
            )
        schedule.animation.duration = SCHEDULE_ANIMATION_DURATION

        val currentList = getTmpList()
        schedule.animate(currentList)

        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.DialogAnimation
            setGravity(Gravity.BOTTOM)
        }
    }

    /**
     * Info dialog for Department
     * */
    private fun showDepDialog(branch: ApiBranchItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_department)

        // init
        val close = dialog.findViewById<ImageView>(R.id.imageView)
        val schedule = dialog.findViewById<LineChartView>(R.id.line_chart)
        val ulList = dialog.findViewById<ListView>(R.id.dep_lv_hours_ul)
        val flList = dialog.findViewById<ListView>(R.id.dep_lv_hours_fl)
        val address = dialog.findViewById<TextView>(R.id.dep_txt_address)
        val rko = dialog.findViewById<TextView>(R.id.dep_txt_rko_value)
        val type = dialog.findViewById<TextView>(R.id.dep_txt_office_type_value)
        val format = dialog.findViewById<TextView>(R.id.dep_txt_format_value)
        val suo = dialog.findViewById<ImageView>(R.id.dep_img_suo)
        val ramp = dialog.findViewById<ImageView>(R.id.dep_img_ramp)
        val kep = dialog.findViewById<ImageView>(R.id.dep_img_kep)
        val metro = dialog.findViewById<TextView>(R.id.dep_txt_metro_value)

        val buildRoute = dialog.findViewById<MaterialButton>(R.id.btn_find_route)
        val addEvent = dialog.findViewById<FloatingActionButton>(R.id.btn_add_event)
        val feedback = dialog.findViewById<FloatingActionButton>(R.id.btn_feedback)

        close.setOnClickListener {
            dialog.hide()
        }

        buildRoute.setOnClickListener {
            routePoints = listOf(Point(branch.latitude ?: 0.0, branch.longitude ?: 0.0))
            dialog.hide()
        }

        addEvent.setOnClickListener {
            TimePickerDialog(
                dialog.window?.context,
                this,
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                Calendar.getInstance().get(Calendar.MINUTE),
                true
            ).show()
            addressToVisit = branch.address ?: ""
        }

        feedback.setOnClickListener {
            val dialog2 = Dialog(this.window.context)
            dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog2.setContentView(R.layout.bottom_rating)

            dialog2.show()
            dialog2.window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                attributes.windowAnimations = R.style.DialogAnimation
                setGravity(Gravity.BOTTOM)
            }
        }

        //
        val functions = mapOf<Int, ImageView>(
            1 to dialog.findViewById(R.id.dep_img_s1),
            2 to dialog.findViewById(R.id.dep_img_s2),
            3 to dialog.findViewById(R.id.dep_img_s3),
            4 to dialog.findViewById(R.id.dep_img_s4),
            5 to dialog.findViewById(R.id.dep_img_s5),
            6 to dialog.findViewById(R.id.dep_img_s6)
        )
        //

        address.text = branch.address
        rko.text = branch.rko
        type.text = branch.officeType
        format.text = branch.salePointFormat

        if (branch.suoAvailability != null && branch.suoAvailability) {
            suo.setImageResource(R.drawable.approved)
        } else {
            suo.setImageResource(R.drawable.not_approved)
        }

        if (branch.hasRamp != null && branch.hasRamp) {
            ramp.setImageResource(R.drawable.approved)
        } else {
            ramp.setImageResource(R.drawable.not_approved)
        }

        if (branch.kep != null && branch.kep == "true") {
            kep.setImageResource(R.drawable.approved)
        } else {
            kep.setImageResource(R.drawable.not_approved)
        }

        metro.text = branch.metroStation

        branch.foos?.forEach {
            val key = functionToNumber[it.functionName]
            functions[key]?.setImageResource(R.drawable.approved)
        }

        val adapter = MyListAdapter(this, ARR_DATES, ARR_HOURS)
        ulList.adapter = adapter
        flList.adapter = adapter

        //

        schedule.gradientFillColors =
            intArrayOf(
                ContextCompat.getColor(this, R.color.md_theme_light_onPrimary),
                Color.TRANSPARENT
            )
        schedule.animation.duration = SCHEDULE_ANIMATION_DURATION

        val currentList = getTmpList()
        schedule.animate(currentList)

        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.DialogAnimation
            setGravity(Gravity.BOTTOM)
        }
    }

    /**
     * Add Calendar event feature
     * */
    private fun addCalendarEvent(time: Long) {
        val intent = Intent(Intent.ACTION_EDIT)
        intent.type = "vnd.android.cursor.item/event"
        intent.putExtra("beginTime", time)
        intent.putExtra("allDay", false)
        intent.putExtra("title", "Посетить отделение ВТБ")
        intent.putExtra(Events.EVENT_LOCATION, addressToVisit)
        startActivity(intent)
    }

    /**
     * Filter dialogue
     * */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showFilterDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_filter)

        val close = dialog.findViewById<ImageView>(R.id.imageView)
        val rko = dialog.findViewById<TextView>(R.id.fil_txt_rko_value);
        val type = dialog.findViewById<TextView>(R.id.fil_txt_office_type_value);
        val format = dialog.findViewById<TextView>(R.id.fil_txt_format_value);
        val suo = dialog.findViewById<ImageView>(R.id.fil_img_suo); suo.tag =
            R.drawable.not_approved
        val ramp = dialog.findViewById<ImageView>(R.id.fil_img_ramp); ramp.tag =
            R.drawable.not_approved
        val kep = dialog.findViewById<ImageView>(R.id.fil_img_kep); kep.tag =
            R.drawable.not_approved

        rko.setOnClickListener {
            if (rko.text == "нет РКО") {
                rko.text = "есть РКО"
                rko.setTextColor(resources.getColor(R.color.md_theme_dark_primary, resources.newTheme()))
            } else {
                rko.text = "нет РКО"
                rko.setTextColor(resources.getColor(R.color.unpicked, resources.newTheme()))
            }
        }

        type.setOnClickListener {
            if (type.text == "Обычное") {
                type.text = "Привилегированное"
                type.setTextColor(resources.getColor(R.color.premium, resources.newTheme()))
            } else {
                type.text = "Обычное"
                type.setTextColor(resources.getColor(R.color.unpicked, resources.newTheme()))
            }
        }

        format.setOnClickListener {
            if (format.text == "Стандарт") {
                format.text = "Универсальный"
                format.setTextColor(resources.getColor(R.color.md_theme_dark_primary, resources.newTheme()))
            } else {
                format.text = "Стандарт"
                format.setTextColor(resources.getColor(R.color.unpicked, resources.newTheme()))
            }
        }

        val curRubCl = dialog.findViewById<ConstraintLayout>(R.id.fil_cl_rub); curRubCl.tag = false
        val curUsdCl = dialog.findViewById<ConstraintLayout>(R.id.fil_cl_usd); curUsdCl.tag = false
        val curEurCl = dialog.findViewById<ConstraintLayout>(R.id.fil_cl_eur); curEurCl.tag = false

        val curRubTV = dialog.findViewById<TextView>(R.id.fil_txt_rub); curRubTV.tag = false
        val curUsdTV = dialog.findViewById<TextView>(R.id.fil_txt_usd); curUsdTV.tag = false
        val curEurTV = dialog.findViewById<TextView>(R.id.fil_txt_eur); curEurTV.tag = false

        curRubCl.setOnClickListener {
            if (curRubCl.tag == false) {
                curRubCl.background = resources.getDrawable(R.drawable.picker_background, resources.newTheme())
                curRubTV.setTextColor(resources.getColor(R.color.white, resources.newTheme()))
                curRubCl.tag = true
                curRubTV.tag = true
            } else {
                curRubCl.background = null
                curRubTV.setTextColor(resources.getColor(R.color.unpicked, resources.newTheme()))
                curRubCl.tag = false
                curRubTV.tag = false
            }
        }

        curUsdCl.setOnClickListener {
            if (curUsdCl.tag == false) {
                curUsdCl.background = resources.getDrawable(R.drawable.picker_background, resources.newTheme())
                curUsdTV.setTextColor(resources.getColor(R.color.white, resources.newTheme()))
                curUsdCl.tag = true
                curUsdTV.tag = true
            } else {
                curUsdCl.background = null
                curUsdTV.setTextColor(resources.getColor(R.color.unpicked, resources.newTheme()))
                curUsdCl.tag = false
                curUsdTV.tag = false
            }
        }

        curEurCl.setOnClickListener {
            if (curEurCl.tag == false) {
                curEurCl.background = resources.getDrawable(R.drawable.picker_background, resources.newTheme())
                curEurTV.setTextColor(resources.getColor(R.color.white, resources.newTheme()))
                curEurCl.tag = true
                curEurTV.tag = true
            } else {
                curEurCl.background = null
                curEurTV.setTextColor(resources.getColor(R.color.unpicked, resources.newTheme()))
                curEurCl.tag = false
                curEurTV.tag = false
            }
        }

        val chargeRub = dialog.findViewById<ImageView>(R.id.fil_img_charge); chargeRub.tag =
            R.drawable.not_approved
        val wheelchair = dialog.findViewById<ImageView>(R.id.fil_img_wheelchair); wheelchair.tag =
            R.drawable.not_approved
        val blind = dialog.findViewById<ImageView>(R.id.fil_img_blind); blind.tag =
            R.drawable.not_approved
        val nfc = dialog.findViewById<ImageView>(R.id.fil_img_nfc); nfc.tag =
            R.drawable.not_approved
        val qr = dialog.findViewById<ImageView>(R.id.fil_img_qr); qr.tag = R.drawable.not_approved

        val s1 = dialog.findViewById<ImageView>(R.id.fil_img_s1); s1.tag = R.drawable.not_approved
        val s2 = dialog.findViewById<ImageView>(R.id.fil_img_s2); s2.tag = R.drawable.not_approved
        val s3 = dialog.findViewById<ImageView>(R.id.fil_img_s3); s3.tag = R.drawable.not_approved
        val s4 = dialog.findViewById<ImageView>(R.id.fil_img_s4); s4.tag = R.drawable.not_approved
        val s5 = dialog.findViewById<ImageView>(R.id.fil_img_s5); s5.tag = R.drawable.not_approved
        val s6 = dialog.findViewById<ImageView>(R.id.fil_img_s6); s6.tag = R.drawable.not_approved


        val findDep = dialog.findViewById<MaterialButton>(R.id.btn_find_dep)
        val findAtm = dialog.findViewById<MaterialButton>(R.id.btn_find_atm)

        close.setOnClickListener {
            dialog.hide()
        }

        suo.setOnClickListener {
            if (suo.tag == R.drawable.not_approved) {
                suo.setImageResource(R.drawable.approved)
                suo.tag = R.drawable.approved
            } else {
                suo.setImageResource(R.drawable.not_approved)
                suo.tag = R.drawable.not_approved
            }
        }
        ramp.setOnClickListener {
            if (ramp.tag == R.drawable.not_approved) {
                ramp.setImageResource(R.drawable.approved)
                ramp.tag = R.drawable.approved
            } else {
                ramp.setImageResource(R.drawable.not_approved)
                ramp.tag = R.drawable.not_approved
            }
        }
        kep.setOnClickListener {
            if (kep.tag == R.drawable.not_approved) {
                kep.setImageResource(R.drawable.approved)
                kep.tag = R.drawable.approved
            } else {
                kep.setImageResource(R.drawable.not_approved)
                kep.tag = R.drawable.not_approved
            }
        }

        //
        chargeRub.setOnClickListener {
            if (chargeRub.tag == R.drawable.not_approved) {
                chargeRub.setImageResource(R.drawable.approved)
                chargeRub.tag = R.drawable.approved
            } else {
                chargeRub.setImageResource(R.drawable.not_approved)
                chargeRub.tag = R.drawable.not_approved
            }
        }
        wheelchair.setOnClickListener {
            if (wheelchair.tag == R.drawable.not_approved) {
                wheelchair.setImageResource(R.drawable.approved)
                wheelchair.tag = R.drawable.approved
            } else {
                wheelchair.setImageResource(R.drawable.not_approved)
                wheelchair.tag = R.drawable.not_approved
            }
        }
        blind.setOnClickListener {
            if (blind.tag == R.drawable.not_approved) {
                blind.setImageResource(R.drawable.approved)
                blind.tag = R.drawable.approved
            } else {
                blind.setImageResource(R.drawable.not_approved)
                blind.tag = R.drawable.not_approved
            }
        }
        nfc.setOnClickListener {
            if (nfc.tag == R.drawable.not_approved) {
                nfc.setImageResource(R.drawable.approved)
                nfc.tag = R.drawable.approved
            } else {
                nfc.setImageResource(R.drawable.not_approved)
                nfc.tag = R.drawable.not_approved
            }
        }
        qr.setOnClickListener {
            if (qr.tag == R.drawable.not_approved) {
                qr.setImageResource(R.drawable.approved)
                qr.tag = R.drawable.approved
            } else {
                qr.setImageResource(R.drawable.not_approved)
                qr.tag = R.drawable.not_approved
            }
        }

        //

        s1.setOnClickListener {
            if (s1.tag == R.drawable.not_approved) {
                s1.setImageResource(R.drawable.approved)
                s1.tag = R.drawable.approved
            } else {
                s1.setImageResource(R.drawable.not_approved)
                s1.tag = R.drawable.not_approved
            }
        }
        s2.setOnClickListener {
            if (s2.tag == R.drawable.not_approved) {
                s2.setImageResource(R.drawable.approved)
                s2.tag = R.drawable.approved
            } else {
                s2.setImageResource(R.drawable.not_approved)
                s2.tag = R.drawable.not_approved
            }
        }
        s3.setOnClickListener {
            if (s3.tag == R.drawable.not_approved) {
                s3.setImageResource(R.drawable.approved)
                s3.tag = R.drawable.approved
            } else {
                s3.setImageResource(R.drawable.not_approved)
                s3.tag = R.drawable.not_approved
            }
        }
        s4.setOnClickListener {
            if (s4.tag == R.drawable.not_approved) {
                s4.setImageResource(R.drawable.approved)
                s4.tag = R.drawable.approved
            } else {
                s4.setImageResource(R.drawable.not_approved)
                s4.tag = R.drawable.not_approved
            }
        }
        s5.setOnClickListener {
            if (s5.tag == R.drawable.not_approved) {
                s5.setImageResource(R.drawable.approved)
                s5.tag = R.drawable.approved
            } else {
                s5.setImageResource(R.drawable.not_approved)
                s5.tag = R.drawable.not_approved
            }
        }
        s6.setOnClickListener {
            if (s6.tag == R.drawable.not_approved) {
                s6.setImageResource(R.drawable.approved)
                s6.tag = R.drawable.approved
            } else {
                s6.setImageResource(R.drawable.not_approved)
                s6.tag = R.drawable.not_approved
            }
        }

        findAtm.setOnClickListener {
            viewModel.getPoint(userPoint.latitude, userPoint.longitude) {
                routePoints = listOf(Point(it.latitude, it.longitude))
                dialog.hide()
                setCameraToPosition(Point(it.latitude, it.longitude))
            }
        }

        findDep.setOnClickListener {
            viewModel.getPoint(userPoint.latitude, userPoint.longitude) {
                routePoints = listOf(Point(it.latitude, it.longitude))
                dialog.hide()
                setCameraToPosition(Point(it.latitude, it.longitude))
            }
        }

        //

        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.DialogAnimation
            setGravity(Gravity.BOTTOM)
        }
    }

    //
    // Working with camera
    //

    private fun drawUserLocationPoint(point: Point) {
        val imageProvider = ImageProvider.fromResource(this, R.drawable.my_point)
        placemarksCollection.clear()
        placemarksCollection.addPlacemark(
            point,
            imageProvider,
            IconStyle().apply {
                scale = 0.3f
                zIndex = 1f
            }
        )
    }

    private fun resetCameraToUser(result: (Point) -> Unit) {
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
                userPoint = Point(location.latitude, location.longitude)
                result.invoke(userPoint)
                setCameraToPosition(userPoint, ZOOM_BASE)
                drawUserLocationPoint(userPoint)
            }
        }
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
            ),
            Animation(Animation.Type.SMOOTH, 0.3f)
        ) {

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

    private fun getTmpList(): List<Pair<String, Float>> {
        return listOf(
            Pair("00:00", (1..100).random().toFloat()),
            Pair("01:00", (1..100).random().toFloat()),
            Pair("02:00", (1..100).random().toFloat()),
            Pair("03:00", (1..100).random().toFloat()),
            Pair("04:00", (1..100).random().toFloat()),
            Pair("05:00", (1..100).random().toFloat()),
            Pair("06:00", (1..100).random().toFloat()),
            Pair("07:00", (1..100).random().toFloat()),
            Pair("08:00", (1..100).random().toFloat()),
            Pair("09:00", (1..100).random().toFloat()),
            Pair("10:00", (1..100).random().toFloat()),
            Pair("11:00", (1..100).random().toFloat()),
            Pair("12:00", (1..100).random().toFloat()),
            Pair("13:00", (1..100).random().toFloat()),
            Pair("14:00", (1..100).random().toFloat()),
            Pair("15:00", (1..100).random().toFloat()),
            Pair("16:00", (1..100).random().toFloat()),
            Pair("17:00", (1..100).random().toFloat()),
            Pair("18:00", (1..100).random().toFloat()),
            Pair("19:00", (1..100).random().toFloat()),
            Pair("20:00", (1..100).random().toFloat()),
            Pair("21:00", (1..100).random().toFloat()),
            Pair("22:00", (1..100).random().toFloat()),
            Pair("23:00", (1..100).random().toFloat()),
        )
    }

    companion object {
        private const val ZOOM_IN_MULT = 1.1f
        private const val ZOOM_OUT_MULT = 0.9f
        private const val ZOOM_BASE = 14.0f

        private const val CLUSTER_RADIUS = 60.0
        private const val CLUSTER_MIN_ZOOM = 15


        private const val SCHEDULE_ANIMATION_DURATION = 1000L

        private val functionToNumber = mapOf(
            "Работа с вкладами" to 1,
            "Работа с кредитами" to 2,
            "Выпуск карты" to 3,
            "Выдача и обмен валюты" to 4,
            "Международные переводы" to 5,
            "Услуги страхования" to 6,
        )


        private val ARR_DATES = arrayOf("пн", "вт", "ср", "чт", "пт-вс")
        private val ARR_HOURS = arrayOf("10:00-19:00", "10:00-19:00", "10:00-19:00", "10:00-19:00", "10:00-29:00")
    }

    override fun onTimeSet(p0: TimePicker?, p1: Int, p2: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, p1)
        calendar.set(Calendar.MINUTE, p2)
        val time = calendar.time.time

        addCalendarEvent(time)
    }
}