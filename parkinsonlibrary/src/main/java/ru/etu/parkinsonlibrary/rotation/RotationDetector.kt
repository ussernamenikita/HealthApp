package ru.etu.parkinsonlibrary.rotation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import ru.etu.parkinsonlibrary.R

/**
 * Объект который работает с датчиком поворота
 */
class RotationDetector(private val context: Context,
                       private val debounceParam: Long) {

    private var sensorEventListener: SensorEventListener? = null
    private var currentCallback: CoordinationCallback? = null
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)


    fun getOrientation(callback: CoordinationCallback) {
        this.currentCallback = callback
        checkRotationSensor()
        unregister()
        this.sensorEventListener = object : SensorEventListener {
            private var lastSavedTimestamp = 0L

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                val currentTime = System.currentTimeMillis()
                if (event != null && currentTime - lastSavedTimestamp >= debounceParam) {
                    currentCallback?.onNewAngles(getDataFromSensors(event.values))
                    lastSavedTimestamp = currentTime
                }
            }
        }
        sensorManager.registerListener(sensorEventListener, rotationSensor, 16000)
    }

    private fun getDataFromSensors(rotationVector: FloatArray?): Rotation {
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
        return Rotation(pitch.toFloat(), azimut.toFloat(), roll.toFloat())
    }

    fun clear() {
        unregister()
        currentCallback = null
    }

    private fun unregister() = sensorEventListener.let { sensorManager.unregisterListener(it) }


    private fun checkRotationSensor() {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        if (sensors.find { it.type == Sensor.TYPE_ROTATION_VECTOR } == null) {
            Toast.makeText(context, context.getString(R.string.no_sensor_detected_for_rotation_detection), Toast.LENGTH_LONG).show()
        }
    }

    class Rotation(val pitch: Float,
                   val azimut: Float,
                   val roll: Float)


}

interface CoordinationCallback {
    fun onNewAngles(rotation: RotationDetector.Rotation)
}
