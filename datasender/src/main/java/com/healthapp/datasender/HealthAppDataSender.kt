package com.healthapp.datasender

import androidx.work.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object HealthAppDataSender {

    /**
     * Url бэкенда.
     * После изменения этого свойства старые задачи будут отменены, и запланированы новые,
     * с новым адресом сервера.
     */
    var BASE_URL = "https://healthapp.space"
        set(value) {
            field = value
            reschedule()
        }

    /**
     * Время в минутах в течении которого будет производится отправка данных на сервер
     * Например если выставить 15, то отправка на сервер будет производится раз в 15 минут
     * После изменения этого свойства старые задачи будут отменены, и запланированы новые,
     * с новым интервалом.
     * Минимальное значение 15 минут, если посатвить меньше, то значение автоматически будет поднято до 15 минут.
     */
    var WORK_DELAY_MINUTES: Long = 15
        set(value) {
            field = Math.max(15, value)
            reschedule()
        }

    fun getApiService(): SenderAPI =
            Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(SenderAPI::class.java)


    private const val WORK_NAME_DEFAULT = "Send data work"

    /**
     * Запуск периодической отправки
     * После вызова этой функции будет запланирована отправка
     * данных на сервер раз в [WORK_DELAY_MINUTES] минут (минимум 15)
     */
    fun schedule() {
        WorkManager
                .getInstance()
                .enqueueUniquePeriodicWork(WORK_NAME_DEFAULT, ExistingPeriodicWorkPolicy.KEEP, createWorkRequest())
    }

    private fun reschedule() {
        WorkManager
                .getInstance()
                .enqueueUniquePeriodicWork(WORK_NAME_DEFAULT, ExistingPeriodicWorkPolicy.REPLACE, createWorkRequest())

    }


    private fun createConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // if connected to internet
            .build()

    private fun createWorkRequest() = PeriodicWorkRequest
            .Builder(SendDataWorker::class.java, WORK_DELAY_MINUTES, TimeUnit.MINUTES)
            .setConstraints(createConstraints())
            // setting a backoff on case the work needs to retry
            .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

}
