package ru.etu.parkinsonlibrary.rotation

import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import ru.etu.parkinsonlibrary.R
import ru.etu.parkinsonlibrary.coordinate.LocationProvider
import ru.etu.parkinsonlibrary.database.consumer.DatabaseRotationConsumer
import ru.etu.parkinsonlibrary.di.DependencyProducer
import java.util.concurrent.TimeUnit


/**
 * Сервис, который постоянно мониторинг углы наклона устройств
 * и записывает их в базу данных.
 * Данные записываются в базу данных каждую секунду,
 * при изменении значений. Если телефон находится в одинаковом положении,
 * т.е его углы наклона не меняются, то данные в базу не сохраняются.
 */
class RotationDetectorService : CoordinationCallback, Service() {

    override fun onNewAngles(rotation: RotationDetector.Rotation) {
        consumer.onNewAngels(rotation)
    }


    companion object {
        const val LOCATION_PERMISSIONS_KEY = "LOCATION_PERMISSIONS_KEY"
        private const val NOTIFICATION_ENABLED = "NOTIFICATION_ENABLED "

        const val WORK_NAME_DEFAULT = "Rotation service startup worker"
        const val WORK_DELAY_MINUTES = 30L

        fun saveNotificationEnabled(enabled: Boolean, context: Context) {
            context
                    .applicationContext
                    .getSharedPreferences(NOTIFICATION_ENABLED, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(NOTIFICATION_ENABLED, enabled)
                    .apply()
        }

        fun isNotificationEnabled(context: Context): Boolean {
            return context
                    .applicationContext
                    .getSharedPreferences(NOTIFICATION_ENABLED, Context.MODE_PRIVATE)
                    .getBoolean(NOTIFICATION_ENABLED, false)

        }
    }


    private lateinit var rotationDetector: RotationDetector
    private lateinit var locationProvider: LocationProvider

    private lateinit var consumer: DatabaseRotationConsumer

    override fun onCreate() {
        super.onCreate()
        val module = DependencyProducer(this.application)
        this.rotationDetector = module.getRotationDetector(this)
        this.consumer = module.getRotationDatabaseConsumer()
        this.locationProvider = module.getLocatinProvider()
        rotationDetector.getOrientation(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            moveToForeground()
        }
    }

    private fun createWorkRequest() = PeriodicWorkRequest
            .Builder(RotationServiceStartupWorker::class.java, WORK_DELAY_MINUTES, TimeUnit.MINUTES, WORK_DELAY_MINUTES / 2, TimeUnit.MINUTES)
            // setting a backoff on case the work needs to retry
            .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()


    private fun moveToForeground() {
        val disableIntent = Intent(this, RotationDetectorService::class.java)
        disableIntent.putExtra(NOTIFICATION_ENABLED, false)
        val deletePendingIntent = PendingIntent.getService(this, 0, disableIntent, 0)
        val notificationManager = NotificationManager(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createMainNotificationChannel()
            val notification = NotificationCompat.Builder(this, notificationManager.getMainNotificationId())
                    .setSmallIcon(R.drawable.ic_heart_border)
                    .setContentTitle(getString(R.string.rotation_detector_sensor_notification_title))
                    .setContentText(getString(R.string.rotation_detector_sensor_notification_text))
                    .setGroup("Rotation group")
                    .addAction(android.R.drawable.ic_delete, getString(R.string.disable_monitoring_action), deletePendingIntent)
                    .build()
            startForeground(1224, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.getBooleanExtra(LOCATION_PERMISSIONS_KEY, false) == true) {
            locationProvider.startTrackingLocation(this)
            locationProvider.consumer = consumer
        }
        val savedState = isNotificationEnabled(this)
        val enabled = intent?.getBooleanExtra(NOTIFICATION_ENABLED, savedState) ?: savedState
        saveNotificationEnabled(enabled, this)
        if (!enabled) {
            stopScheduledWorks()
            stopSelf()
            return START_NOT_STICKY
        }
        scheduleWorks()
        return START_STICKY
    }

    private fun scheduleWorks() {
        WorkManager
                .getInstance()
                .enqueueUniquePeriodicWork(WORK_NAME_DEFAULT, ExistingPeriodicWorkPolicy.KEEP, createWorkRequest())
    }

    private fun stopScheduledWorks() {
        WorkManager
                .getInstance().cancelUniqueWork(WORK_NAME_DEFAULT)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationProvider.stopTrackingLocation()
        rotationDetector.clear()
        val broadcastIntent = Intent("ru.etu.parkinsonlibrary.rotation.RestartRotationServiceReceiver")
        sendBroadcast(broadcastIntent)
    }
}

fun RotationDetectorService.getString(id: Int) = resources.getString(id)


class NotificationManager(private val context: Context) {

    companion object {
        private val CHANNEL_ID = "Rotation"
        private val CHANNEL_NAME = "Rotation service chanel"
    }

    private val CHANNEL_DESCRIPTION = context.resources.getString(R.string.channel_description)

    @RequiresApi(Build.VERSION_CODES.O)
    fun getMainNotificationId(): String {
        return CHANNEL_ID
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createMainNotificationChannel() {
        val id = CHANNEL_ID
        val name = CHANNEL_NAME
        val description = CHANNEL_DESCRIPTION
        val importance = android.app.NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(id, name, importance)
        mChannel.description = description
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val existingChanel = mNotificationManager.getNotificationChannel(id)
        if (existingChanel != null) {
            mNotificationManager.deleteNotificationChannel(id)
        }
        mNotificationManager.createNotificationChannel(mChannel)
    }
}