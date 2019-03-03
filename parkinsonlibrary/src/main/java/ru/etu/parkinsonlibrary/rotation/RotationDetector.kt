package ru.etu.parkinsonlibrary.rotation

import android.app.Service
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.Scheduler
import ru.etu.parkinsonlibrary.R
import ru.etu.parkinsonlibrary.database.BaseDao
import ru.etu.parkinsonlibrary.database.OrientationEntity
import ru.etu.parkinsonlibrary.database.consumer.BaseConsumer
import java.util.concurrent.TimeUnit

class RotationDetector(private val service: Service,
                       dao: BaseDao<OrientationEntity>,
                       scheduler: Scheduler,
                       private val debounceParam: Long) : BaseConsumer<OrientationEntity>(dao, scheduler) {


    private val sensorManager = service.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    fun getOrientation(): Observable<Array<Float>> {
        checkRotationSensor()
        var sensorEventListener: SensorEventListener? = null
        val sensorObs = Observable.create<Array<Float>> { emitter ->
            sensorEventListener = object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null && !emitter.isDisposed) {
                        val d = getDataFromSensors(event.values)
                        d?.let { emitter.onNext(d) }
                    }
                }

            }
            sensorManager.registerListener(sensorEventListener, rotationSensor, 16000)
        }

        return sensorObs.doOnDispose {
            sensorEventListener?.let {
                sensorManager.unregisterListener(sensorEventListener)
            }
        }.debounce(debounceParam, TimeUnit.MILLISECONDS)
    }

    private fun getDataFromSensors(rotationVector: FloatArray?): Array<Float>? {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        val worldAxisForDeviceAxisX: Int = SensorManager.AXIS_X
        val worldAxisForDeviceAxisY: Int = SensorManager.AXIS_Z

        val adjustedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY, adjustedRotationMatrix)

        // Transform rotation matrix into azimuth/pitch/roll
        val orientation = FloatArray(3)
        SensorManager.getOrientation(adjustedRotationMatrix, orientation)

        // Convert radians to degrees
        val pitch = orientation[1] * 180.0 / Math.PI
        val roll = orientation[2] * 180.0 / Math.PI
        val azimut = orientation[0] * 180.0 / Math.PI
        return arrayOf(pitch.toFloat(), azimut.toFloat(), roll.toFloat())
    }


    private fun checkRotationSensor() {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        if (sensors.find { it.type == Sensor.TYPE_ROTATION_VECTOR } == null) {
            Toast.makeText(service, service.getString(R.string.no_sensor_detected_for_rotation_detection), Toast.LENGTH_LONG).show()
        }
    }

}
