package ru.etu.parkinsonlibrary.di

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.support.v4.app.Fragment
import ru.etu.parkinsonlibrary.coordinate.LocationPermissionRequer
import ru.etu.parkinsonlibrary.coordinate.LocationProvider
import ru.etu.parkinsonlibrary.coordinate.RotationCallback
import ru.etu.parkinsonlibrary.database.ParkinsonLibraryDatabase
import ru.etu.parkinsonlibrary.database.consumer.DatabaseMissClickConsumer
import ru.etu.parkinsonlibrary.database.consumer.DatabaseRotationConsumer
import ru.etu.parkinsonlibrary.database.consumer.DatabaseTypingErrorConsumer
import ru.etu.parkinsonlibrary.rotation.RotationDetector
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

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

    fun getLocatinProvider(): LocationProvider = LocationProvider(1000, application)

    fun getLocationPermissionRequer(fragment: Fragment, rotationCallback: RotationCallback): LocationPermissionRequer = LocationPermissionRequer(fragment, rotationCallback)


    fun getRotationDatabaseConsumer(): DatabaseRotationConsumer = DatabaseRotationConsumer(getDatabase().getOrientatoinDao())


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

