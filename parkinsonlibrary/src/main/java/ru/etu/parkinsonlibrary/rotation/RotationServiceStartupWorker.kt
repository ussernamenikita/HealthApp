package ru.etu.parkinsonlibrary.rotation

import android.app.Application
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.etu.parkinsonlibrary.di.DependencyProducer

class RotationServiceStartupWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {

    override fun doWork(): Result {
        DependencyProducer(applicationContext as Application).startMonitoringServiceIntent(applicationContext)
        return Result.success()
    }

}