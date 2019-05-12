package ru.etu.parkinsonlibrary.database.consumer


import ru.etu.parkinsonlibrary.database.BaseDao
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

open class BaseConsumer<T>(private val dao: BaseDao<T>) {

    private val container = ConcurrentLinkedQueue<T>()
    private val executor = Executors.newSingleThreadExecutor()
    private val insertScheduled = AtomicBoolean(false)

    open fun onNewItem(newItem: T) {
        container.offer(newItem)
        startInsertIfNeed()
    }

    private fun startInsertIfNeed() {
        if (!insertScheduled.get()) {
            insertScheduled.set(true)
            executor.submit {
                do {
                    val item = container.poll()
                    item?.let { dao.insert(item) }
                } while (item != null)
                insertScheduled.set(false)
            }
        }
    }
}


