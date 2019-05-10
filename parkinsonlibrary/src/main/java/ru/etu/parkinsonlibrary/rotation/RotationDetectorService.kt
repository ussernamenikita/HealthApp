package ru.etu.parkinsonlibrary.rotation

import android.R
import android.app.NotificationChannel
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import io.reactivex.disposables.Disposable
import ru.etu.parkinsonlibrary.coordinate.LocationConsumer
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
class RotationDetectorService : Service(){


    companion object {
        const val LOCATION_PERMISSIONS_KEY = "LOCATION_PERMISSIONS_KEY"
    }

    private var rotationSubscription: Disposable? = null


    private lateinit var locationProvider: LocationProvider

    private lateinit var consumer: DatabaseRotationConsumer

    override fun onCreate() {
        super.onCreate()
        val module = DependencyProducer(this.application)
        val uiScheduler = module.getUIScheduler()
        val rotationDetector = module.getRotationDetector(this)
        this.consumer = module.getRotationDatabaseConsumer()
        this.locationProvider = module.getLocatinProvider()
        rotationSubscription = rotationDetector.getOrientation().observeOn(uiScheduler).subscribe({ result ->
            consumer.onNewAngels(result)
        }, {
            it.printStackTrace()
            stopSelf()
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            moveToForeground()
        }
    }

    private fun moveToForeground() {
        val notificationManager = NotificationManager(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createMainNotificationChannel()
            val notification = NotificationCompat.Builder(this, notificationManager.getMainNotificationId())
                    .setSmallIcon(R.color.white)
                    .setContentTitle("Rotation ")
                    .setContentText("Monitoring rotation is working")
                    .build()
            startForeground(1224, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if(intent?.getBooleanExtra(LOCATION_PERMISSIONS_KEY,false) == true){
            locationProvider.startTrackingLocation(this )
            locationProvider.consumer = consumer
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        rotationSubscription?.dispose()
        locationProvider.stopTrackingLocation()
        val broadcastIntent = Intent("ru.etu.parkinsonlibrary.rotation.RestartRotationServiceReceiver")
        sendBroadcast(broadcastIntent)
    }
}


class NotificationManager(private val context: Context) {

    companion object {
        private val CHANNEL_ID = "Rotation"
        private val CHANNEL_NAME = "Rotation service chanel"
        private val CHANNEL_DESCRIPTION = "Chanel for rotation service, if it's need to be configure like a foreground"
    }

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