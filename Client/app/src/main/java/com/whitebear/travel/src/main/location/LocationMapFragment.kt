package com.whitebear.travel.src.main.location

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.ContextCompat.startForegroundService
import com.whitebear.travel.config.BaseFragment
import com.whitebear.travel.src.main.MainActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.whitebear.travel.R
import com.whitebear.travel.config.ApplicationClass
import com.whitebear.travel.databinding.FragmentLocationMapBinding
import com.whitebear.travel.src.dto.Place
import com.whitebear.travel.src.dto.Responses
import com.whitebear.travel.src.dto.RouteLike
import com.whitebear.travel.src.dto.camping.Camping
import com.whitebear.travel.src.main.route.RouteDetailAdapter
import com.whitebear.travel.src.network.service.DataService
import com.whitebear.travel.src.network.viewmodel.MainViewModel
import kotlinx.coroutines.runBlocking
import net.daum.mf.map.api.*
import net.daum.mf.map.api.MapPoint.GeoCoordinate
import retrofit2.Response
import com.bumptech.glide.request.RequestOptions




/**
 * @since 04/22/22
 * @author Jiwoo Choi
 */
class LocationMapFragment : BaseFragment<FragmentLocationMapBinding>(FragmentLocationMapBinding::bind, R.layout.fragment_location_map), MapView.CurrentLocationEventListener {
    private val TAG = "LocationMapFragment"
    private lateinit var mainActivity : MainActivity

    private lateinit var mapView: MapView
    private lateinit var mapViewContainer : ViewGroup
    private var currentMapPoint : MapPoint? = null

    private var currentLat: Double = 35.869326
    private var currentLng: Double = 128.595565

    private var markerArr = arrayListOf<MapPoint>()

    private var range = 50.0

    private var campingPOIList = arrayListOf<MapPOIItem>()
    private var visible = true // ????????? ?????? on / off
    private lateinit var eventListener : MarkerEventListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        eventListener = MarkerEventListener(requireContext(), mainViewModel)   // ?????? ?????? ????????? ?????????
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity.hideBottomNav(true)


        mainViewModel.userLoc.observe(viewLifecycleOwner, {
            if(it != null) {
                runBlocking {
                    mainViewModel.getPlacesByGps(it.latitude, it.longitude, range)
//                    mainViewModel.getCampingList(it.latitude, it.longitude, (range * 1000).toInt())
                    currentLat = it.latitude
                    currentLng = it.longitude

                    setCircleByRange(currentLat, currentLng)
                }
            }
        })

        initListener()
    }

    private fun initListener(){
        initKakaoMap()
        initSpinner()
        backBtnClickEvent()
        floatingBtnClickEvent()
    }

    private fun initSpinner(){
        val rangeList = arrayListOf("50km", "30km", "20km", "10km")
        val adapter = ArrayAdapter(requireContext(),android.R.layout.simple_dropdown_item_1line, rangeList)
        binding.mapFragmentSpinnerRange.adapter = adapter

        binding.mapFragmentSpinnerRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                removePing()
                removeCampingMarker()
                visible = true
                when (position) {
                    0 -> {
                        range = 50.0
                        if(currentLat == 35.869326) {
                            showCustomToast("?????? ????????? ????????? ??? ????????????.")
                        }
                        mainViewModel.userLoc.observe(viewLifecycleOwner, {
                            if(it != null) {
                                runBlocking {
                                    mainViewModel.getPlacesByGps(it.latitude, it.longitude, range)
                                    currentLat = it.latitude
                                    currentLng = it.longitude
                                    setCircleByRange(currentLat, currentLng)
                                }
                            }
                        })

                    }
                    1 -> {
                        range = 30.0
                        if(currentLat == 35.869326) {
                            showCustomToast("?????? ????????? ????????? ??? ????????????.")
                        }
                        runBlocking {
                            mainViewModel.getPlacesByGps(currentLat, currentLng, range)
                        }
                        setCircleByRange(currentLat, currentLng)
                    }
                    2 -> {
                        range = 20.0
                        if(currentLat == 35.869326) {
                            showCustomToast("?????? ????????? ????????? ??? ????????????.")
                        }
                        runBlocking {
                            mainViewModel.getPlacesByGps(currentLat, currentLng, range)
                        }
                        setCircleByRange(currentLat, currentLng)
                    }
                    3 -> {
                        range = 10.0
                        if(currentLat == 35.869326) {
                            showCustomToast("?????? ????????? ????????? ??? ????????????.")
                        }
                        runBlocking {
                            mainViewModel.getPlacesByGps(currentLat, currentLng, range)
                        }
                        setCircleByRange(currentLat, currentLng)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
    }

    private fun initKakaoMap(){
        mapView = MapView(requireContext())
        if(mapView.parent != null){
            (mapView.parent as ViewGroup).removeView(mapView)
        }
        mapViewContainer = binding.mapFragmentPlaceMapView as ViewGroup
        mapViewContainer.addView(mapView)

        if (!mainActivity.checkLocationServicesStatus()) {
            mainActivity.showDialogForLocationServiceSetting()
        } else {
            mainActivity.checkRunTimePermission()
        }

        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading

//        mapView.setCalloutBalloonAdapter(CustomBalloonAdapter(layoutInflater))  // ????????? ????????? ??????
        mapView.setPOIItemEventListener(eventListener)  // ?????? ?????? ????????? ????????? ??????

        mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(currentLat, currentLng), 6, true);
        setCircleByRange(currentLat, currentLng)


        mainViewModel.placesByGps.observe(viewLifecycleOwner) {
            if (it == null || it.isEmpty()) {
                Snackbar.make(requireView(), "?????? ?????? ????????? ???????????? ????????????.", Snackbar.LENGTH_LONG).show()
            } else {
                addPing()
            }
        }
    }

    private fun addPing(){
        markerArr = arrayListOf()
        val placeList = mainViewModel.placesByGps.value

        if(placeList != null && placeList.isNotEmpty()) {
            for(item in placeList){
                val mapPoint = MapPoint.mapPointWithGeoCoord(item.lat, item.long)
                markerArr.add(mapPoint)
            }
            setPing(markerArr, placeList)
        } else {
            showCustomToast("????????? ????????? ????????? ????????????.")
        }
    }

    private fun setPing(markerArr : ArrayList<MapPoint>, placeList: MutableList<Place>) {
        removePing()
        val list = arrayListOf<MapPOIItem>()
        for(i in 0 until markerArr.size){
            val marker = MapPOIItem()
            marker.itemName = placeList[i].name
            marker.mapPoint = markerArr[i]
            marker.markerType = MapPOIItem.MarkerType.RedPin
            marker.tag = -1
            list.add(marker)
        }
        mapView.addPOIItems(list.toArray(arrayOfNulls(list.size)))
    }

    private fun setCircleByRange(curLat: Double, curLng: Double) {
        val mapPoint = MapPoint.mapPointWithGeoCoord(curLat, curLng)
        val circle1 = MapCircle(
            mapPoint,  // center
            (range * 1000).toInt(),  // radius
            Color.TRANSPARENT,  // strokeColor
            Color.argb(100, 12, 49, 122) // fillColor
        )
        circle1.tag = 1234
        mapView.addCircle(circle1)

        val circle2 = MapCircle(
            MapPoint.mapPointWithGeoCoord(curLat, curLng),
            0,  // radius
            Color.argb(128, 255, 0, 0),  // strokeColor
            Color.argb(128, 255, 255, 0) // fillColor
        )
        circle2.tag = 5678
        mapView.addCircle(circle2)

        val mapPointBoundsArray = arrayOf(circle1.bound, circle2.bound)
        val mapPointBounds = MapPointBounds(mapPointBoundsArray)
        val padding = 50 // px

        mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding))

    }

    private fun removePing() {
        mapView.removeAllPOIItems()
        mapView.removeAllCircles()
    }

    private fun backBtnClickEvent() {
        binding.mapFragmentIvBack.setOnClickListener {
            this@LocationMapFragment.findNavController().popBackStack()
        }
    }


    /**
     * floating Button ?????? ?????????(??? ?????? ????????? ??????)
     */
    private fun floatingBtnClickEvent() {
        binding.mapFragmentFabGetCamping.setOnClickListener {
            if(visible) {
                Log.d(TAG, "floatingBtnClickEvent: ${range * 1000}")
                runBlocking {
                    mainViewModel.getCampingList(currentLat, currentLng, (range * 1000).toInt())
                }
                setCampingPlaceMarker()
                visible = false
            } else {
                removeCampingMarker()
                visible = true
            }
        }
    }

    /**
     * ?????? ????????? ?????? ??????
     */
    private fun setCampingPlaceMarker() {
        removeCampingMarker()

        val markerList = arrayListOf<MapPoint>()
        val campingList = mainViewModel.campingList.value
        if(campingList != null && campingList.isNotEmpty()) {
        Log.d(TAG, "setCampingPlaceMarker: ${campingList.size}")
            for(item in campingList){
                val mapPoint = MapPoint.mapPointWithGeoCoord(item.mapY, item.mapX)
                markerList.add(mapPoint)
            }

            for(i in 0 until markerList.size){
                val marker = MapPOIItem()
                marker.itemName = campingList[i].facltNm
                marker.mapPoint = markerList[i]
                marker.markerType = MapPOIItem.MarkerType.BluePin
                marker.tag = i
                campingPOIList.add(marker)
            }
            mapView.addPOIItems(campingPOIList.toArray(arrayOfNulls(campingPOIList.size)))
            Log.d(TAG, "setCampingPlaceMarker: ${campingPOIList.size} / ${markerList.size} / ${campingList.size}")
        } else {
            showCustomToast("????????? ????????? ?????? ????????? ????????????.")
        }
    }

    /**
     * ????????? ?????? ?????? ??????
     */
    private fun removeCampingMarker() {
        if(campingPOIList.isNotEmpty()) {
            mapView.removePOIItems(campingPOIList.toArray(arrayOfNulls(campingPOIList.size)))
            campingPOIList = arrayListOf()
        }
    }



//    ????????? ????????? ???????????? ???????????? ??? ??????.
    override fun onCurrentLocationUpdate(p0: MapView, p1: MapPoint, p2: Float) {
//        removePing()
//        visible = true

        val mapPointGeo: GeoCoordinate = p1.mapPointGeoCoord
        currentLat = mapPointGeo.latitude
        currentLng = mapPointGeo.longitude
        runBlocking {
            mainViewModel.getPlacesByGps(mapPointGeo.latitude, mapPointGeo.longitude, range)
        }
        currentMapPoint = MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude)

        //??? ????????? ?????? ?????? ??????
        p0.setMapCenterPoint(currentMapPoint, true)
        p0.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(currentLat, currentLng), 6, true);

        setCircleByRange(currentLat, currentLng)
    }

//    ????????? ??????(Heading) ???????????? ???????????? ??? ??????.
//    MapView.setCurrentLocationTrackingMode ???????????? ??????
//    ????????? ????????? ???????????? ????????? ????????? ?????? ??????(CurrentLocationTrackingMode.TrackingModeOnWithHeading)
//    ????????? ?????? ???????????? ??????????????? delegate ????????? ????????????.
    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) {

    }

    // ????????? ?????? ????????? ????????? ?????? ????????????.
    override fun onCurrentLocationUpdateFailed(p0: MapView?) {
        Log.e(TAG, "onCurrentLocationUpdateFailed: ")
    }

    // ????????? ????????? ????????? ???????????? ?????? ????????? ?????? ????????????.
    // ?????? ???????????? ?????? ????????? ???????????? ?????? ???????????? Alert Dialog ?????????????????? ??????????????? ????????????.
    // ??? ???????????? ???????????? ???????????? ?????? ????????? ?????? ?????? ?????? ??????.
    override fun onCurrentLocationUpdateCancelled(p0: MapView?) {
        showCustomToast("?????? ?????? ????????? ????????? ?????????????????????.")
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
        mapViewContainer.removeAllViews()
        mainActivity.hideBottomNav(false)
    }






    // ????????? ????????? ?????????
    inner class CustomBalloonAdapter(inflater: LayoutInflater): CalloutBalloonAdapter {
        val mCalloutBalloon: View = inflater.inflate(R.layout.map_balloon_layout, null)
        val name: TextView = mCalloutBalloon.findViewById(R.id.ball_tvName)
        val address: TextView = mCalloutBalloon.findViewById(R.id.ball_tvAddress)

        override fun getCalloutBalloon(poiItem: MapPOIItem?): View {
            // ?????? ?????? ??? ????????? ?????????
            name.text = poiItem?.itemName
//            address.text = poiItem.
            return mCalloutBalloon
        }

        override fun getPressedCalloutBalloon(poiItem: MapPOIItem?): View {
            // ????????? ?????? ???
//            address.text = "getPressedCalloutBalloon"
            return mCalloutBalloon
        }
    }


    // ?????? ?????? ????????? ?????????
    inner class MarkerEventListener(val context: Context, val viewModel: MainViewModel): MapView.POIItemEventListener {
        private val TAG = "LocationMapFragment"
        override fun onPOIItemSelected(mapView: MapView?, poiItem: MapPOIItem?) {
            // ?????? ?????? ???
        }

        override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?) {
            // ????????? ?????? ??? (Deprecated)
            // ??? ????????? ??????????????? ?????? ?????? ?????? ????????? ????????????
        }

        override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?, buttonType: MapPOIItem.CalloutBalloonButtonType?) {

            if(poiItem!!.tag != -1) {

                val campingList = viewModel.campingList.value

                // ?????? ????????? ????????????
                val idx = poiItem.tag
                val item = campingList!![idx]

                // ????????? ?????? ???
                val dialog = Dialog(context)
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_place_info,null)
                dialog.setContentView(dialogView)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                dialog.show()


                val phone = dialogView.findViewById<ImageView>(R.id.placeInfoDialog_ivPlacePhone)
                val homePage = dialogView.findViewById<ImageView>(R.id.placeInfoDialog_ivPlaceHomePage)

                // phone ???????????? ????????? ?????? ??????
                phone.setOnClickListener {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.tel}")))
                }
                // homepage ???????????? ????????? ??????
                homePage.setOnClickListener {
                    var webpage = Uri.parse(item.homepage)

                    if (!item.homepage.startsWith("http://") && !item.homepage.startsWith("https://")) {
                        webpage = Uri.parse("http://${item.homepage}")
                    }

                    val intent = Intent(Intent.ACTION_VIEW, webpage)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
//                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.homepage)))
                }

                // ?????????
                val campingImg = dialogView.findViewById<ImageView>(R.id.placeInfoDialog_ivPlaceImg)
                if(item.firstImageUrl == "null" || item.firstImageUrl == null || item.firstImageUrl.length == 0 ) {
                    campingImg.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    Glide.with(dialogView)
                        .load(R.drawable.ic_tent_color_512px)
                        .into(campingImg)

                } else {
//                    campingImg.visibility = View.VISIBLE
                    campingImg.scaleType = ImageView.ScaleType.FIT_XY

                    Glide.with(dialogView)
                        .load(item.firstImageUrl)
                        .into(campingImg)
                }

                dialogView.findViewById<TextView>(R.id.placeInfoDialog_tvPlaceInduty).text = "[${item.induty}]"   // ??????
                dialogView.findViewById<TextView>(R.id.placeInfoDialog_tvPlaceName).text = item.facltNm   // ?????? ??????
                dialogView.findViewById<TextView>(R.id.placeInfoDialog_tvPlaceAddr).text = item.addr1    // ??????
                dialogView.findViewById<TextView>(R.id.placeInfoDialog_tvPlaceAddrDetail).text = item.addr2 // ?????? ??????
                dialogView.findViewById<TextView>(R.id.placeInfoDialog_tvPlaceLineIntro).text = item.lineIntro // ??? ??? ??????

                dialogView.findViewById<AppCompatButton>(R.id.placeInfoDialog_btnClose).setOnClickListener {
                    dialog.dismiss()
                }
            }

        }

        override fun onDraggablePOIItemMoved(mapView: MapView?, poiItem: MapPOIItem?, mapPoint: MapPoint?) {
            // ????????? ?????? ??? isDraggable = true ??? ??? ????????? ??????????????? ??????
        }
    }
}