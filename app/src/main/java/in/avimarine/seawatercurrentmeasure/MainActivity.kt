package `in`.avimarine.seawatercurrentmeasure

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
    private lateinit var countDownTimer: CountDownTimer
    private var lastLocationTime: Long = 0
    private var lastLocation: Location? = null
    private var firstTime: Long = 0
    private var secondTime: Long = 0
    private var delayedStartTime: Long = 0
    private lateinit var firstLocation: Location
    private lateinit var secondLocation: Location
    private val MY_PERMISSIONS_REQUEST: Int = 12345
    private var locationRequest: LocationRequest? = null
    private val UPDATE_INTERVAL: Long = 1000
    private val FASTEST_INTERVAL: Long = 1000 // = 5 seconds
    private var measurementState: MeasurementState = MeasurementState.STOPPED
    private var delayedStartInterval: Long = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    val Any.TAG: String
        get() {
            return javaClass.simpleName
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationPermissionGranted())
            askForPermissions()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    lastLocation = location
                    lastLocationTime = location.time
                    locationIntoTextViews(
                        location,
                        text_last_lat,
                        text_last_lon,
                        text_last_time,
                        text_last_acc
                    )
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
        val (magnetic, fromNotation, speedUnit) = getPreferences()
        if (!isLocationPermissionGranted())
            askForPermissions()
        else {
            if (delayedStartInterval > 0 && firstTime == 0L) {
                delayedStartTime = System.currentTimeMillis()
                measurementState = MeasurementState.DELAYED_START
                if (::countDownTimer.isInitialized) {
                    countDownTimer.cancel();
                }
                countDownTimer = CountDownButtonTimer(delayedStartInterval, 1000).start()
                return;
            }
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        if (firstTime == 0L) {
                            startMeasurement(location)
                        } else {
                            endMeasurement(location, speedUnit, magnetic, fromNotation)
                        }
                    }
                }
        }
    }

    private fun endMeasurement(
        location: Location,
        speedUnit: String,
        magnetic: Boolean,
        fromNotation: Boolean
    ) {
        secondLocation = location
        secondTime = System.currentTimeMillis()
        locationIntoTextViews(location, text_lat2, text_lon2, text_time2)
        val dist = getDistance(firstLocation, secondLocation)
        var dir = getDirection(firstLocation,secondLocation)
        text_speed.text = getSpeedString(firstTime, secondTime, dist, speedUnit)

        text_dir.text = getDirString(
            dir,
            magnetic,
            fromNotation,
            secondLocation,
            secondTime
        )
        firstTime = 0
        secondTime = 0
        countUpTimer.stop()
        formatButton(MeasurementState.STOPPED, 0)
    }

    private fun getPreferences(): Triple<Boolean, Boolean, String> {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        var magnetic = false
        var fromNotation = false
        var speedUnit = "m_per_min"
        if (sharedPreferences != null) {
            magnetic = sharedPreferences.getBoolean("magnetic", false)
            fromNotation = sharedPreferences.getBoolean("from_notation", false)
            speedUnit = sharedPreferences.getString("speed_unit", "m_per_min")
            delayedStartInterval = sharedPreferences.getString("delayed_start", "0").toLong() * 1000
        }
        return Triple(magnetic, fromNotation, speedUnit)
    }

    private fun startMeasurement(location: Location) {
        firstLocation = location
        firstTime = System.currentTimeMillis()
        measurementState = MeasurementState.RUNNING
        locationIntoTextViews(location, text_lat1, text_lon1, text_time1, null)
        locationIntoTextViews(
            location,
            text_lat2,
            text_lon2,
            text_time2,
            null,
            true
        )
        text_speed.text = "?"
        text_dir.text = "?"
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel();
        }
        if (::countUpTimer.isInitialized) {
            countUpTimer.stop()
        }
        countUpTimer = CountUpButtonTimer(1000).start()
    }

    private fun startMeasurement() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    startMeasurement(location)
                }
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
            Toast.makeText(
                this,
                "You need to enable permissions to display location !",
                Toast.LENGTH_SHORT
            ).show()
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun formatButton(measurementState: MeasurementState, time: Long) {
        if (measurementState == MeasurementState.RUNNING) {
            start_btn.setBackgroundResource(R.drawable.btn_rnd_red)
            start_btn.setText("Stop\n" + getTimerString(time))
        } else if (measurementState == MeasurementState.STOPPED) {
            start_btn.setBackgroundResource(R.drawable.btn_rnd_grn)
            start_btn.setText("Start")
        } else if (measurementState == MeasurementState.DELAYED_START) {
            start_btn.setBackgroundResource(R.drawable.btn_rnd_ylw)
            start_btn.setText("Start in\n" + getTimerString(time))
        }

    }

    inner class CountUpButtonTimer(interval: Long) : CountUpTimer(interval) {
        override fun onTick(elapsedTime: Long) {
            formatButton(MeasurementState.RUNNING, System.currentTimeMillis() - firstTime);
        }
    }

    inner class CountDownButtonTimer(countDownInterval: Long, interval: Long) :
        CountDownTimer(countDownInterval, interval) {
        override fun onFinish() {
            startMeasurement()
        }

        override fun onTick(elapsedTime: Long) {
            formatButton(
                measurementState,
                delayedStartTime + delayedStartInterval - System.currentTimeMillis()
            );
        }
    }


}



