package ru.etu.parkinsonlibrary.database.consumer

import ru.etu.parkinsonlibrary.database.MissClickDao
import ru.etu.parkinsonlibrary.database.MissClickEntity
import ru.etu.parkinsonlibrary.missclick.CloseTouchEvent
import ru.etu.parkinsonlibrary.missclick.MissClickEventsConsumer

class DatabaseMissClickConsumer(missClickDao: MissClickDao) : MissClickEventsConsumer,
        BaseConsumer<MissClickEntity>(missClickDao) {


    override fun onConsume(timestamp: Long, clickDistanceFromCenter: Double, closeEvents: ArrayList<CloseTouchEvent>) {
        for (event in closeEvents) {
            System.currentTimeMillis()
            this.onNewItem(MissClickEntity(null, event.timestamp, event.distanceFromCenter, true))
        }
        this.onNewItem(MissClickEntity(null, timestamp, clickDistanceFromCenter, false))
    }

}