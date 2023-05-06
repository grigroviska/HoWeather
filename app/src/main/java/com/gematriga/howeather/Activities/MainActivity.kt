package com.gematriga.howeather.Activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.gematriga.howeather.Models.WeatherModel
import com.gematriga.howeather.R
import com.gematriga.howeather.Utilities.ApiUtilities
import com.gematriga.howeather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var currentLocation : Location
    private lateinit var fusedLocationProvider : FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 101
    private val apiKey= "f4ff72e3cb789658ce71efa591b539c6"
    var cbName : String? = null
    var cCity : String? = null
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        cbName = binding.citySearch.text.toString()

        //If the user is logging in for the first time, the app will show Istanbul by default
        sharedPreferences = this.getSharedPreferences("com.gematriga.howeather", Context.MODE_PRIVATE)

        cCity = sharedPreferences.getString("city", "")

        if (cCity == ""){

            getCityWeather("istanbul")

            showDialog()

        }else{

            fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
            getCurrentLocation()

        }



        binding.citySearch.setOnEditorActionListener { textView, i, keyEvent ->
            if (i== EditorInfo.IME_ACTION_SEARCH){

                getCityWeather(binding.citySearch.text.toString())

                val view = this.currentFocus

                if (view != null){

                    val imm : InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

                    imm.hideSoftInputFromWindow(view.windowToken,0)

                    binding.citySearch.clearFocus()

                }
                return@setOnEditorActionListener true
            }
            else{

                return@setOnEditorActionListener false

            }
        }
        binding.currentLocation.setOnClickListener {

            getCurrentLocation()

        }
        //The ability to share the screen the user sees with other applications
        binding.shareHoweather.setOnClickListener {

            captureScreenShot()

        }

    }
    //Requesting a location permit and taking action accordingly
    private fun showDialog(){

        val builder : AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Location")
        builder.setMessage("Your location is required")
        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            getCurrentLocation()
        }

        builder.setNegativeButton(android.R.string.no) { dialog, which ->
            Toast.makeText(this@MainActivity, "Your location was necessary, but if you don't trust it, you can call it yourself.",Toast.LENGTH_LONG).show()
        }
        builder.show()

    }

    private fun getCityWeather(city: String){

        binding.progressBar.visibility = View.VISIBLE

        ApiUtilities.getApiInterface()?.getCityWeatherData(city,apiKey)?.enqueue(
            object : Callback<WeatherModel>{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {

                    if(response.isSuccessful){

                        binding.progressBar.visibility = View.GONE

                        response.body()?.let {

                            setData(it)

                        }

                    }else{

                        Toast.makeText(this@MainActivity,"No City Found", Toast.LENGTH_LONG).show()
                        binding.progressBar.visibility = View.GONE

                    }

                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {



                }

            }
        )
    }

    private fun fetchCurrentLocationWeather(latitude : String, longitude : String){

        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude,longitude, apiKey)
            ?.enqueue(object : Callback<WeatherModel>{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {

                    if (response.isSuccessful){

                        binding.progressBar.visibility = View.GONE

                        response.body()?.let {

                            setData(it)

                        }

                    }

                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {



                }
            })

    }

    private fun getCurrentLocation(){

        if (checkPermissions()){

            if (isLocationEnabled()){

                if (ActivityCompat.checkSelfPermission(

                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION

                    )!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )!= PackageManager.PERMISSION_GRANTED
                ){

                    requestPermissions()

                    return

                }

                fusedLocationProvider.lastLocation.addOnSuccessListener {

                        location ->

                    if (location!= null){

                        currentLocation = location

                        binding.progressBar.visibility = View.VISIBLE
                        fetchCurrentLocationWeather(

                            location.latitude.toString(),
                            location.longitude.toString()


                        )


                    }


                }

            }

            else{

                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

                startActivity(intent)

            }

        }

        else{

            requestPermissions()

        }

    }
    //Requesting a location permit and taking action accordingly
    private fun requestPermissions(){

        ActivityCompat.requestPermissions(

            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE

        )

    }

    private fun isLocationEnabled() : Boolean{

        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                ||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }

    private fun checkPermissions(): Boolean{

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(

                this,
                Manifest.permission.ACCESS_FINE_LOCATION

            )== PackageManager.PERMISSION_GRANTED){

            return true

        }

        return false

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode==LOCATION_REQUEST_CODE){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                getCurrentLocation()

            }else{



            }

        }

    }
    //Change the data pulled from the API to the desired values
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setData(body: WeatherModel){

        binding.apply {

            val currentDate = SimpleDateFormat("dd/MM/yyyy hh:mm").format(Date())

            dateTime.text = currentDate.toString()

            maxTemp.text = "Max " + k2c(body?.main?.temp_max!!) + "°"

            minTemp.text = "Min " + k2c(body?.main?.temp_min!!) + "°"

            temp.text = "" + k2c(body?.main?.temp!!) + "°"

            weatherTitle.text = body.weather[0].main

            sunriseValue.text = ts2td(body.sys.sunrise.toLong())

            sunsetValue.text = ts2td(body.sys.sunset.toLong())

            pressureValue.text = body.main.pressure.toString()

            humidityValue.text = body.main.humidity.toString()+ "%"

            tempFValue.text = "" + k2c(body.main.temp).times(1.8).plus(32)
                .roundToInt() + ""

            citySearch.setText(body.name)

            cbName = citySearch.text.toString()

            feelsLike.text = "" + k2c(body?.main?.feels_like!!) + "°"

            windValue.text = body.wind.speed.toString() + "m/s"

            groundValue.text = body.main.grnd_level.toString()

            seaValue.text = body.main.sea_level.toString()

            countryValue.text = body.sys.country.toString()

        }

        updateUI(body.weather[0].id)

    }


    private fun captureScreenShot() {

        try{

            val now = Date()
            android.text.format.DateFormat.format("dd/MM/YYYY hh:mm", now)

            val path = getExternalFilesDir(null)!!.absolutePath + "/" + ".jpg"

            val bitmap = Bitmap.createBitmap(
                binding.mainLayout.width,
                binding.mainLayout.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            binding.mainLayout.draw(canvas)
            val imageFile = File(path)
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            val URI =
                FileProvider.getUriForFile(applicationContext, "com.gematriga.howeather.provider", imageFile)

            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, "Weather in $cbName" + "\n" + "($now)" + "\n"  + "HoWeather")
            intent.putExtra(Intent.EXTRA_STREAM, URI)
            intent.type = "text/plain"
            startActivity(intent)
        }catch (e: Exception){

            Toast.makeText(this@MainActivity,e.localizedMessage,Toast.LENGTH_LONG).show()

        }


    }
    //Changing the background according to the air rating
    private fun updateUI(id: Int) {

        binding.apply {

            when(id){

                in 200..232->{

                    weatherImg.setImageResource(R.drawable.ic_storm_weather)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)


                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.thunderstrom_bg)

                }

                in 300..321->{

                    weatherImg.setImageResource(R.drawable.ic_few_clouds)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.drizzle_bg)


                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.drizzle_bg)

                }

                in 500..531->{

                    weatherImg.setImageResource(R.drawable.ic_rainy_weather)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.rain_bg)


                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.rain_bg)

                }

                in 600..622->{

                    weatherImg.setImageResource(R.drawable.ic_snow_weather)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.snow_bg)


                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.snow_bg)

                }

                in 701..781->{

                    weatherImg.setImageResource(R.drawable.ic_broken_clouds)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.atmosphere_bg)


                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.atmosphere_bg)

                }

                800->{

                    weatherImg.setImageResource(R.drawable.ic_clear_day)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clear_bg)


                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clear_bg)

                }

                in 801..804->{

                    weatherImg.setImageResource(R.drawable.ic_cloudy_weather)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clouds_bg)


                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clouds_bg)

                }

                else->{

                    weatherImg.setImageResource(R.drawable.ic_unknown)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.unknown_bg)


                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.unknown_bg)

                }

            }

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ts2td(ts: Long): String {

        val localTime = ts.let {

            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()

        }

        return localTime.toString()

    }

    private fun k2c(t: Double): Double {

        var intTemp = t

        intTemp= intTemp.minus(273)

        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()

    }


}