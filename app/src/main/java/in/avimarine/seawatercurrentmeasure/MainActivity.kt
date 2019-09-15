package `in`.avimarine.seawatercurrentmeasure

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
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
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Context.BIND_AUTO_CREATE
import `in`.avimarine.seawatercurrentmeasure.LocationUpdatesService
import android.content.Intent
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class MainActivity : AppCompatActivity() {

    private var inMeasurement = false
    private var backgroundLocationPermissions: Boolean = false
    private var locationUpdates: Boolean = false
    private var appVisibility: Boolean = true
    private lateinit var countUpTimer: CountUpTimer
    private lateinit var countDownToStartTimer: CountDownTimer
    private lateinit var countDownToFinishTimer: CountDownTimer
    private var lastLocationTime: Long = 0
    private var lastLocation: Location? = null
    private var firstTime: Long = 0
    private var secondTime: Long = 0
    private var delayedStartTime: Long = 0
    private lateinit var firstLocation: Location
    private lateinit var secondLocation: Location
    private val FINE_LOCATION_PERMISSIONS_REQUEST_CODE: Int = 12345
    private val BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE = 34;
    private var locationRequest: LocationRequest? = null
    private val UPDATE_INTERVAL: Long = 1000
    private val FASTEST_INTERVAL: Long = 1000 // = 5 seconds
    private var measurementState: MeasurementState = MeasurementState.STOPPED
    private var delayedStartInterval: Long = 0
    private var autoFinishInterval: Long = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    val Any.TAG: String
        get() {
            return javaClass.simpleName
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initForegroundService()
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
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    FINE_LOCATION_PERMISSIONS_REQUEST_CODE
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
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            mService?.requestLocationUpdates();
        }
        if (!isLocationPermissionGranted())
            askForPermissions()
        else {
            if (delayedStartInterval > 0 && firstTime == 0L) {
                delayedStartTime = System.currentTimeMillis()
                measurementState = MeasurementState.DELAYED_START
                if (::countDownToStartTimer.isInitialized) {
                    countDownToStartTimer.cancel();
                }
                countDownToStartTimer = CountDownButtonTimer(delayedStartInterval, 1000).start()
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
        var dir = getDirection(firstLocation, secondLocation)
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
        if (!appVisibility){
            stopLocationUpdates()
        }
        inMeasurement = false

    }

    private fun endMeasurement() {
        val (magnetic, fromNotation, speedUnit) = getPreferences()
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    endMeasurement(location, speedUnit, magnetic, fromNotation)
                }
            }
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
        if (::countDownToStartTimer.isInitialized) {
            countDownToStartTimer.cancel();
        }
        if (autoFinishInterval > 0) {
            countDownToFinishTimer = CountDownButtonTimer(autoFinishInterval, 1000).start()
            measurementState = MeasurementState.RUNNING_AUTO_FINISH
        }
        if (::countUpTimer.isInitialized) {
            countUpTimer.stop()
        }
        countUpTimer = CountUpButtonTimer(1000).start()
        inMeasurement = true
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
            FINE_LOCATION_PERMISSIONS_REQUEST_CODE -> {
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
            BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.size <= 0) {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.")
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted.
                    mService?.requestLocationUpdates()
                    backgroundLocationPermissions = true
                } else {
                    // Permission denied.
                    backgroundLocationPermissions = false
                }
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onPause() {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        appVisibility = false
        if (!inMeasurement) {
            stopLocationUpdates()
            LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        appVisibility = true
        startLocationUpdates()
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
            IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
    }

    override fun onStart(){
        super.onStart()
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(
            Intent(this, LocationUpdatesService::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
//        PreferenceManager.getDefaultSharedPreferences(this).unre
//            .unregisterOnSharedPreferenceChangeListener();
        super.onStop()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        mService?.removeLocationUpdates();
        locationUpdates = false
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
        locationUpdates = true
    }

    private fun getPreferences(): Triple<Boolean, Boolean, String> {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        var magnetic = false
        var fromNotation = false
        var speedUnit : String = "m_per_min"
        if (sharedPreferences != null) {
            magnetic = sharedPreferences.getBoolean("magnetic", false)
            fromNotation = sharedPreferences.getBoolean("from_notation", false)
            speedUnit = sharedPreferences.getString("speed_unit", "m_per_min").toString()
            delayedStartInterval = (sharedPreferences.getString("delayed_start", "0")?.toLong() ?: 0) * 1000
            autoFinishInterval = (sharedPreferences.getString("auto_finish", "0")?.toLong() ?: 0) * 1000
        }
        return Triple(magnetic, fromNotation, speedUnit)
    }

    private fun formatButton(measurementState: MeasurementState, time: Long) {
        if (measurementState == MeasurementState.RUNNING) {
            start_btn.setBackgroundResource(R.drawable.btn_rnd_red)
            start_btn.setText("STOP\n" + getTimerString(time))
        } else if (measurementState == MeasurementState.STOPPED) {
            start_btn.setBackgroundResource(R.drawable.btn_rnd_grn)
            start_btn.setText("START")
        } else if (measurementState == MeasurementState.DELAYED_START) {
            start_btn.setBackgroundResource(R.drawable.btn_rnd_ylw)
            start_btn.setText("Start in\n" + getTimerString(time))
        } else if (measurementState == MeasurementState.RUNNING_AUTO_FINISH) {
            start_btn.setBackgroundResource(R.drawable.btn_rnd_blue)
            start_btn.setText(
                "STOP\n" + getTimerString(time) + "\nAuto stop\nin: " + getTimerString(
                    autoFinishInterval - time
                )
            )
        }

    }

    ////----------------Foreground service -------------------

    // A reference to the service used to get location updates.
    private var mService: LocationUpdatesService? = null
    // Tracks the bound state of the service.
    private var mBound = false
    // The BroadcastReceiver used to listen from broadcasts from the service.
    private lateinit var myReceiver: MyReceiver
    // Monitors the state of the connection to the service.
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocationUpdatesService.LocalBinder
            mService = binder.service
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        return (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            Snackbar.make(
                findViewById(R.id.activity_main),
                R.string.permission_rationale,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok, object : View.OnClickListener {
                    override fun onClick(view: View) {
                        // Request permission
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                            BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE
                        )
                    }
                })
                .show()
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun initForegroundService() {
        myReceiver = MyReceiver()
        setContentView(R.layout.activity_main);

        // Check that the user hasn't revoked permissions by going to Settings.
        if (Utils.requestingLocationUpdates(this)) {
            if (!checkPermissions()) {
                requestPermissions()
            }
        }
    }

    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stopMeasurement = intent.getBooleanExtra(LocationUpdatesService.EXTRA_STOP_MEASUREMENT, false)
            if (stopMeasurement){
                endMeasurement()
            }
            val location =
                intent.getParcelableExtra<Location>(LocationUpdatesService.EXTRA_LOCATION)
            if (location != null) {
//                Toast.makeText(
//                    this@MainActivity, Utils.getLocationText(location),
//                    Toast.LENGTH_SHORT
//                ).show()
                //TODO USe the location received from receiver
            }
        }
    }


    ///-----------------------------------------------------


    inner class CountUpButtonTimer(interval: Long) : CountUpTimer(interval) {
        override fun onTick(elapsedTime: Long) {
            formatButton(measurementState, System.currentTimeMillis() - firstTime);
        }
    }

    inner class CountDownButtonTimer(countDownInterval: Long, interval: Long) :
        CountDownTimer(countDownInterval, interval) {
        override fun onFinish() {
            if (measurementState == MeasurementState.RUNNING_AUTO_FINISH) {
                endMeasurement()
                if (!appVisibility) {
                    //If app is in background be sure to stop location updates.
                    stopLocationUpdates()
                }
            } else {
                startMeasurement()
            }
        }

        override fun onTick(elapsedTime: Long) {
            if ((!locationUpdates) && (firstTime + autoFinishInterval - System.currentTimeMillis()) < 10000) {
                //If location updates are off, start them for the auto finish (can happen when app is in background)
                startLocationUpdates()
            }
            if (measurementState == MeasurementState.RUNNING_AUTO_FINISH) {
                return
            }
            formatButton(
                measurementState,
                delayedStartTime + delayedStartInterval - System.currentTimeMillis()
            );
        }
    }


}



