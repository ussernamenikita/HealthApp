package ru.etu.parkinsonlibrary.database.consumer

import ru.etu.parkinsonlibrary.database.TypingErrorEntity
import ru.etu.parkinsonlibrary.database.TypingErrorsDao
import ru.etu.parkinsonlibrary.typingerror.TypingErrorTextListener

/**
 * Consumer для событий ошибок печати,
 * Записывает полученные события в базу данных.
 */
class DatabaseTypingErrorConsumer(typeErrorDao: TypingErrorsDao) : TypingErrorTextListener.TypingErrorConsumer,
        BaseConsumer<TypingErrorEntity>(typeErrorDao) {


    override fun onEvent(currentTimestamp: Long, changes: CharSequence, l: Long) {
        this.onNewItem(TypingErrorEntity(null, currentTimestamp, changes.toString(), l))
    }


}
