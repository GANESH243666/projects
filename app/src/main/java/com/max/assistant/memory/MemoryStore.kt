package com.max.assistant.memory

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

@Entity(tableName = "conversation_turns")
data class ConversationTurn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userText: String,
    val maxReply: String
)

@Entity(tableName = "memory_facts")
data class MemoryFact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fact: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM conversation_turns ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentTurns(limit: Int): List<ConversationTurn>
    @Query("SELECT * FROM conversation_turns ORDER BY timestamp DESC")
    fun observeTurns(): Flow<List<ConversationTurn>>
    @Insert suspend fun insertTurn(turn: ConversationTurn)
    @Query("SELECT * FROM memory_facts ORDER BY createdAt DESC")
    suspend fun allFacts(): List<MemoryFact>
    @Query("SELECT * FROM memory_facts ORDER BY createdAt DESC")
    fun observeFacts(): Flow<List<MemoryFact>>
    @Insert suspend fun insertFact(fact: MemoryFact)
    @Query("DELETE FROM conversation_turns WHERE id = :id") suspend fun deleteTurn(id: Long)
    @Query("DELETE FROM memory_facts WHERE id = :id") suspend fun deleteFact(id: Long)
    @Query("DELETE FROM conversation_turns") suspend fun deleteTurns()
    @Query("DELETE FROM memory_facts") suspend fun deleteFacts()
}

@Database(entities = [ConversationTurn::class, MemoryFact::class], version = 1, exportSchema = false)
abstract class MaxDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    companion object {
        @Volatile private var instance: MaxDatabase? = null
        fun get(context: Context): MaxDatabase = instance ?: synchronized(this) {
            instance ?: run {
                val passphrase = DatabaseKeyStore(context).getOrCreateKey()
                val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
                Room.databaseBuilder(context.applicationContext, MaxDatabase::class.java, "max_memory_encrypted.db")
                    .openHelperFactory(factory).build().also { instance = it }
            }
        }
    }
}

private class DatabaseKeyStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("max_secure_storage", Context.MODE_PRIVATE)
    private val alias = "max_memory_key_v1"

    fun getOrCreateKey(): String {
        val stored = prefs.getString("sqlcipher_key", null)
        if (stored == null) {
            val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
            prefs.edit().putString("sqlcipher_key", encrypt(raw)).apply()
            return Base64.encodeToString(raw, Base64.NO_WRAP)
        }
        return Base64.encodeToString(decrypt(stored), Base64.NO_WRAP)
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    private fun encrypt(raw: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(raw), Base64.NO_WRAP)
    }

    private fun decrypt(value: String): ByteArray {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, combined.copyOfRange(0, 12)))
        return cipher.doFinal(combined.copyOfRange(12, combined.size))
    }
}

class MemoryStore(context: Context) {
    private val dao = MaxDatabase.get(context).memoryDao()

    suspend fun contextFor(query: String): String {
        val turns = dao.recentTurns(10).asReversed()
        val words = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val allFacts = dao.allFacts()
        val facts = allFacts.filter { item -> words.isEmpty() || words.any { item.fact.lowercase().contains(it) || item.category.contains(it) } }.ifEmpty { allFacts.take(10) }
        return buildString {
            append("Recent conversation:\n")
            turns.forEach { append("User: ${it.userText}\nMAX: ${it.maxReply}\n") }
            append("Relevant facts:\n")
            facts.forEach { append("- [${it.category}] ${it.fact}\n") }
        }
    }

    suspend fun saveTurn(userText: String, maxReply: String) {
        if (userText.isBlank()) return
        dao.insertTurn(ConversationTurn(userText = userText.trim(), maxReply = maxReply.trim()))
        extractFacts(userText).forEach { (fact, category) ->
            if (dao.allFacts().none { it.fact.equals(fact, ignoreCase = true) }) dao.insertFact(MemoryFact(fact = fact, category = category))
        }
    }

    fun turns(): Flow<List<ConversationTurn>> = dao.observeTurns()
    fun facts(): Flow<List<MemoryFact>> = dao.observeFacts()
    suspend fun deleteTurn(id: Long) = dao.deleteTurn(id)
    suspend fun deleteFact(id: Long) = dao.deleteFact(id)
    suspend fun deleteAll() { dao.deleteTurns(); dao.deleteFacts() }

    private fun extractFacts(text: String): List<Pair<String, String>> {
        val facts = mutableListOf<Pair<String, String>>()
        Regex("(?:my name is|मेरा नाम है|मुझे कहते हैं)\\s+(.+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.let { facts += "नाम: ${it.trim()}" to "name" }
        Regex("(?:i like|i love|मुझे पसंद है|मेरी पसंद है)\\s+(.+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.let { facts += "पसंद: ${it.trim()}" to "preference" }
        Regex("(?:i usually|हर रोज|रोज़|मेरी routine)\\s+(.+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.let { facts += "रूटीन: ${it.trim()}" to "routine" }
        return facts
    }
}
