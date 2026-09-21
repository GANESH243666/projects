package com.max.assistant.memory

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "memories")
data class Memory(@PrimaryKey(autoGenerate = true) val id: Long = 0, val text: String, val createdAt: Long = System.currentTimeMillis(), val important: Boolean = true)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<Memory>
    @Insert suspend fun insert(memory: Memory)
    @Query("DELETE FROM memories WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [Memory::class], version = 1, exportSchema = false)
abstract class MaxDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    companion object { @Volatile private var instance: MaxDatabase? = null
        fun get(context: Context): MaxDatabase = instance ?: synchronized(this) { instance ?: Room.databaseBuilder(context, MaxDatabase::class.java, "max_memory.db").build().also { instance = it } }
    }
}

class MemoryStore(context: Context) {
    private val dao = MaxDatabase.get(context).memoryDao()
    suspend fun context(): String = dao.recent(20).joinToString("\n") { "- ${it.text}" }
    suspend fun remember(text: String) { if (text.isNotBlank()) dao.insert(Memory(text = text.trim())) }
}
