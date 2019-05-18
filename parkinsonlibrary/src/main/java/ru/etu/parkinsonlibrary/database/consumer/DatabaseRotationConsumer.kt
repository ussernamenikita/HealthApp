package ru.etu.parkinsonlibrary.database.consumer

import android.location.Location
import ru.etu.parkinsonlibrary.coordinate.LocationConsumer
import ru.etu.parkinsonlibrary.database.OrientationDao
import ru.etu.parkinsonlibrary.database.OrientationEntity
import ru.etu.parkinsonlibrary.rotation.RotationDetector
import java.util.concurrent.atomic.AtomicReference

/**
 * Consumer сохраняет данные о наклоне устройсва в базу данных, если они изменились
 */
class DatabaseRotationConsumer(dao: OrientationDao) :
        BaseConsumer<OrientationEntity>(dao), LocationConsumer {

    private val currentLocation = AtomicReference<Location>()

    override fun onLocation(location: Location?) {
        location?.let { currentLocation.set(it) }
    }

    private var currentOrientation: List<Int>? = null

    private var lastOrientation: List<Int>? = null

    private var lastSavedLocation: Location? = null

    fun onNewAngels(data: RotationDetector.Rotation) {
        this.currentOrientation = listOf(data.azimut.toInt(), data.pitch.toInt(), data.roll.toInt())
        val location = currentLocation.get()
        if (!isEquals(currentOrientation, lastOrientation) || !isEquals(lastSavedLocation, location)) {
            onNewItem(OrientationEntity(null, System.currentTimeMillis(),
                    azimut = data.azimut.toInt(),
                    pitch = data.pitch.toInt(),
                    roll = data.roll.toInt(),
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    altitude = location?.altitude,
                    speed = location?.speed?.toDouble()))
        }
        this.lastOrientation = currentOrientation
        this.lastSavedLocation = currentLocation.get()
    }

    private fun isEquals(o1: Location?, o2: Location?): Boolean {
        if (o1 == null && o2 == null) {
            return true
        }
        if (o1 != null) {
            if (o2 != null) {
                return o1.latitude == o1.latitude &&
                        o1.longitude == o1.longitude &&
                        o1.altitude == o1.altitude
            }
        }
        return false
    }

    private fun isEquals(currentOrientation: List<Int>?, lastOrientation: List<Int>?): Boolean {
        if (currentOrientation == null && lastOrientation == null) {
            return true
        }
        if (currentOrientation != null) {
            if (lastOrientation != null) {
                return lastOrientation.containsAll(currentOrientation) && currentOrientation.containsAll(lastOrientation)
            }
        }
        return false
    }
}