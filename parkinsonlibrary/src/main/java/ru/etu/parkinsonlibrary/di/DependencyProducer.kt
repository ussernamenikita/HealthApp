package ru.etu.parkinsonlibrary.di

import android.app.Activity
import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.support.v4.app.Fragment
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ru.etu.parkinsonlibrary.coordinate.RotationCallback
import ru.etu.parkinsonlibrary.coordinate.LocationPermissionRequer
import ru.etu.parkinsonlibrary.coordinate.LocationProvider
import ru.etu.parkinsonlibrary.database.*
import ru.etu.parkinsonlibrary.database.consumer.DatabaseMissClickConsumer
import ru.etu.parkinsonlibrary.database.consumer.DatabaseRotationConsumer
import ru.etu.parkinsonlibrary.database.consumer.DatabaseTypingErrorConsumer
import ru.etu.parkinsonlibrary.database.csv.EntityToCsv
import ru.etu.parkinsonlibrary.database.csv.MissclickToCsv
import ru.etu.parkinsonlibrary.database.csv.RotationEntityToCsv
import ru.etu.parkinsonlibrary.database.csv.TypingErrorToCsv
import ru.etu.parkinsonlibrary.rotation.RotationDetector
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Объект который создает зависимости
 */
class DependencyProducer(private val application: Application) {

    private var databaseInstance: ParkinsonLibraryDatabase? = null
    private var backGroundScheduler: Scheduler

    init {
        val service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), getThreadFactory())
        backGroundScheduler = Schedulers.from(service)
    }

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
        return DatabaseTypingErrorConsumer(database.typingErrorDao(), backGroundScheduler)
    }

    fun createDatabaseMissclickConsumer(): DatabaseMissClickConsumer {
        val database = getDatabase()
        return DatabaseMissClickConsumer(database.missClickDao(), backGroundScheduler)
    }

    fun getThreadFactory(): ThreadFactory = NamedThreadFactory("ParkinsonLibrary")

    fun getRotationDetector(context: Context): RotationDetector {
        return RotationDetector(context, 1000)
    }

    fun getLocatinProvider():LocationProvider = LocationProvider(1000,application)

    fun getLocationPermissionRequer(activity: Activity, rotationCallback:RotationCallback):LocationPermissionRequer = LocationPermissionRequer(activity,rotationCallback)

    fun getLocationPermissionRequer(fragment: Fragment, rotationCallback:RotationCallback):LocationPermissionRequer = LocationPermissionRequer(fragment,rotationCallback)


    fun getRotationDatabaseConsumer(): DatabaseRotationConsumer = DatabaseRotationConsumer(getDatabase().getOrientatoinDao(), backGroundScheduler)

    fun getMissclickEntityToCSV(): EntityToCsv<MissClickEntity> = MissclickToCsv(getDatabase().missClickDao(), backGroundScheduler)

    fun getTypingErrorToCSV(): EntityToCsv<TypingErrorEntity> = TypingErrorToCsv(getDatabase().typingErrorDao(), backGroundScheduler)

    fun getRotationEntityToCSV(): EntityToCsv<OrientationEntity> = RotationEntityToCsv(getDatabase().getOrientatoinDao(), backGroundScheduler)

    fun getUIScheduler(): Scheduler = AndroidSchedulers.mainThread()
    fun getDatabaseHelper(): DatabaseHelper {
        val database = getDatabase()
        val missClickDao = database.missClickDao()
        val rotationDao = database.getOrientatoinDao()
        val typingErrorDao = database.typingErrorDao()
        val missClickToCsv = getMissclickEntityToCSV()
        val errorTypingToCsv = getTypingErrorToCSV()
        val rotationToCsv = getRotationEntityToCSV()
        return DatabaseHelper(
                missClickDao = missClickDao,
                rotationDao = rotationDao,
                typingErrorDao = typingErrorDao,
                missClickToCsv = missClickToCsv,
                errorTypingToCsv = errorTypingToCsv,
                rotationToCsv = rotationToCsv,
                backgroundScheduler = backGroundScheduler)
    }
}

class NamedThreadFactory(private val mBaseName: String) : ThreadFactory {

    private val mDefaultThreadFactory: ThreadFactory = Executors.defaultThreadFactory()
    private val mCount = AtomicInteger(0)

    override fun newThread(runnable: Runnable): Thread {
        val thread = mDefaultThreadFactory.newThread(runnable)
        thread.name = mBaseName + "-" + mCount.getAndIncrement()
        return thread
    }
}

