
package com.example.devicemonitor

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: List<Long>,
    val batteryTemperature: List<Float>,
    val batteryPercent: List<Int>,
    val fps: List<Int>,
)

class Converters {
    @TypeConverter
    fun fromLongList(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        if (value.isBlank()) emptyList()
        else value.split(",").mapNotNull { it.trim().toLongOrNull() }

    @TypeConverter
    fun fromFloatList(value: List<Float>): String = value.joinToString(",")

    @TypeConverter
    fun toFloatList(value: String): List<Float> =
        if (value.isBlank()) emptyList()
        else value.split(",").mapNotNull { it.trim().toFloatOrNull() }

    @TypeConverter
    fun fromIntList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isBlank()) emptyList()
        else value.split(",").mapNotNull { it.trim().toIntOrNull() }
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY id DESC")
    fun getAllRecords(): Flow<List<Record>>

    @Insert()
    suspend fun insert(record: Record): Long

    @Delete
    suspend fun delete(record: Record)

    @Query("DELETE FROM records")
    suspend fun clearAll()
}

@Database(entities = [Record::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "record_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}