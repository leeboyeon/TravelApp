package com.whitebear.travel.src.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.gms.location.*
import com.whitebear.travel.R
import com.whitebear.travel.config.ApplicationClass
import com.whitebear.travel.config.BaseActivity
import com.whitebear.travel.databinding.ActivityMainBinding
import com.whitebear.travel.src.network.viewmodel.MainViewModel
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import android.location.LocationManager
import android.widget.Toast
import android.content.DialogInterface

import androidx.core.app.ActivityCompat.startActivityForResult

import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.whitebear.travel.src.dto.Noti
import com.whitebear.travel.src.network.api.FCMApi
import com.whitebear.travel.src.network.service.UserService
import com.whitebear.travel.util.NavDB
import com.whitebear.travel.util.NotiDB
import retrofit2.Response
import kotlin.collections.HashMap


private const val TAG = "MainActivity"
class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate){
    lateinit var mainViewModel : MainViewModel

    // νμ¬ μμΉ locationManager
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null // νμ¬ μμΉλ₯Ό κ°μ Έμ€κΈ° μν λ³μ
    lateinit var mLastLocation: Location // μμΉ κ°μ κ°μ§κ³  μλ κ°μ²΄
    private lateinit var mLocationRequest: LocationRequest // μμΉ μ λ³΄ μμ²­μ λ§€κ°λ³μλ₯Ό μ μ₯νλ

    // μμΉ κΆν
    private val LOCATION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private val LOCATION_CODE = 100
    private var today = ""
    var hour = ""
    var addr = ""
    private var today2Type = ""
    private val GPS_ENABLE_REQUEST_CODE = 2001
    //Room DB
    var notiDB : NotiDB ?= null
    var navDB: NavDB?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //dbμ μ₯
        notiDB = NotiDB.getInstance(this)
        navDB = NavDB.getInstance(this)

        var notiDao = notiDB?.fcmDao()
        val r = java.lang.Runnable {
            if(notiDao?.getFcmCheck(ApplicationClass.sharedPreferencesUtil.getUser().id) == null){
                notiDao?.insertChecked(Noti(ApplicationClass.sharedPreferencesUtil.getUser().id,true,true))
            }
        }
        val thread = Thread(r)
        thread.start()
        
        initNavigation()
        setInstance()
        if(checkPermissionForLocation(this)) {
            startLocationUpdates()
        }
        initFcm()
    }
    private fun setInstance(){
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
//        getMeasure()

    }

    private fun initNavigation(){
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.activity_main_navHost) as NavHostFragment

        // λ€λΉκ²μ΄μ μ»¨νΈλ‘€λ¬
        val navController = navHostFragment.navController

        // λ°μΈλ©
        NavigationUI.setupWithNavController(binding.activityMainBottomNav, navController)
    }



    /**
     * bottom Nav hide & show
     * hide - true
     * show - false
     */
    fun hideBottomNav(state: Boolean) {
        if(state) {
            binding.activityMainBottomNav.visibility = View.GONE
        } else {
            binding.activityMainBottomNav.visibility = View.VISIBLE
        }
    }

    /**
     * μμΉ κΆν
     */
    fun checkPermissionForLocation(context: Context): Boolean {
        // Android 6.0 Marshmallow μ΄μμμλ μμΉ κΆνμ μΆκ° λ°νμ κΆνμ΄ νμ
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // κΆνμ΄ μμΌλ―λ‘ κΆν μμ²­ μλ¦Ό λ³΄λ΄κΈ°
                ActivityCompat.requestPermissions(this, LOCATION, LOCATION_CODE)
                false
            }
        } else {
            true
        }
    }

    fun checkPermission(permissions: Array<out String>, type: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, type)
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {

            LOCATION_CODE -> {
                for(grant in grantResults) {
                    if(grant != PackageManager.PERMISSION_GRANTED) {
                        showCustomToast("μμΉ κΆνμ μΉμΈν΄ μ£ΌμΈμ.")
                        Log.d(TAG, "onRequestPermissionsResult: ")
                    } else if(grant == PackageManager.PERMISSION_GRANTED) {
                        startLocationUpdates()
                    }
                }
            }
        }
    }

    fun startLocationUpdates() {
        mLocationRequest =  LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        //FusedLocationProviderClientμ μΈμ€ν΄μ€λ₯Ό μμ±.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        // κΈ°κΈ°μ μμΉμ κ΄ν μ κΈ° μλ°μ΄νΈλ₯Ό μμ²­νλ λ©μλ μ€ν
        // μ§μ ν λ£¨νΌ μ€λ λ(Looper.myLooper())μμ μ½λ°±(mLocationCallback)μΌλ‘ μμΉ μλ°μ΄νΈλ₯Ό μμ²­
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper()!!)

    }

    // μμ€νμΌλ‘ λΆν° μμΉ μ λ³΄λ₯Ό μ½λ°±μΌλ‘ λ°μ
    private val mLocationCallback = object : LocationCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
            Log.d(TAG, "onLocationResult: ")
            // μμ€νμμ λ°μ location μ λ³΄λ₯Ό onLocationChanged()μ μ λ¬
        }
    }

    // μμ€νμΌλ‘ λΆν° λ°μ μμΉμ λ³΄λ₯Ό νλ©΄μ κ°±μ ν΄μ£Όλ λ©μλ
    fun onLocationChanged(location: Location) {
        mLastLocation = location
        mainViewModel.setUserLoc(location, getAddress(location))
        Log.d(TAG, "onLocationChanged: ${location.latitude} / ${location.longitude}")
        getToday()
        //lat=35.8988, long=128.599
        runBlocking {
            mainViewModel.getWeather("JSON",10,1,today.toInt(),hour,"${location.latitude.toInt()}","${location.longitude.toInt()}")
        }
    }

    fun getAddress(position: Location) : String {
        val geoCoder = Geocoder(this, Locale.getDefault())
        val address = geoCoder.getFromLocation(position.latitude, position.longitude, 1).first()
            .getAddressLine(0)
        addr = address
        Log.d(TAG, "Address, $address")
        return address
    }

    fun getToday() : String {
        var current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formattering = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val hourFormatt = DateTimeFormatter.ofPattern("HH")
        val formatted = current.format(formatter)
        val formatted2 = current.format(formattering)
        val formatted3 = current.format(hourFormatt).toInt()
        today = formatted
        today2Type = formatted2
        if(formatted3.toInt() < 2){
            today = formatter.format(current.minusDays(1))
            hour = "2300"
        }else if(formatted3 < 5){
            hour = "0200"
        }else if(formatted3 < 8){
            hour = "0500"
        }else if(formatted3 < 11){
            hour = "0800"
        }else if(formatted3 < 14){
            hour = "1100"
        }else if(formatted3 < 17){
            hour = "1400"
        }else if(formatted3 < 20){
            hour = "1700"
        }else if(formatted3 < 23){
            hour = "2000"
        }else{
            hour = "2300"
        }
        mainViewModel.setHour(hour)
        mainViewModel.setToday(today.toInt())
        return today
    }

    fun checkRunTimePermission() {
        //λ°νμ νΌλ―Έμ μ²λ¦¬
        // 1. μμΉ νΌλ―Έμμ κ°μ§κ³  μλμ§ μ²΄ν¬ν©λλ€.
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. μ΄λ―Έ νΌλ―Έμμ κ°μ§κ³  μλ€λ©΄
            // ( μλλ‘μ΄λ 6.0 μ΄ν λ²μ μ λ°νμ νΌλ―Έμμ΄ νμμκΈ° λλ¬Έμ μ΄λ―Έ νμ©λ κ±Έλ‘ μΈμν©λλ€.)
            // 3.  μμΉ κ°μ κ°μ Έμ¬ μ μμ
        } else {  //2. νΌλ―Έμ μμ²­μ νμ©ν μ μ΄ μλ€λ©΄ νΌλ―Έμ μμ²­μ΄ νμν©λλ€. 2κ°μ§ κ²½μ°(3-1, 4-1)κ° μμ΅λλ€.
            // 3-1. μ¬μ©μκ° νΌλ―Έμ κ±°λΆλ₯Ό ν μ μ΄ μλ κ²½μ°μλ
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    LOCATION[0]
                )
            ) {
                // 3-2. μμ²­μ μ§ννκΈ° μ μ μ¬μ©μκ°μκ² νΌλ―Έμμ΄ νμν μ΄μ λ₯Ό μ€λͺν΄μ€ νμκ° μμ΅λλ€.
                Toast.makeText(this@MainActivity, "μ΄ μ±μ μ€ννλ €λ©΄ μμΉ μ κ·Ό κΆνμ΄ νμν©λλ€.", Toast.LENGTH_LONG)
                    .show()
                // 3-3. μ¬μ©μκ²μ νΌλ―Έμ μμ²­μ ν©λλ€. μμ²­ κ²°κ³Όλ onRequestPermissionResultμμ μμ λ©λλ€.
                ActivityCompat.requestPermissions(
                    this@MainActivity, LOCATION,
                    LOCATION_CODE
                )
            } else {
                // 4-1. μ¬μ©μκ° νΌλ―Έμ κ±°λΆλ₯Ό ν μ μ΄ μλ κ²½μ°μλ νΌλ―Έμ μμ²­μ λ°λ‘ ν©λλ€.
                // μμ²­ κ²°κ³Όλ onRequestPermissionResultμμ μμ λ©λλ€.
                ActivityCompat.requestPermissions(
                    this@MainActivity, LOCATION,
                    LOCATION_CODE
                )
            }
        }
    }

    //μ¬κΈ°λΆν°λ GPS νμ±νλ₯Ό μν λ©μλλ€
    fun showDialogForLocationServiceSetting() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("μμΉ μλΉμ€ λΉνμ±ν")
        builder.setMessage(
            """
            μ±μ μ¬μ©νκΈ° μν΄μλ μμΉ μλΉμ€κ° νμν©λλ€.
            μμΉ μ€μ μ μμ νμκ² μ΅λκΉ?
            """.trimIndent()
        )
        builder.setCancelable(true)
        builder.setPositiveButton("μ€μ ", DialogInterface.OnClickListener { dialog, id ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE)
        })
        builder.setNegativeButton("μ·¨μ",
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
        builder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GPS_ENABLE_REQUEST_CODE ->                 //μ¬μ©μκ° GPS νμ± μμΌ°λμ§ κ²μ¬
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS νμ±ν λμμ")
                        checkRunTimePermission()
                        return
                    }
                }
        }
    }

    fun checkLocationServicesStatus(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    /**
     * FCM ν ν° μμ  λ° μ±λ μμ±
     */
    private fun initFcm() {
        // FCM ν ν° μμ 
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "FCM ν ν° μ»κΈ°μ μ€ν¨νμμ΅λλ€.", task.exception)
                return@OnCompleteListener
            }
            // token log λ¨κΈ°κΈ°
            Log.d(TAG, "token: ${task.result?:"task.result is null"}")
            uploadToken(task.result!!, ApplicationClass.sharedPreferencesUtil.getUser().id)
        })

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channel_id, "whitebear")
        }
    }

    /**
     * Fcm Notification μμ μ μν μ±λ μΆκ°
     */
    private fun createNotificationChannel(id: String, name: String) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT // or IMPORTANCE_HIGH
        val channel = NotificationChannel(id, name, importance)

        val notificationManager: NotificationManager
                = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val channel_id = "whitebear_channel"
        fun uploadToken(token:String, userId: Int) {

            var response : Response<HashMap<String, Any>>
            runBlocking {
                response = UserService().updateUserToken(ApplicationClass.sharedPreferencesUtil.getUser().id, token)
            }
            if(response.code() == 200) {
                val res = response.body()
                if(res != null) {
                    if(res["isSuccess"] == true) {
                        Log.d(TAG, "uploadToken: $token")
                    } else {
                        Log.d(TAG, "uploadToken: ${res["message"]}")
                    }
                }
            } else {
                Log.e(TAG, "uploadToken: ν ν° μ λ³΄ λ±λ‘ μ€ ν΅μ  μ€λ₯")
            }
        }
    }
}