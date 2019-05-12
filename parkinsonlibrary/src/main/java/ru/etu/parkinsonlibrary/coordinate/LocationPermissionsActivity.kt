package ru.etu.parkinsonlibrary.coordinate

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ru.etu.parkinsonlibrary.di.DependencyProducer

class LocationPermissionsActivity : RotationCallback, AppCompatActivity() {


    private lateinit var dependencyProducer :DependencyProducer
    private lateinit var permissionRequer : LocationPermissionRequer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dependencyProducer = DependencyProducer(this.application)
        permissionRequer = dependencyProducer.getLocationPermissionRequer(this, this)
        permissionRequer.requestPermissions()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionRequer.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onGranted() {
        dependencyProducer.startMonitoringServiceIntent(this)
        finish()
    }

    override fun onDenied() {
        finish()
    }
}