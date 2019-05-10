package ru.etu.parkinsonlibrary.database.csv

import io.reactivex.Scheduler
import io.reactivex.Single
import ru.etu.parkinsonlibrary.database.*
import ru.etu.parkinsonlibrary.stringToCsv

/**
 * Различные мапперы из моделей в базе данных в фомат csv
 */
interface EntityToCsv<T> {
    fun toCsv(items: List<T>): String
    fun getAsCsv(): Single<String>
}


class MissclickToCsv(private val missClickDao: MissClickDao,
                     private val backgroundScheduler: Scheduler) : EntityToCsv<MissClickEntity> {
    override fun getAsCsv(): Single<String> {
        return missClickDao.getAllAsSingle().subscribeOn(backgroundScheduler).map { toCsv(it) }.onErrorResumeNext(Single.just(""))
    }


    override fun toCsv(items: List<MissClickEntity>): String {
        val builder = StringBuilder()
        for (item in items) {
            if (builder.isNotEmpty()) {
                builder.append("\n")
            }
            builder.append(toString(item))
        }
        return builder.toString()
    }

    private fun toString(entity: MissClickEntity): String = "${entity.id},${entity.timestamp},${entity.distance},${entity.isMissClick}"
}

class TypingErrorToCsv(private val typeErrorDao: TypingErrorsDao,
                       private val backgroundScheduler: Scheduler) : EntityToCsv<TypingErrorEntity> {

    override fun getAsCsv(): Single<String> {
        return typeErrorDao.getAllAsSingle().subscribeOn(backgroundScheduler).map { toCsv(it) }.onErrorResumeNext(Single.just(""))

    }

    override fun toCsv(items: List<TypingErrorEntity>): String {
        val stringBuilder = StringBuilder()
        for (item in items) {
            if (stringBuilder.isNotEmpty()) {
                stringBuilder.append("\n")
            }
            stringBuilder.append(toString(item))
        }
        return stringBuilder.toString()
    }

    private fun toString(entity: TypingErrorEntity): String = "${entity.id},${entity.timestamp},${stringToCsv(entity.charSequence)},${entity.timeChanges}"

}

class RotationEntityToCsv(private val dao: OrientationDao,
                          private val netScheduler: Scheduler) : EntityToCsv<OrientationEntity> {
    override fun toCsv(items: List<OrientationEntity>): String {
        val builder = StringBuilder()
        for (item in items) {
            if (builder.isNotEmpty()) {
                builder.append("\n")
            }
            builder.append(toString(item))
        }
        return builder.toString()
    }

    private fun toString(item: OrientationEntity) = "${item.id}," +
            "${item.timestamp}," +
            "${item.pitch}," +
            "${item.azimut}," +
            "${item.roll}," +
            "${item.latitude}"+
            "${item.longitude}"+
            "${item.altitude}"

    override fun getAsCsv(): Single<String> = dao.getAllSingle().observeOn(netScheduler).map { toCsv(it) }

}