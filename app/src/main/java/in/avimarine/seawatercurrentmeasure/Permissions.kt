package `in`.avimarine.seawatercurrentmeasure

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import `in`.avimarine.androidutils.TAG

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 27/12/2019.
 */
internal object Permissions {

    val FINE_LOCATION_PERMISSIONS_REQUEST_CODE: Int = 12345
    val BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Returns true if permission were already granted, false when asking for permission
     */
    fun askForPermissions(activity: Activity) : Boolean {
        if (!isLocationPermissionGranted(activity)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user'getDirString response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    FINE_LOCATION_PERMISSIONS_REQUEST_CODE
                )
            }
            return false
        }
        return true
    }
    private fun isLocationPermissionGranted(context:Context) =
        (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)

    /**
     * Returns the current state of the permissions needed.
     */
    fun checkPermissions(context: Context): Boolean {
        return (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }
    fun requestPermissions(activity: Activity) {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            Snackbar.make(
                activity.findViewById(R.id.activity_main),
                R.string.permission_rationale,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok, object : View.OnClickListener {
                    override fun onClick(view: View) {
                        // Request permission
                        ActivityCompat.requestPermissions(
                            activity,
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
                activity,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE
            )
        }
    }

}