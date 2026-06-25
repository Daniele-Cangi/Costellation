package app.constellationpulse.data

import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class PulseSeal(
    val dateKey: String,
    val createdAtMillis: Long,
    val message: String,
    val valence: Int,
    val arousal: Int,
    val energy: Int,
    val focus: Int,
    val social: Int
) {
    val pulseIndex: Int
        get() = listOf(valence, arousal, energy, focus, social).average().roundToInt()

    val isBright: Boolean
        get() = pulseIndex >= 62

    fun toJson(): String {
        return JSONObject()
            .put("date", dateKey)
            .put("createdAtMillis", createdAtMillis)
            .put("message", message)
            .put("valence", valence.clampSignal())
            .put("arousal", arousal.clampSignal())
            .put("energy", energy.clampSignal())
            .put("focus", focus.clampSignal())
            .put("social", social.clampSignal())
            .toString(2)
    }

    companion object {
        fun today(
            message: String,
            valence: Int,
            arousal: Int,
            energy: Int,
            focus: Int,
            social: Int
        ): PulseSeal {
            return PulseSeal(
                dateKey = todayKey(),
                createdAtMillis = System.currentTimeMillis(),
                message = message,
                valence = valence.clampSignal(),
                arousal = arousal.clampSignal(),
                energy = energy.clampSignal(),
                focus = focus.clampSignal(),
                social = social.clampSignal()
            )
        }

        fun todayKey(): String {
            return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        }

        fun fromJson(json: String): PulseSeal? {
            return try {
                val source = JSONObject(json)
                PulseSeal(
                    dateKey = source.optString("date", source.optString("dateKey")),
                    createdAtMillis = source.optLong("createdAtMillis", 0L),
                    message = source.optString("message", ""),
                    valence = source.optInt("valence", 50).clampSignal(),
                    arousal = source.optInt("arousal", 50).clampSignal(),
                    energy = source.optInt("energy", 50).clampSignal(),
                    focus = source.optInt("focus", 50).clampSignal(),
                    social = source.optInt("social", 50).clampSignal()
                )
            } catch (_: JSONException) {
                null
            }
        }
    }
}

private fun Int.clampSignal(): Int = coerceIn(0, 100)
