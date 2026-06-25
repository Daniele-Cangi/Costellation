package app.constellationpulse.data

import android.content.Context
import kotlin.math.max

data class ChorusMemory(
    val day: String,
    val peakPresences: Int,
    val sentEchoes: Int,
    val receivedEchoes: Int,
    val lastFieldMood: String
)

class ChorusMemoryRepository(context: Context) {
    private val preferences = context.getSharedPreferences("chorus-memory", Context.MODE_PRIVATE)

    fun load(day: String = PulseSeal.todayKey()): ChorusMemory {
        return ChorusMemory(
            day = day,
            peakPresences = preferences.getInt(key(day, "peakPresences"), 0),
            sentEchoes = preferences.getInt(key(day, "sentEchoes"), 0),
            receivedEchoes = preferences.getInt(key(day, "receivedEchoes"), 0),
            lastFieldMood = preferences.getString(key(day, "lastFieldMood"), "") ?: ""
        )
    }

    fun recordPresences(day: String, count: Int, mood: String): ChorusMemory {
        val current = load(day)
        val next = current.copy(
            peakPresences = max(current.peakPresences, count),
            lastFieldMood = mood.ifBlank { current.lastFieldMood }
        )
        save(next)
        return next
    }

    fun recordSentEcho(day: String): ChorusMemory {
        val next = load(day).let { current ->
            current.copy(sentEchoes = current.sentEchoes + 1)
        }
        save(next)
        return next
    }

    fun recordReceivedEchoes(day: String, count: Int): ChorusMemory {
        val current = load(day)
        val next = current.copy(receivedEchoes = max(current.receivedEchoes, count))
        save(next)
        return next
    }

    private fun save(memory: ChorusMemory) {
        preferences.edit()
            .putInt(key(memory.day, "peakPresences"), memory.peakPresences)
            .putInt(key(memory.day, "sentEchoes"), memory.sentEchoes)
            .putInt(key(memory.day, "receivedEchoes"), memory.receivedEchoes)
            .putString(key(memory.day, "lastFieldMood"), memory.lastFieldMood)
            .apply()
    }

    private fun key(day: String, name: String): String = "$day:$name"
}
