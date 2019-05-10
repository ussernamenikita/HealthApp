package ru.etu.parkinsonlibrary.database

import android.arch.persistence.room.*
import io.reactivex.Single

/**
 * Конфигурация базы данных
 */
@Database(entities = [MissClickEntity::class,
    TypingErrorEntity::class,
    OrientationEntity::class], version = 2, exportSchema = false)
abstract class ParkinsonLibraryDatabase : RoomDatabase() {
    abstract fun missClickDao(): MissClickDao
    abstract fun typingErrorDao(): TypingErrorsDao
    abstract fun getOrientatoinDao(): OrientationDao

}

@Dao
interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(event: T)

    fun get(limit: Int): List<T>

    @Delete
    fun delete(items: List<T>)
}

@Entity
data class MissClickEntity(@PrimaryKey(autoGenerate = true) val id: Long? = 0L,
                           @ColumnInfo(name = "timestamp") var timestamp: Long,
                           @ColumnInfo(name = "distance") var distance: Double,
                           @ColumnInfo(name = "isMissClick") var isMissClick: Boolean)

@Dao
interface MissClickDao : BaseDao<MissClickEntity> {

    @Query("DELETE FROM MissClickEntity")
    fun clear()

    @Query("SELECT * FROM MissClickEntity")
    fun getAll(): List<MissClickEntity>


    @Query("SELECT * FROM MissClickEntity")
    fun getAllAsSingle(): Single<List<MissClickEntity>>

    @Query("SELECT * FROM MissClickEntity LIMIT :limit")
    override fun get(limit: Int): List<MissClickEntity>

}

@Entity
data class TypingErrorEntity(@PrimaryKey(autoGenerate = true) val id: Long? = 0L,
                             @ColumnInfo(name = "timeStamp") val timestamp: Long,
                             @ColumnInfo(name = "symbol") val charSequence: String,
                             @ColumnInfo(name = "timeChanges") val timeChanges: Long)

@Dao
interface TypingErrorsDao : BaseDao<TypingErrorEntity> {

    @Query("DELETE FROM TypingErrorEntity")
    fun clear()

    @Query("SELECT * FROM TypingErrorEntity")
    fun getAll(): List<TypingErrorEntity>

    @Query("SELECT * FROM TypingErrorEntity")
    fun getAllAsSingle(): Single<List<TypingErrorEntity>>

    @Query("SELECT * FROM TypingErrorEntity LIMIT :limit")
    override fun get(limit: Int): List<TypingErrorEntity>
}

@Entity
data class OrientationEntity(@PrimaryKey(autoGenerate = true) val id: Long? = null,
                             @ColumnInfo(name = "timestamp") val timestamp: Long,
                             @ColumnInfo(name = "pitch") val pitch: Int,
                             @ColumnInfo(name = "azimut") val azimut: Int,
                             @ColumnInfo(name = "roll") val roll: Int,
                             @ColumnInfo(name = "latitude") val latitude: Double?,
                             @ColumnInfo(name = "longitude") val longitude: Double?,
                             @ColumnInfo(name = "altitude") val altitude: Double?)

@Dao
interface OrientationDao : BaseDao<OrientationEntity> {

    @Query("DELETE FROM OrientationEntity")
    fun clear()

    @Query("SELECT * FROM OrientationEntity")
    fun getAll(): List<OrientationEntity>

    @Query("SELECT * FROM OrientationEntity")
    fun getAllSingle(): Single<List<OrientationEntity>>

    @Query("SELECT * FROM OrientationEntity LIMIT :limit")
    override fun get(limit: Int): List<OrientationEntity>
}

