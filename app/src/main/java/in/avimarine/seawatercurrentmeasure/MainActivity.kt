package `in`.avimarine.seawatercurrentmeasure

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicMask


class MainActivity : AppCompatActivity() {
    private lateinit var textView2: TextView
    private var lastLocationTime: Long = 0
    private var lastLocation: Location? = null
    private var firstTime: Long = 0
    private var secondTime: Long = 0
    private var firstLocation: Location? = null
    private var secondLocation: Location? = null
    private lateinit var textView: TextView
    private val MY_PERMISSIONS_REQUEST: Int = 12345
    private var locationRequest: LocationRequest? = null
    private val UPDATE_INTERVAL: Long = 1000
    private val FASTEST_INTERVAL: Long = 1000 // = 5 seconds


    private lateinit var locationCallback: LocationCallback

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        textView = findViewById(R.id.textView) as TextView
        textView2 = findViewById(R.id.textView2) as TextView

        if (!isLocationPermissionGranted())
            askForPermissions()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    lastLocation = location
                    lastLocationTime = location.time

                    textView2.text = lastLocation.toString()
                }
            }
        }
        startLocationUpdates()
    }

    private fun askForPermissions() {
        if (!isLocationPermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
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


    @SuppressLint("MissingPermission")
    fun startButtonClick(view: View) {
        if (!isLocationPermissionGranted())
            askForPermissions()
        else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        if (firstLocation == null) {
                            firstLocation = location
                            firstTime = System.currentTimeMillis()
                        } else {
                            secondLocation = location
                            secondTime = System.currentTimeMillis()
                            val dist = getDistance(
                                firstLocation!!.latitude, firstLocation!!.longitude, secondLocation!!.latitude,
                                secondLocation!!.longitude
                            )
                            val dir = getDirection(
                                firstLocation!!.latitude, firstLocation!!.longitude, secondLocation!!.latitude,
                                secondLocation!!.longitude
                            )
                            val speed = getSpeed(dist, firstTime, secondTime)
                            textView.text = dist.toString() + ", " + dir.toString() + ", " + speed.toString()
                            firstLocation = null
                            secondLocation = null
                        }
                    } else {
                        textView.text = "Null"
                    }
                }
        }
    }

    private fun getSpeed(dist: Double, firstTime: Long, secondTime: Long): Any {
        val duration = (secondTime - firstTime).toDouble() / 1000
        return dist / duration
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

    private val geod = Geodesic.WGS84// This matches EPSG4326, which is the coordinate system used by Geolake

    /**
     * Get the distance between two points in meters.
     * @param lat1 First point's latitude
     * @param lon1 First point's longitude
     * @param lat2 Second point's latitude
     * @param lon2 Second point's longitude
     * @return Distance between the first and the second point in meters
     */
    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val line = geod.InverseLine(
            lat1,
            lon1,
            lat2,
            lon2,
            GeodesicMask.DISTANCE_IN or GeodesicMask.LATITUDE or GeodesicMask.LONGITUDE
        )
        return line.Distance()
    }

    /**
     * Get the distance between two points in meters.
     * @param lat1 First point's latitude
     * @param lon1 First point's longitude
     * @param lat2 Second point's latitude
     * @param lon2 Second point's longitude
     * @return Distance between the first and the second point in meters
     */
    fun getDirection(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val line = geod.InverseLine(
            lat1,
            lon1,
            lat2,
            lon2,
            GeodesicMask.DISTANCE_IN or GeodesicMask.LATITUDE or GeodesicMask.LONGITUDE
        )
        return line.Azimuth()
    }


    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
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


}

