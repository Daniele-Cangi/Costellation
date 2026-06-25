package app.constellationpulse.backend

import android.content.Context
import app.constellationpulse.data.PulseSeal
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.security.MessageDigest

data class RemoteFieldOrb(
    val orbId: String,
    val day: String,
    val cellId: String,
    val valence: Int,
    val arousal: Int,
    val energy: Int,
    val focus: Int,
    val social: Int,
    val createdAtMillis: Long
) {
    fun toPulseSeal(): PulseSeal {
        return PulseSeal(
            dateKey = day,
            createdAtMillis = createdAtMillis,
            message = "",
            valence = valence,
            arousal = arousal,
            energy = energy,
            focus = focus,
            social = social
        )
    }
}

data class RemoteFieldEcho(
    val targetOrbId: String,
    val echoSeed: String,
    val createdAtMillis: Long
)

data class RemoteChorusPresence(
    val presenceId: String,
    val day: String,
    val coarseCellId: String,
    val touchStability: Float,
    val stillness: Float,
    val turbulence: Float,
    val clientSeed: Int,
    val joinedAtMillis: Long,
    val lastSeenAtMillis: Long
)

data class RemoteChorusState(
    val globalPresenceCount: Int = 0,
    val localFieldDensity: Float = 0f,
    val synchronizationLevel: Float = 0f,
    val coherence: Float = 0f,
    val turbulence: Float = 0f,
    val afterglowSeed: Int = 0,
    val activePresences: List<RemoteChorusPresence> = emptyList()
)

class FirebaseFieldService(private val context: Context) {
    private val isConfigured: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    fun isAvailable(): Boolean = isConfigured

    fun publishOrb(
        cellId: String,
        pulse: PulseSeal,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!isConfigured) {
            onComplete(false)
            return
        }

        ensureAnonymousAuth { uid ->
            if (uid == null) {
                onComplete(false)
                return@ensureAnonymousAuth
            }

            val orbId = dailyHash(uid, pulse.dateKey)
            val data = mapOf(
                "orbId" to orbId,
                "day" to pulse.dateKey,
                "valence" to pulse.valence,
                "arousal" to pulse.arousal,
                "energy" to pulse.energy,
                "focus" to pulse.focus,
                "social" to pulse.social,
                "createdAtMillis" to System.currentTimeMillis()
            )

            orbsCollection(pulse.dateKey, cellId)
                .document(orbId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun resolveDailyOrbId(
        day: String,
        onReady: (String?) -> Unit
    ) {
        if (!isConfigured) {
            onReady(null)
            return
        }

        ensureAnonymousAuth { uid ->
            onReady(uid?.let { dailyHash(it, day) })
        }
    }

    fun listenNearbyField(
        day: String,
        cellId: String,
        onUpdate: (List<RemoteFieldOrb>) -> Unit,
        onError: () -> Unit = {}
    ): ListenerRegistration? {
        if (!isConfigured) {
            return null
        }

        return orbsCollection(day, cellId)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(40)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError()
                    return@addSnapshotListener
                }

                val orbs = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull { document ->
                        val orbId = document.getString("orbId") ?: document.id
                        val orbDay = document.getString("day") ?: day
                        RemoteFieldOrb(
                            orbId = orbId,
                            day = orbDay,
                            cellId = cellId,
                            valence = document.getLong("valence")?.toInt()?.coerceIn(0, 100) ?: 50,
                            arousal = document.getLong("arousal")?.toInt()?.coerceIn(0, 100) ?: 50,
                            energy = document.getLong("energy")?.toInt()?.coerceIn(0, 100) ?: 50,
                            focus = document.getLong("focus")?.toInt()?.coerceIn(0, 100) ?: 50,
                            social = document.getLong("social")?.toInt()?.coerceIn(0, 100) ?: 50,
                            createdAtMillis = document.getLong("createdAtMillis") ?: 0L
                        )
                    }

                onUpdate(orbs)
            }
    }

    fun listenReceivedEchoes(
        day: String,
        cellId: String,
        targetOrbId: String,
        onUpdate: (List<RemoteFieldEcho>) -> Unit,
        onError: () -> Unit = {}
    ): ListenerRegistration? {
        if (!isConfigured || targetOrbId.isBlank()) {
            return null
        }

        return echoesCollection(day, cellId)
            .whereEqualTo("targetOrbId", targetOrbId)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError()
                    return@addSnapshotListener
                }

                val echoes = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull { document ->
                        val echoSeed = document.getString("echoSeed") ?: return@mapNotNull null
                        RemoteFieldEcho(
                            targetOrbId = document.getString("targetOrbId") ?: targetOrbId,
                            echoSeed = echoSeed,
                            createdAtMillis = document.getLong("createdAtMillis") ?: 0L
                        )
                    }
                    .sortedByDescending { it.createdAtMillis }

                onUpdate(echoes)
            }
    }

    fun sendEcho(
        day: String,
        cellId: String,
        targetOrbId: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!isConfigured) {
            onComplete(false)
            return
        }

        ensureAnonymousAuth { uid ->
            if (uid == null) {
                onComplete(false)
                return@ensureAnonymousAuth
            }

            val echoSeed = dailyHash("$uid:$targetOrbId", day)
            val data = mapOf(
                "targetOrbId" to targetOrbId,
                "echoSeed" to echoSeed,
                "createdAtMillis" to System.currentTimeMillis()
            )

            echoesCollection(day, cellId)
                .document()
                .set(data)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun publishChorusPresence(
        day: String,
        coarseCellId: String,
        touchStability: Float,
        stillness: Float,
        turbulence: Float,
        clientSeed: Int,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!isConfigured) {
            onComplete(false)
            return
        }

        ensureAnonymousAuth { uid ->
            if (uid == null) {
                onComplete(false)
                return@ensureAnonymousAuth
            }

            val presenceId = dailyHash("chorus:$uid", day)
            val now = System.currentTimeMillis()
            val data = mapOf(
                "presenceId" to presenceId,
                "day" to day,
                "coarseCellId" to coarseCellId,
                "touchStability" to touchStability.coerceIn(0f, 1f),
                "stillness" to stillness.coerceIn(0f, 1f),
                "turbulence" to turbulence.coerceIn(0f, 1f),
                "clientSeed" to clientSeed,
                "joinedAtMillis" to now,
                "lastSeenAtMillis" to now
            )

            chorusPresencesCollection(day)
                .document(presenceId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun listenChorusState(
        day: String,
        localCellId: String,
        onUpdate: (RemoteChorusState) -> Unit,
        onError: () -> Unit = {}
    ): ListenerRegistration? {
        if (!isConfigured) {
            return null
        }

        return chorusPresencesCollection(day)
            .orderBy("lastSeenAtMillis", Query.Direction.DESCENDING)
            .limit(160)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError()
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                val activeWindowMillis = 24_000L
                val presences = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull { document ->
                        val lastSeen = document.getLong("lastSeenAtMillis") ?: return@mapNotNull null
                        if (now - lastSeen > activeWindowMillis) {
                            return@mapNotNull null
                        }
                        val presenceId = document.getString("presenceId") ?: document.id
                        RemoteChorusPresence(
                            presenceId = presenceId,
                            day = document.getString("day") ?: day,
                            coarseCellId = document.getString("coarseCellId") ?: "",
                            touchStability = document.getDouble("touchStability")?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
                            stillness = document.getDouble("stillness")?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
                            turbulence = document.getDouble("turbulence")?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
                            clientSeed = document.getLong("clientSeed")?.toInt() ?: presenceId.hashCode(),
                            joinedAtMillis = document.getLong("joinedAtMillis") ?: lastSeen,
                            lastSeenAtMillis = lastSeen
                        )
                    }

                onUpdate(buildChorusState(day, localCellId, presences))
            }
    }

    private fun ensureAnonymousAuth(onReady: (String?) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            onReady(currentUser.uid)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result -> onReady(result.user?.uid) }
            .addOnFailureListener { onReady(null) }
    }

    private fun orbsCollection(day: String, cellId: String) =
        FirebaseFirestore.getInstance()
            .collection("dailyFields")
            .document(day)
            .collection("cells")
            .document(cellId)
            .collection("orbs")

    private fun echoesCollection(day: String, cellId: String) =
        FirebaseFirestore.getInstance()
            .collection("dailyFields")
            .document(day)
            .collection("cells")
            .document(cellId)
            .collection("echoes")

    private fun chorusPresencesCollection(day: String) =
        FirebaseFirestore.getInstance()
            .collection("dailyChoruses")
            .document(day)
            .collection("presences")

    private fun buildChorusState(
        day: String,
        localCellId: String,
        presences: List<RemoteChorusPresence>
    ): RemoteChorusState {
        if (presences.isEmpty()) {
            return RemoteChorusState(afterglowSeed = dailyHash("afterglow", day).hashCode())
        }

        val count = presences.size
        val localCount = presences.count { it.coarseCellId == localCellId && localCellId.isNotBlank() }
        val touch = presences.map { it.touchStability }.average().toFloat().coerceIn(0f, 1f)
        val stillness = presences.map { it.stillness }.average().toFloat().coerceIn(0f, 1f)
        val turbulence = presences.map { it.turbulence }.average().toFloat().coerceIn(0f, 1f)
        val density = (localCount / 8f).coerceIn(0f, 1f)
        val sync = (touch * 0.42f + stillness * 0.40f + (1f - turbulence) * 0.18f).coerceIn(0f, 1f)
        val scaleBoost = (count / 24f).coerceIn(0f, 1f)
        val coherence = (sync * 0.72f + scaleBoost * 0.28f).coerceIn(0f, 1f)
        val afterglowSeed = presences
            .fold(dailyHash("afterglow", day).hashCode()) { acc, presence ->
                acc xor presence.clientSeed xor presence.presenceId.hashCode()
            }

        return RemoteChorusState(
            globalPresenceCount = count,
            localFieldDensity = density,
            synchronizationLevel = sync,
            coherence = coherence,
            turbulence = turbulence,
            afterglowSeed = afterglowSeed,
            activePresences = presences
        )
    }

    private fun dailyHash(value: String, day: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest("$day:$value".toByteArray())

        return bytes.take(12).joinToString("") { byte -> "%02x".format(byte) }
    }
}
