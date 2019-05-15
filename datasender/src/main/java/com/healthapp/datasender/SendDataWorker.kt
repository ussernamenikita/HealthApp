package com.healthapp.datasender

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import retrofit2.Response
import ru.etu.parkinsonlibrary.database.BaseDao
import ru.etu.parkinsonlibrary.database.MissClickEntity
import ru.etu.parkinsonlibrary.database.OrientationEntity
import ru.etu.parkinsonlibrary.database.TypingErrorEntity
import ru.etu.parkinsonlibrary.di.DependencyProducer


const val CHUNK_SIZE = 2000
const val ERROR_CODE_NOT_AUTHORIZED = 401
const val ERROR_CODE_INTERNAL_SERVER_ERROR = 500


class SendDataWorker(appContext: Context, params: WorkerParameters) :
        Worker(appContext, params) {

    companion object {
        const val LOG_TAG = "SendDataWorker"
        const val USER_ID = "DATA_SENDER_USER_ID"
    }

    private val service = HealthAppDataSender.getApiService()

    private lateinit var userID: String

    override fun doWork(): Result {
        Log.d(LOG_TAG, "Try send data to server")
        try {
            this.userID = getUserId()
            val producer = DependencyProducer(applicationContext as Application)
            val db = producer.getDatabase()
            sendAll(db.missClickDao(), this::sendMissClickToServer)
            sendAll(db.typingErrorDao(), this::sendTyingErrorEntity)
            sendAll(db.getOrientatoinDao(), this::sendOrientationToServer)
        } catch (authException: UnauthorizedException) {
            Log.d(LOG_TAG, "Send data to server failed with $authException")
            return Result.failure()
        } catch (serverError: ServerError) {
            Log.d(LOG_TAG, "Send data to server failed with $serverError")
            return Result.failure()
        } catch (t: Throwable) {
            Log.d(LOG_TAG, "Send data to server failed with $t")
            t.printStackTrace()
            return Result.failure()
        }
        Log.d(LOG_TAG, "Send data to server success")
        return Result.success()
    }

    private fun getUserId(): String {
        val id = applicationContext.getSharedPreferences(USER_ID, Context.MODE_PRIVATE).getString(USER_ID, null)
        if (id == null) {
            Log.d(LOG_TAG, "User id is null, can't send data to backend")
            throw UnauthorizedException("User id is null")
        }
        return id
    }


    private fun <T> sendAll(dao: BaseDao<T>, sendFunction: (List<T>) -> Response<Void>): Boolean {
        var chunk: List<T>
        do {
            chunk = dao.get(CHUNK_SIZE)
            if(chunk.isEmpty()){
                return true
            }
            val response = sendFunction(chunk)
            if (response.isSuccessful) {
                Log.d(LOG_TAG,"Successfully send ${chunk.size} items to database")
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
            service.sendMissclickData(userID, missClicks).execute()

    private fun sendOrientationToServer(data: List<OrientationEntity>): Response<Void> =
            service.sendOrientationData(userID, data).execute()


    private fun sendTyingErrorEntity(data: List<TypingErrorEntity>): Response<Void> =
            service.sendTextTypingErrorsData(userID, data).execute()

}

fun SendDataWorker.getString(id: Int): String = applicationContext.resources.getString(id)

class ServerError(s: String) : Exception(s)

class UnauthorizedException(errorMsg: String) : Exception(errorMsg)
