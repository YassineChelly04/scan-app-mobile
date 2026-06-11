package com.scanni.app.core.geometry

/**
 * Temporal smoothing and steadiness tracking for live document detection.
 *
 * Feed one detection result per camera frame; the stabilizer exponentially smooths
 * the quad (so the overlay glides instead of jittering), tolerates short detection
 * dropouts, and reports a steadiness progress that reaches [State.Locked] once the
 * document has been still for [Config.steadyDurationMs] — the auto-capture trigger.
 */
class QuadStabilizer(private val config: Config = Config()) {

    data class Config(
        /** Weight of the newest sample in the exponential moving average. */
        val smoothing: Float = 0.45f,
        /** Per-frame corner movement (normalized units) below which the frame counts as steady. */
        val steadyMaxDelta: Float = 0.012f,
        /** How long the quad must stay steady before locking. */
        val steadyDurationMs: Long = 1100,
        /** Keep tracking through detection dropouts shorter than this. */
        val lostGraceMs: Long = 400,
    )

    sealed interface State {
        data object Searching : State
        data class Tracking(val quad: Quad, val steadyProgress: Float) : State
        data class Locked(val quad: Quad) : State
    }

    private var smoothed: Quad? = null
    private var steadySince: Long = -1
    private var lastSeenAt: Long = -1
    private var locked = false

    fun onFrame(detected: Quad?, nowMs: Long): State {
        if (detected == null) {
            val current = smoothed
            if (current != null && lastSeenAt >= 0 && nowMs - lastSeenAt <= config.lostGraceMs) {
                return stateAt(nowMs)
            }
            reset()
            return State.Searching
        }

        lastSeenAt = nowMs
        val previous = smoothed
        val next = previous?.lerp(detected, config.smoothing) ?: detected
        smoothed = next

        val delta = previous?.maxCornerDistance(next) ?: Float.MAX_VALUE
        if (delta > config.steadyMaxDelta) {
            steadySince = nowMs
        } else if (steadySince < 0) {
            steadySince = nowMs
        }
        return stateAt(nowMs)
    }

    /** Clears all tracking state, e.g. when the camera mode changes. */
    fun reset() {
        smoothed = null
        steadySince = -1
        lastSeenAt = -1
        locked = false
    }

    /** Unlocks after a capture so the next page can be detected, keeping the smoothed quad. */
    fun rearm() {
        locked = false
        steadySince = -1
    }

    private fun stateAt(nowMs: Long): State {
        val quad = smoothed ?: return State.Searching
        if (locked) return State.Locked(quad)
        val progress = if (steadySince < 0) {
            0f
        } else {
            ((nowMs - steadySince).toFloat() / config.steadyDurationMs).coerceIn(0f, 1f)
        }
        if (progress >= 1f) {
            locked = true
            return State.Locked(quad)
        }
        return State.Tracking(quad, progress)
    }
}
