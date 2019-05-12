package ru.etu.parkinsonlibrary.coordinate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task


class LocationProvider constructor(intervalMs: Int, private val context: Context) : LocationCallback() {
    private val locationPermission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private val accuracy: Int = LocationRequest.PRIORITY_HIGH_ACCURACY
    private var intervalMs = 1000


    var isTrackingLocation: Boolean = false
        private set
    private val fusedLocationClient: FusedLocationProviderClient
    var consumer: LocationConsumer? = null

    /**
     * Sets up the location request.
     *
     * @return The LocationRequest object containing the desired parameters.
     */
    private val locationRequest: LocationRequest
        get() {
            val locationRequest = LocationRequest()
            locationRequest.interval = intervalMs.toLong()
            locationRequest.fastestInterval = (intervalMs / 2).toLong()
            locationRequest.priority = accuracy
            return locationRequest
        }

    init {
        this.intervalMs = intervalMs
        isTrackingLocation = false
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    }

    fun stopTrackingLocation() {
        if (isTrackingLocation) {
            isTrackingLocation = false
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    private var task: Task<LocationSettingsResponse>? = null

    @SuppressLint("MissingPermission")
    fun startTrackingLocation(context: Context) {
        if (itHaveAllPermissions()) {
            isTrackingLocation = true
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    this, null).addOnCompleteListener { println("LOCATION COMPLETE") }
                    .addOnCanceledListener { println("LOCATION CANCELED") }
                    .addOnFailureListener { println("LOCATION FAILURE") }
                    .addOnSuccessListener { println("LOCATION SUCCESS") }
        }
    }

    private fun itHaveAllPermissions(): Boolean {
        for (perm in locationPermission) {
            if (ActivityCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }


    override fun onLocationResult(locationResult: LocationResult?) {
        this.consumer?.onLocation(locationResult!!.lastLocation)
    }
}
