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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.LocationPermissions.Companion.PERMISSIONS_REQUEST_LOCATION_UI
import `in`.avimarine.androidutils.LocationPermissions.Companion.arePermissionsGranted
import `in`.avimarine.androidutils.LocationPermissions.Companion.askForLocationPermission
import `in`.avimarine.androidutils.units.SpeedUnits
import `in`.avimarine.seawatercurrentmeasure.databinding.ActivityMainNewBinding
import `in`.avimarine.seawatercurrentmeasure.ui.GPSViewModel
import `in`.avimarine.seawatercurrentmeasure.ui.MainViewModel


class MainActivity : AppCompatActivity() {

    private var inMeasurement = false
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
    private var locationRequest: LocationRequest? = null
    private val UPDATE_INTERVAL: Long = 1000
    private val FASTEST_INTERVAL: Long = 1000 // = 5 seconds
    private var measurementState: MeasurementState = MeasurementState.STOPPED
    private var delayedStartInterval: Long = 0
    private var autoFinishInterval: Long = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var binder: LocationUpdatesService.LocalBinder
    private lateinit var binding: ActivityMainNewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initForegroundService()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        askForLocationPermission(
            this,
            PERMISSIONS_REQUEST_LOCATION_UI,
            getString(R.string.permission_rationale)
        )
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    lastLocation = location
                    lastLocationTime = location.time
                    binding.gps = GPSViewModel(location)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    locationIntoTextViews(
                        Location("a"),
                        binding.textTime2,
                        binding.gpsAccuracy,
                        true
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
        if (id == R.id.history_menu_action) {
            val intent = Intent(this, HistoryActivity::class.java)
            this.startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_VOLUME_DOWN) {
            startButtonClick(binding.startBtn)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @SuppressLint("MissingPermission")
    fun startButtonClick(view: View) {
        val (magnetic, fromNotation, speedUnit) = Preferences.getPreferences(this)
        delayedStartInterval = Preferences.getDelayedStartInterval(this)
        if (!arePermissionsGranted(this)) {
            askForLocationPermission(
                this,
                PERMISSIONS_REQUEST_LOCATION_UI,
                getString(R.string.permission_rationale)
            )
        } else {
            mService?.requestLocationUpdates();
        }
        if (askForLocationPermission(
                this,
                PERMISSIONS_REQUEST_LOCATION_UI,
                getString(R.string.permission_rationale)
            )
        ) {
            if (delayedStartInterval > 0 && firstTime == 0L) {
                if (measurementState == MeasurementState.DELAYED_START) {
                    measurementState = MeasurementState.STOPPED
                    resetMeasurmentState()

                    return
                }
                delayedStartTime = System.currentTimeMillis()
                measurementState = MeasurementState.DELAYED_START
                if (::countDownToStartTimer.isInitialized) {
                    countDownToStartTimer.cancel()
                }
                countDownToStartTimer = CountDownButtonTimer(delayedStartInterval, 1000).start()
                return
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

    private fun updateUI(measurement: Measurement, measurementError: Boolean = false){
        if (measurementError) {
            binding.viewmodel = null
            return
        }
        val (magnetic, fromNotation, speedUnit) = Preferences.getPreferences(this)
        binding.viewmodel = MainViewModel(measurement, magnetic, fromNotation, speedUnit )
    }

    private fun endMeasurement(
        location: Location,
        speedUnit: SpeedUnits,
        magnetic: Boolean,
        fromNotation: Boolean
    ) {
        secondLocation = location
        secondTime = System.currentTimeMillis()
        locationIntoTextViews(location, null, binding.gpsAccuracy)
        val measurement = Measurement(firstLocation, secondLocation)
        if (measurement.spd.value.isNaN()){
            updateUI(measurement, measurementError = true)
            resetMeasurmentState()
            return
        }

        updateUI(measurement)
        HistoryDataSource.addHistory(measurement, this)
        resetMeasurmentState()

    }

    private fun resetMeasurmentState() {
        firstTime = 0
        secondTime = 0
        if (::countDownToStartTimer.isInitialized) {
            countDownToStartTimer.cancel()
        }
        if (::countDownToFinishTimer.isInitialized) {
            countDownToFinishTimer.cancel()
        }
        if (::countUpTimer.isInitialized) {
            countUpTimer.stop()
        }
        formatButton(MeasurementState.STOPPED, 0)
        if (!appVisibility) {
            stopLocationUpdates()
        }
        inMeasurement = false
        binder.stop()
    }

    private fun endMeasurement() {
        val (magnetic, fromNotation, speedUnit) = Preferences.getPreferences(this)
        if (!askForLocationPermission(
                this,
                PERMISSIONS_REQUEST_LOCATION_UI,
                getString(R.string.permission_rationale)
            )
        ) {
            return
        }
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        endMeasurement(location, speedUnit, magnetic, fromNotation)
                    }
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "No permissions in startMeasurement", e)
        }
    }

    private fun startMeasurement(location: Location) {
        autoFinishInterval = Preferences.getAutoFinishInterval(this)
        firstLocation = location
        firstTime = System.currentTimeMillis()
        measurementState = MeasurementState.RUNNING
        locationIntoTextViews(location, null,  binding.gpsAccuracy)
        locationIntoTextViews(
            location,
            null,
            binding.gpsAccuracy,
            true
        )
        binding.textSpeed.text = "?"
        binding.textDir.text = "?"
        binding.textDirErr.text = "?"
        binding.textSpdErr.text = "?"
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
        binder.updateTime(autoFinishInterval, delayedStartInterval, delayedStartTime, firstTime)
    }

    private fun startMeasurement() {
        if (!askForLocationPermission(
                this,
                PERMISSIONS_REQUEST_LOCATION_UI,
                getString(R.string.permission_rationale)
            )
        ) {
            return
        }
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        startMeasurement(location)
                    }
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "No permissions in startMeasurement", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION_UI -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    val alertDialog = AlertDialog.Builder(this@MainActivity).create()
                    alertDialog.setTitle("Permissions")
                    alertDialog.setMessage("Location permission are required for this app to work.")
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK"
                    ) { _, _ -> this.finish() }
                    alertDialog.show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onPause() {
        appVisibility = false
        if (!inMeasurement) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        appVisibility = true
        startLocationUpdates()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver,
            IntentFilter(LocationUpdatesService.ACTION_BROADCAST)
        );
    }

    override fun onStart() {
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

        fusedLocationClient.requestLocationUpdates(locationRequest!!, locationCallback, null)
        locationUpdates = true
    }


    private fun formatButton(measurementState: MeasurementState, time: Long) {
        if (measurementState == MeasurementState.RUNNING) {
            binding.startBtn.setBackgroundResource(R.drawable.btn_rnd_red)
            binding.startBtn.setText("STOP\n" + getTimerString(time))
        } else if (measurementState == MeasurementState.STOPPED) {
            binding.startBtn.setBackgroundResource(R.drawable.btn_rnd_grn)
            binding.startBtn.setText("START")
        } else if (measurementState == MeasurementState.DELAYED_START) {
            binding.startBtn.setBackgroundResource(R.drawable.btn_rnd_ylw)
            binding.startBtn.setText("Start in\n" + getTimerString(time))
        } else if (measurementState == MeasurementState.RUNNING_AUTO_FINISH) {
            binding.startBtn.setBackgroundResource(R.drawable.btn_rnd_blue)
            binding.startBtn.setText(
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
            binder = service as LocationUpdatesService.LocalBinder
            Log.d(TAG, "Binding")
            binder.updateTime(
                autoFinishInterval,
                delayedStartInterval,
                delayedStartTime,
                firstTime
            )//https://stackoverflow.com/questions/9954878/android-pass-parameter-to-service-from-activity
            mService = binder.service
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }

    private fun initForegroundService() {
        myReceiver = MyReceiver()
//        setContentView(R.layout.activity_main);

        // Check that the user hasn't revoked permissions by going to Settings.
        if (Utils.requestingLocationUpdates(this) && !arePermissionsGranted(this)) {
            askForLocationPermission(
                this,
                PERMISSIONS_REQUEST_LOCATION_UI,
                getString(R.string.permission_rationale)
            )
        }
    }

    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stopMeasurement =
                intent.getBooleanExtra(LocationUpdatesService.EXTRA_STOP_MEASUREMENT, false)
            if (stopMeasurement) {
                resetMeasurmentState()
            }
            val location =
                intent.getParcelableExtra<Location>(LocationUpdatesService.EXTRA_LOCATION)
            if (location != null) {
                Log.d(TAG, location.toString())
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



