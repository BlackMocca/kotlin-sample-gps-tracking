package com.nicos.sampleforegroundservice.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import com.nicos.sampleforegroundservice.R
import android.util.Log

class LocationService : Service(), LocationListener {

    companion object {
        private const val locationChannelId = "locationChannelId"
        private const val channelName = "locationName"
        private const val minTimeLocationUpdateInMillisecond = 1000L // send loc every 1 second
        private const val minDistanceLocationUpdateInMeter = 1F // distance meter
    }

    override fun onCreate() {
        notificationService()
        super.onCreate()
    }

    /**
     * The Notification is mandatory for background services
     * */
    private fun notificationService() {
        Notification.Builder(this, locationChannelId).apply {
            setContentTitle(getString(R.string.location_service))
            setOngoing(true)
            setContentText(getString(R.string.running_service_to_find_you_location))
            setSmallIcon(R.drawable.ic_notifcation_icon)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            NotificationChannel(locationChannelId, channelName, importance).apply {
                description = getString(R.string.running_service_to_find_you_location)
                with((this@LocationService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)) {
                    createNotificationChannel(this@apply)
                }
            }
            //need core 1.12 and higher and SDK 29 and higher
            ServiceCompat.startForeground(
                this@LocationService, 1, this.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
            //this@LocationService.startForeground(1, this.build())
        }
    }

    /**
     * Main process for the service - find the background location and print it with Toast Message
     * */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!checkIfLocationPermissionIsGrande()) return START_NOT_STICKY

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            findTheLocationForSdkBiggerThan30(locationManager = locationManager)
        } else {
            findTheLocationForSdkLowerThan31(locationManager = locationManager)
        }
        return START_STICKY
    }

    private fun findTheLocationForSdkBiggerThan30(locationManager: LocationManager) {
        locationManager.requestLocationUpdates(
            LocationManager.FUSED_PROVIDER,
            minTimeLocationUpdateInMillisecond,
            minDistanceLocationUpdateInMeter,
            this@LocationService
        )
    }

    private fun findTheLocationForSdkLowerThan31(locationManager: LocationManager) {
        /**
         * this code is deprecated from the SDK 34 but we need it for lower than SDK 34
         * */
        Criteria().apply {
            accuracy = Criteria.ACCURACY_COARSE
            powerRequirement =
                Criteria.POWER_LOW

            val provider = locationManager.getBestProvider(
                this,
                false
            )
            if (provider != null) {
                locationManager.requestLocationUpdates(
                    provider,
                    minTimeLocationUpdateInMillisecond,
                    minDistanceLocationUpdateInMeter,
                    this@LocationService
                )
            }
        }
    }

    /**
     * Mandatory override when extend the Service()
     * */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Call back to get the update coordination
     * */
    override fun onLocationChanged(location: Location) {
        Log.d("LocationUpdate", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
        Toast.makeText(
            this,
            "Get Location ${location.latitude} and ${location.longitude}",
            Toast.LENGTH_LONG
        ).show()

        val updatedNotification = Notification.Builder(this, locationChannelId).apply {
            setContentTitle("Location Updated")
            setContentText("${location.latitude}, ${location.longitude}")
            setSmallIcon(R.drawable.ic_notifcation_icon)
            setOngoing(true)
        }.build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, updatedNotification)
    }

    private fun checkIfLocationPermissionIsGrande() = ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}