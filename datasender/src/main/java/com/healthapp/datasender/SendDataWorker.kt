package com.healthapp.datasender

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.healthapp.firebaseauth.FirebaseAuth
import retrofit2.Response
import ru.etu.parkinsonlibrary.database.BaseDao
import ru.etu.parkinsonlibrary.database.MissClickEntity
import ru.etu.parkinsonlibrary.database.OrientationEntity
import ru.etu.parkinsonlibrary.database.TypingErrorEntity
import ru.etu.parkinsonlibrary.di.DependencyProducer

const val CHUNK_SIZE = 2000
const val ERROR_CODE_NOT_AUTHORIZED = 401
const val ERROR_CODE_INTERNAL_SERVER_ERROR = 500
const val AUTH_NOTIFICATION_ID = 1
const val CHANNEL_NAME = "Authentication"

class SendDataWorker(appContext: Context, params: WorkerParameters) :
        Worker(appContext, params) {

    private val service = HealthAppDataSender.getApiService()

    private lateinit var authToken: String

    override fun doWork(): Result {
        try {
            this.authToken = getToken()
            val producer = DependencyProducer(applicationContext as Application)
            val db = producer.getDatabase()
            sendAll(db.missClickDao(), this::sendMissClickToServer)
            sendAll(db.typingErrorDao(), this::sendTyingErrorEntity)
            sendAll(db.getOrientatoinDao(), this::sendOrientationToServer)
        } catch (authException: UnauthorizedException) {
            showNotificationAboutAuthentication()
            return Result.failure()
        } catch (serverError: ServerError) {
            return Result.failure()
        }
        return Result.success()
    }

    private fun showNotificationAboutAuthentication() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val intent = Intent(applicationContext, DataSenderAuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_NAME)
                .setContentTitle(getString(R.string.auth_notification_title))
                .setContentText(getString(R.string.auth_notification_content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        with(NotificationManagerCompat.from(applicationContext)) {
            // notificationId is a unique int for each notification that you must define
            notify(AUTH_NOTIFICATION_ID, builder.build())
        }
    }

    private fun createChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_NAME
            val descriptionText = getString(R.string.auth_chanel_description)
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_NAME, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: android.app.NotificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getToken(): String {
        val user = FirebaseAuth.getCurrentUser()
        if (user == null) {
            throw UnauthorizedException("Current user is null, user must authorize first")
        } else {
            val result = Tasks.await(user.getIdToken(true))
            val token = result.token
            if (token == null) {
                throw UnauthorizedException("Can not get token for current user")
            } else {
                return token
            }
        }
    }


    private fun <T> sendAll(dao: BaseDao<T>, sendFunction: (List<T>) -> Response<Void>): Boolean {
        var chunk: List<T>
        do {
            chunk = dao.get(CHUNK_SIZE)
            val response = sendFunction(chunk)
            if (response.isSuccessful) {
                dao.delete(chunk)
            } else {
                when (response.code()) {
                    ERROR_CODE_NOT_AUTHORIZED -> throw  UnauthorizedException("Server return authorization error")
                    ERROR_CODE_INTERNAL_SERVER_ERROR -> throw ServerError("Internal server error")
                    else -> return false
                }
            }
        } while (chunk.size >= CHUNK_SIZE)
        return true
    }


    private fun sendMissClickToServer(missClicks: List<MissClickEntity>): Response<Void> =
            service.sendMissclickData(authToken, missClicks).execute()

    private fun sendOrientationToServer(data: List<OrientationEntity>): Response<Void> =
            service.sendOrientationData(authToken, data).execute()


    private fun sendTyingErrorEntity(data: List<TypingErrorEntity>): Response<Void> =
            service.sendTextTypingErrorsData(authToken, data).execute()

}

fun SendDataWorker.getString(id: Int): String = applicationContext.resources.getString(id)

class ServerError(s: String) : Exception(s)

class UnauthorizedException(errorMsg: String) : Exception(errorMsg)
