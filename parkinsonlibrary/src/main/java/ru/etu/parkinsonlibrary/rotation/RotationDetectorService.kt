package ru.etu.parkinsonlibrary.rotation

import android.app.NotificationChannel
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import ru.etu.parkinsonlibrary.R
import ru.etu.parkinsonlibrary.coordinate.LocationProvider
import ru.etu.parkinsonlibrary.database.consumer.DatabaseRotationConsumer
import ru.etu.parkinsonlibrary.di.DependencyProducer


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

    private fun moveToForeground() {
        val notificationManager = NotificationManager(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createMainNotificationChannel()
            val notification = NotificationCompat.Builder(this, notificationManager.getMainNotificationId())
                    .setSmallIcon(R.drawable.ic_heart_border)
                    .setContentTitle(getString(R.string.rotation_detector_sensor_notification_title))
                    .setContentText(getString(R.string.rotation_detector_sensor_notification_text))
                    .setGroup("Rotation group")
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
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationProvider.stopTrackingLocation()
        rotationDetector.clear()
        val broadcastIntent = Intent("ru.etu.parkinsonlibrary.rotation.RestartRotationServiceReceiver")
        sendBroadcast(broadcastIntent)
    }
}

fun RotationDetectorService.getString(id:Int) = resources.getString(id)


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
        val existingChanel = mNotificationManager.getNotificationChannel(CHANNEL_ID)
        if (existingChanel == null) {
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }
}