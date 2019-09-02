package `in`.avimarine.seawatercurrentmeasure

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var countUpTimer: CountUpTimer
    private lateinit var tv_last_acc: TextView
    private lateinit var button: Button
    private lateinit var tv_speed: TextView
    private lateinit var tv_time2: TextView
    private lateinit var tv_lon2: TextView
    private lateinit var tv_lat2: TextView
    private lateinit var tv_time1: TextView
    private lateinit var tv_lon1: TextView
    private lateinit var tv_lat1: TextView
    private lateinit var tv_last_time: TextView
    private lateinit var tv_last_lon: TextView
    private lateinit var tv_dir: TextView
    private lateinit var tv_last_lat: TextView
    private var lastLocationTime: Long = 0
    private var lastLocation: Location? = null
    private var firstTime: Long = 0
    private var secondTime: Long = 0
    private lateinit var firstLocation: Location
    private lateinit var secondLocation: Location
    private val MY_PERMISSIONS_REQUEST: Int = 12345
    private var locationRequest: LocationRequest? = null
    private val UPDATE_INTERVAL: Long = 1000
    private val FASTEST_INTERVAL: Long = 1000 // = 5 seconds


    private lateinit var locationCallback: LocationCallback
    val Any.TAG: String
        get() {
            return javaClass.simpleName
        }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tv_last_lat = findViewById(R.id.text_last_lat) as TextView
        tv_last_lon = findViewById(R.id.text_last_lon) as TextView
        tv_last_time = findViewById(R.id.text_last_time) as TextView
        tv_last_acc = findViewById(R.id.text_last_acc) as TextView
        tv_lat1 = findViewById(R.id.text_lat1) as TextView
        tv_lon1 = findViewById(R.id.text_lon1) as TextView
        tv_time1 = findViewById(R.id.text_time1) as TextView
        tv_lat2 = findViewById(R.id.text_lat2) as TextView
        tv_lon2 = findViewById(R.id.text_lon2) as TextView
        tv_time2 = findViewById(R.id.text_time2) as TextView
        tv_speed = findViewById(R.id.text_speed) as TextView
        tv_dir = findViewById(R.id.text_dir) as TextView
        button = findViewById(R.id.start_btn) as Button

        if (!isLocationPermissionGranted())
            askForPermissions()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    lastLocation = location
                    lastLocationTime = location.time
                    locationIntoTextViews(location, tv_last_lat, tv_last_lon, tv_last_time, tv_last_acc)
                }
            }
        }
        startLocationUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()
        if (id == R.id.settings_menu_action) {
            val intent = Intent(this, MySettingsActivity::class.java)
            this.startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun locationIntoTextViews(
        loc: Location,
        lat_tv: TextView,
        lon_tv: TextView,
        time_tv: TextView,
        acc_tv: TextView? = null,
        empty: Boolean = false
    ) {
        if (empty) {
            lat_tv.text = "?"
            lon_tv.text = "?"
            time_tv.text = "?"
            if (acc_tv != null)
                acc_tv.text = "?"
        } else {
            lat_tv.text = String.format("%.6f", loc.latitude)
            lon_tv.text = String.format("%.6f", loc.longitude)
            time_tv.text = timeStamptoDateString(loc.time)
            if (acc_tv != null)
                acc_tv.text = String.format("%.1f m", loc.accuracy)
        }
    }

    private fun askForPermissions() {
        if (!isLocationPermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user'getDirString response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST
                )
            }
        }
    }

    private fun isLocationPermissionGranted() =
        (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_VOLUME_DOWN) {
            startButtonClick(start_btn)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @SuppressLint("MissingPermission")
    fun startButtonClick(view: View) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        val magnetic = sharedPreferences.getBoolean("magnetic", false)
        val fromNotation = sharedPreferences.getBoolean("from_notation", false)
        val speedUnit = sharedPreferences.getString("speed_unit", "m_per_min")
        if (!isLocationPermissionGranted())
            askForPermissions()
        else {
            fusedLocationClient.lastLocation //TODO check why I can do that (location can be null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        if (firstTime == 0L) {
                            firstLocation = location
                            firstTime = System.currentTimeMillis()
                            locationIntoTextViews(location, text_lat1, text_lon1, text_time1, null)
                            locationIntoTextViews(location, text_lat2, text_lon2, text_time2, null, true)
                            tv_speed.text = "?"
                            tv_dir.text = "?"
                            countUpTimer = CountUpButtonTimer(1000).start()


                        } else {
                            secondLocation = location
                            secondTime = System.currentTimeMillis()
                            locationIntoTextViews(location, text_lat2, text_lon2, text_time2)
                            val dist = getDistance(
                                firstLocation.latitude, firstLocation.longitude, secondLocation.latitude,
                                secondLocation.longitude
                            )
                            var dir = getDirection(
                                firstLocation.latitude, firstLocation.longitude, secondLocation.latitude,
                                secondLocation.longitude
                            )
                            tv_speed.text = getSpeedString(firstTime, secondTime, dist, speedUnit)

                            tv_dir.text = getDirString(dir, magnetic, fromNotation, secondLocation, secondTime)
                            firstTime = 0
                            secondTime = 0
                            countUpTimer.stop()
                            formatButton(ButtonState.STOPPED,0)
                        }
                    }
                }
        }
    }

    private fun formatButton(buttonState: ButtonState, time: Long) {
        if (buttonState == ButtonState.RUNNING){
            button.setBackgroundResource(R.drawable.btn_rnd_red)
            button.setText("Stop\n" + getTimerString(time))
        }
        else if (buttonState == ButtonState.STOPPED){
            button.setBackgroundResource(R.drawable.btn_rnd_grn)
            button.setText("Start")
        }

    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    val alertDialog = AlertDialog.Builder(this@MainActivity).create()
                    alertDialog.setTitle("Permissions")
                    alertDialog.setMessage("Location permission are required for this app to work.")
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        { dialog, which -> this.finish() })
                    alertDialog.show()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        locationRequest = LocationRequest()
        locationRequest!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest!!.setInterval(UPDATE_INTERVAL)
        locationRequest!!.setFastestInterval(FASTEST_INTERVAL)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show()
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }
    inner class CountUpButtonTimer(interval: Long): CountUpTimer(interval) {
        override fun onTick(elapsedTime: Long) {
            formatButton(ButtonState.RUNNING, System.currentTimeMillis() - firstTime);
        }
    }

}



