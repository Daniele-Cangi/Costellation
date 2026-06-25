package app.constellationpulse.data

import android.content.Context
import java.io.File

class PulseRepository(context: Context) {
    private val pulseDirectory = File(context.filesDir, "pulse").apply {
        mkdirs()
    }

    fun sealToday(pulse: PulseSeal): Boolean {
        val file = fileFor(PulseSeal.todayKey())
        if (file.exists()) {
            return false
        }

        file.writeText(pulse.copy(dateKey = PulseSeal.todayKey()).toJson())
        return true
    }

    fun save(pulse: PulseSeal) {
        fileFor(pulse.dateKey).writeText(pulse.toJson())
    }

    fun loadToday(): PulseSeal? = load(PulseSeal.todayKey())

    fun loadAll(): List<PulseSeal> {
        return pulseDirectory
            .listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .mapNotNull { file -> PulseSeal.fromJson(file.readText()) }
            .sortedByDescending { it.dateKey }
    }

    fun count(): Int = pulseDirectory
        .listFiles { file -> file.isFile && file.extension == "json" }
        .orEmpty()
        .size

    fun load(dateKey: String): PulseSeal? {
        val file = fileFor(dateKey)
        if (!file.exists()) {
            return null
        }

        return PulseSeal.fromJson(file.readText())
    }

    private fun fileFor(dateKey: String): File = File(pulseDirectory, "$dateKey.json")
}
