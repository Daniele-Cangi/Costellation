package app.constellationpulse.backend

import android.content.Context
import app.constellationpulse.data.ChorusRelic
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

data class RemoteSealState(
    val globalSealCount: Int = 0,
    val localSealDensity: Float = 0f,
    val valence: Int = 50,
    val arousal: Int = 50,
    val energy: Int = 50,
    val focus: Int = 50,
    val social: Int = 50,
    val afterglowSeed: Int = 0,
    val sealedOrbs: List<RemoteFieldOrb> = emptyList()
)

data class RemoteChorusPresence(
    val presenceId: String,
    val day: String,
    val coarseCellId: String,
    val valence: Int,
    val arousal: Int,
    val energy: Int,
    val focus: Int,
    val social: Int,
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
    val valence: Int = 50,
    val arousal: Int = 50,
    val energy: Int = 50,
    val focus: Int = 50,
    val social: Int = 50,
    val afterglowSeed: Int = 0,
    val activePresences: List<RemoteChorusPresence> = emptyList()
)

data class RemoteChorusRelic(
    val day: String,
    val afterglowSeed: Int,
    val globalPresenceCount: Int,
    val localFieldDensity: Float,
    val synchronizationLevel: Float,
    val coherence: Float,
    val turbulence: Float,
    val sealedAtMillis: Long
) {
    fun toChorusRelic(): ChorusRelic {
        return ChorusRelic(
            day = day,
            createdAtMillis = sealedAtMillis,
            afterglowSeed = afterglowSeed,
            globalPresenceCount = globalPresenceCount,
            localFieldDensity = localFieldDensity,
            synchronizationLevel = synchronizationLevel,
            coherence = coherence,
            turbulence = turbulence
        )
    }
}

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
                "cellId" to cellId,
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
                .addOnSuccessListener {
                    dailySealOrbDocument(pulse.dateKey, orbId)
                        .set(data, SetOptions.merge())
                        .addOnCompleteListener { onComplete(true) }
                }
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

    fun listenDailySealState(
        day: String,
        localCellId: String,
        onUpdate: (RemoteSealState) -> Unit,
        onError: () -> Unit = {}
    ): ListenerRegistration? {
        if (!isConfigured) {
            return null
        }

        return dailySealOrbsCollection(day)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(240)
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
                        RemoteFieldOrb(
                            orbId = orbId,
                            day = document.getString("day") ?: day,
                            cellId = document.getString("cellId") ?: "",
                            valence = document.getLong("valence")?.toInt()?.coerceIn(0, 100) ?: 50,
                            arousal = document.getLong("arousal")?.toInt()?.coerceIn(0, 100) ?: 50,
                            energy = document.getLong("energy")?.toInt()?.coerceIn(0, 100) ?: 50,
                            focus = document.getLong("focus")?.toInt()?.coerceIn(0, 100) ?: 50,
                            social = document.getLong("social")?.toInt()?.coerceIn(0, 100) ?: 50,
                            createdAtMillis = document.getLong("createdAtMillis") ?: 0L
                        )
                    }

                onUpdate(buildSealState(day, localCellId, orbs))
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
        valence: Int = 50,
        arousal: Int = 50,
        energy: Int = 50,
        focus: Int = 50,
        social: Int = 50,
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
                "valence" to valence.coerceIn(0, 100),
                "arousal" to arousal.coerceIn(0, 100),
                "energy" to energy.coerceIn(0, 100),
                "focus" to focus.coerceIn(0, 100),
                "social" to social.coerceIn(0, 100),
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
                            valence = document.getLong("valence")?.toInt()?.coerceIn(0, 100) ?: 50,
                            arousal = document.getLong("arousal")?.toInt()?.coerceIn(0, 100) ?: 50,
                            energy = document.getLong("energy")?.toInt()?.coerceIn(0, 100) ?: 50,
                            focus = document.getLong("focus")?.toInt()?.coerceIn(0, 100) ?: 50,
                            social = document.getLong("social")?.toInt()?.coerceIn(0, 100) ?: 50,
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

    fun publishSharedChorusRelic(
        relic: ChorusRelic,
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

            val data = mapOf(
                "day" to relic.day,
                "afterglowSeed" to relic.afterglowSeed,
                "globalPresenceCount" to relic.globalPresenceCount.coerceAtLeast(0),
                "localFieldDensity" to relic.localFieldDensity.coerceIn(0f, 1f),
                "synchronizationLevel" to relic.synchronizationLevel.coerceIn(0f, 1f),
                "coherence" to relic.coherence.coerceIn(0f, 1f),
                "turbulence" to relic.turbulence.coerceIn(0f, 1f),
                "sealedAtMillis" to System.currentTimeMillis(),
                "sealedBy" to dailyHash("relic:$uid", relic.day)
            )

            chorusAfterglowDocument(relic.day)
                .set(data, SetOptions.merge())
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun listenSharedChorusRelic(
        day: String,
        onUpdate: (RemoteChorusRelic?) -> Unit,
        onError: () -> Unit = {}
    ): ListenerRegistration? {
        if (!isConfigured) {
            return null
        }

        return chorusAfterglowDocument(day)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError()
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    onUpdate(null)
                    return@addSnapshotListener
                }

                onUpdate(
                    RemoteChorusRelic(
                        day = snapshot.getString("day") ?: day,
                        afterglowSeed = snapshot.getLong("afterglowSeed")?.toInt()
                            ?: dailyHash("afterglow", day).hashCode(),
                        globalPresenceCount = snapshot.getLong("globalPresenceCount")?.toInt()?.coerceAtLeast(0) ?: 0,
                        localFieldDensity = snapshot.getDouble("localFieldDensity")?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
                        synchronizationLevel = snapshot.getDouble("synchronizationLevel")?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
                        coherence = snapshot.getDouble("coherence")?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
                        turbulence = snapshot.getDouble("turbulence")?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
                        sealedAtMillis = snapshot.getLong("sealedAtMillis") ?: 0L
                    )
                )
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

    private fun dailySealOrbsCollection(day: String) =
        FirebaseFirestore.getInstance()
            .collection("dailySeals")
            .document(day)
            .collection("orbs")

    private fun dailySealOrbDocument(day: String, orbId: String) =
        dailySealOrbsCollection(day).document(orbId)

    private fun chorusPresencesCollection(day: String) =
        FirebaseFirestore.getInstance()
            .collection("dailyChoruses")
            .document(day)
            .collection("presences")

    private fun chorusAfterglowDocument(day: String) =
        FirebaseFirestore.getInstance()
            .collection("dailyChoruses")
            .document(day)
            .collection("afterglow")
            .document("relic")

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
        val valence = presences.map { it.valence }.average().toInt().coerceIn(0, 100)
        val arousal = presences.map { it.arousal }.average().toInt().coerceIn(0, 100)
        val energy = presences.map { it.energy }.average().toInt().coerceIn(0, 100)
        val focus = presences.map { it.focus }.average().toInt().coerceIn(0, 100)
        val social = presences.map { it.social }.average().toInt().coerceIn(0, 100)
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
            valence = valence,
            arousal = arousal,
            energy = energy,
            focus = focus,
            social = social,
            afterglowSeed = afterglowSeed,
            activePresences = presences
        )
    }

    private fun buildSealState(
        day: String,
        localCellId: String,
        orbs: List<RemoteFieldOrb>
    ): RemoteSealState {
        if (orbs.isEmpty()) {
            return RemoteSealState(afterglowSeed = dailyHash("sealed", day).hashCode())
        }

        val localCount = orbs.count { it.cellId == localCellId && localCellId.isNotBlank() }
        val afterglowSeed = orbs
            .fold(dailyHash("sealed", day).hashCode()) { acc, orb ->
                acc xor orb.orbId.hashCode() xor orb.createdAtMillis.toInt()
            }

        return RemoteSealState(
            globalSealCount = orbs.size,
            localSealDensity = (localCount / 8f).coerceIn(0f, 1f),
            valence = orbs.map { it.valence }.average().toInt().coerceIn(0, 100),
            arousal = orbs.map { it.arousal }.average().toInt().coerceIn(0, 100),
            energy = orbs.map { it.energy }.average().toInt().coerceIn(0, 100),
            focus = orbs.map { it.focus }.average().toInt().coerceIn(0, 100),
            social = orbs.map { it.social }.average().toInt().coerceIn(0, 100),
            afterglowSeed = afterglowSeed,
            sealedOrbs = orbs
        )
    }

    private fun dailyHash(value: String, day: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest("$day:$value".toByteArray())

        return bytes.take(12).joinToString("") { byte -> "%02x".format(byte) }
    }
}
