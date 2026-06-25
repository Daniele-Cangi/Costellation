package app.constellationpulse.data

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

data class ChorusRelic(
    val day: String,
    val createdAtMillis: Long,
    val afterglowSeed: Int,
    val globalPresenceCount: Int,
    val localFieldDensity: Float,
    val synchronizationLevel: Float,
    val coherence: Float,
    val turbulence: Float
) {
    fun toPulseSeal(): PulseSeal {
        return PulseSeal(
            dateKey = day,
            createdAtMillis = createdAtMillis xor afterglowSeed.toLong(),
            message = "A sealed minute of the Chorus.",
            valence = (46 + coherence * 34f).roundToInt().coerceIn(0, 100),
            arousal = (38 + turbulence * 46f).roundToInt().coerceIn(0, 100),
            energy = (42 + (globalPresenceCount.coerceAtMost(42) / 42f) * 46f).roundToInt().coerceIn(0, 100),
            focus = (34 + synchronizationLevel * 56f).roundToInt().coerceIn(0, 100),
            social = (36 + localFieldDensity * 28f + coherence * 32f).roundToInt().coerceIn(0, 100)
        )
    }

    fun toJson(): String {
        return JSONObject()
            .put("day", day)
            .put("createdAtMillis", createdAtMillis)
            .put("afterglowSeed", afterglowSeed)
            .put("globalPresenceCount", globalPresenceCount)
            .put("localFieldDensity", localFieldDensity.coerceIn(0f, 1f).toDouble())
            .put("synchronizationLevel", synchronizationLevel.coerceIn(0f, 1f).toDouble())
            .put("coherence", coherence.coerceIn(0f, 1f).toDouble())
            .put("turbulence", turbulence.coerceIn(0f, 1f).toDouble())
            .toString(2)
    }

    companion object {
        fun fromJson(json: String): ChorusRelic? {
            return try {
                val source = JSONObject(json)
                ChorusRelic(
                    day = source.optString("day"),
                    createdAtMillis = source.optLong("createdAtMillis", 0L),
                    afterglowSeed = source.optInt("afterglowSeed", 0),
                    globalPresenceCount = source.optInt("globalPresenceCount", 0).coerceAtLeast(0),
                    localFieldDensity = source.optDouble("localFieldDensity", 0.0).toFloat().coerceIn(0f, 1f),
                    synchronizationLevel = source.optDouble("synchronizationLevel", 0.0).toFloat().coerceIn(0f, 1f),
                    coherence = source.optDouble("coherence", 0.0).toFloat().coerceIn(0f, 1f),
                    turbulence = source.optDouble("turbulence", 0.0).toFloat().coerceIn(0f, 1f)
                )
            } catch (_: JSONException) {
                null
            }
        }
    }
}

class ChorusRelicRepository(context: Context) {
    private val relicDirectory = File(context.filesDir, "chorus-relics").apply {
        mkdirs()
    }

    fun save(relic: ChorusRelic) {
        fileFor(relic.day).writeText(relic.toJson())
    }

    fun load(day: String = PulseSeal.todayKey()): ChorusRelic? {
        val file = fileFor(day)
        if (!file.exists()) {
            return null
        }

        return ChorusRelic.fromJson(file.readText())
    }

    fun loadAll(): List<ChorusRelic> {
        return relicDirectory
            .listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .mapNotNull { file -> ChorusRelic.fromJson(file.readText()) }
            .sortedByDescending { it.day }
    }

    private fun fileFor(day: String): File = File(relicDirectory, "$day.json")
}
