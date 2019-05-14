package ru.etu.parkinsonlibrary.di

import android.app.Activity
import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import ru.etu.parkinsonlibrary.coordinate.LocationPermissionRequer
import ru.etu.parkinsonlibrary.coordinate.LocationPermissionsActivity
import ru.etu.parkinsonlibrary.coordinate.LocationProvider
import ru.etu.parkinsonlibrary.coordinate.RotationCallback
import ru.etu.parkinsonlibrary.database.ParkinsonLibraryDatabase
import ru.etu.parkinsonlibrary.database.consumer.DatabaseMissClickConsumer
import ru.etu.parkinsonlibrary.database.consumer.DatabaseRotationConsumer
import ru.etu.parkinsonlibrary.database.consumer.DatabaseTypingErrorConsumer
import ru.etu.parkinsonlibrary.rotation.RotationDetector
import ru.etu.parkinsonlibrary.rotation.RotationDetectorService
import ru.etu.parkinsonlibrary.rotation.RotationServiceStartupWorker
import java.util.concurrent.TimeUnit

/**
 * Объект который создает зависимости
 */
class DependencyProducer(private val application: Application) {

    private var databaseInstance: ParkinsonLibraryDatabase? = null

    fun getDatabase(): ParkinsonLibraryDatabase {
        if (this.databaseInstance == null) {
            this.databaseInstance = Room.databaseBuilder(
                    application,
                    ParkinsonLibraryDatabase::class.java, "ParkinsonLibrary"
            ).fallbackToDestructiveMigration()
                    .build()
        }
        return this.databaseInstance!!
    }

    fun createDatabaseTypingErrorConsumer(): DatabaseTypingErrorConsumer {
        val database = getDatabase()
        return DatabaseTypingErrorConsumer(database.typingErrorDao())
    }

    fun createDatabaseMissclickConsumer(): DatabaseMissClickConsumer {
        val database = getDatabase()
        return DatabaseMissClickConsumer(database.missClickDao())
    }

    fun getRotationDetector(context: Context): RotationDetector {
        return RotationDetector(context, 1000)
    }

    fun startMonitoringServiceIntent(activity: Activity) {
        val intent = Intent(activity, RotationDetectorService::class.java)
        val withLocation = getLocationPermissionRequer(activity, null).itHaveAllPermissions()
        intent.putExtra(RotationDetectorService.LOCATION_PERMISSIONS_KEY, withLocation)
        activity.startService(intent)
    }


    fun startMonitoringServiceIntent(context: Context) {
        val intent = Intent(context, RotationDetectorService::class.java)
        context.startService(intent)
    }

    fun startRequestPermisionsActivity(activity: Activity) {
        val intent = Intent(activity, LocationPermissionsActivity::class.java)
        activity.startActivity(intent)
    }

    fun getLocatinProvider(): LocationProvider = LocationProvider(1000, application)

    fun getLocationPermissionRequer(fragment: Fragment, rotationCallback: RotationCallback?): LocationPermissionRequer = LocationPermissionRequer(fragment, rotationCallback)
    fun getLocationPermissionRequer(activity: Activity, rotationCallback: RotationCallback?): LocationPermissionRequer = LocationPermissionRequer(activity, rotationCallback)


    fun getRotationDatabaseConsumer(): DatabaseRotationConsumer = DatabaseRotationConsumer(getDatabase().getOrientatoinDao())


}
