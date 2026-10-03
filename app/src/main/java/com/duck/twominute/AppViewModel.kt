package com.duck.twominute

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duck.twominute.alarms.AlarmScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AppStore(application)
    private val scheduler = AlarmScheduler(application)
    private val feedback = Feedback(application)

    val state: StateFlow<AppState> =
        store.state.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    private val _totalSeconds = MutableStateFlow(Identity.DEFAULT_DURATION)
    val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

    private val _secondsLeft = MutableStateFlow(Identity.DEFAULT_DURATION)
    val secondsLeft: StateFlow<Int> = _secondsLeft.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    private val _justCompleted = MutableStateFlow(false)
    val justCompleted: StateFlow<Boolean> = _justCompleted.asStateFlow()

    private var timerJob: Job? = null
    private var deadline: Long = 0L
    private var activeIdentityId: Long? = null

    // ---------------------------------------------------------------- identities

    /** Insert or update in one call, so the editor can be reused for both. */
    fun saveIdentity(raw: Identity) = edit { current ->
        // Enforce the reminder invariant on every save: one slot per repetition.
        val identity = raw.withReminders(raw.reminderSlots())
        val exists = current.identities.any { it.id == identity.id }
        scheduler.schedule(identity)
        if (exists) {
            current.copy(
                identities = current.identities.map { if (it.id == identity.id) identity else it }
            )
        } else {
            current.copy(identities = current.identities + identity)
        }
    }

    /**
     * Adds the identities the AI planner proposed. Ids are made unique even when
     * several are created within the same millisecond.
     */
    fun addIdentities(newOnes: List<Identity>, onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        if (newOnes.isEmpty()) {
            onDone(0)
            return@launch
        }
        var added = 0
        store.update { current ->
            var nextId = maxOf(System.currentTimeMillis(), (current.identities.maxOfOrNull { it.id } ?: 0L) + 1)
            val prepared = newOnes.map { candidate ->
                val withId = candidate.copy(id = nextId)
                nextId += 1
                withId.withReminders(withId.reminderSlots())
            }
            prepared.forEach { scheduler.schedule(it) }
            added = prepared.size
            current.copy(identities = current.identities + prepared)
        }
        onDone(added)
    }

    fun deleteIdentity(id: Long) = edit { current ->
        scheduler.cancel(id)
        current.copy(identities = current.identities.filterNot { it.id == id }, currentIndex = 0)
    }

    fun toggleArchive(id: Long) = edit { current ->
        val updated = current.identities.map {
            if (it.id == id) it.copy(archived = !it.archived) else it
        }
        updated.firstOrNull { it.id == id }?.let { changed ->
            if (changed.archived) scheduler.cancel(id) else scheduler.schedule(changed)
        }
        current.copy(identities = updated, currentIndex = 0)
    }

    fun setDuration(id: Long, seconds: Int) = edit { current ->
        current.copy(
            identities = current.identities.map {
                if (it.id == id) {
                    it.copy(
                        durationSeconds = seconds.coerceIn(
                            Identity.MIN_DURATION,
                            Identity.MAX_DURATION
                        )
                    )
                } else {
                    it
                }
            }
        )
    }

    fun moveIdentity(id: Long, delta: Int) = edit { current ->
        val list = current.identities.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        if (from < 0) return@edit current
        val to = (from + delta).coerceIn(0, list.size - 1)
        if (to == from) return@edit current
        val moved = list.removeAt(from)
        list.add(to, moved)
        current.copy(identities = list)
    }

    fun selectIdentity(id: Long) = edit { current ->
        val index = current.active.indexOfFirst { it.id == id }
        if (index < 0) current else current.copy(currentIndex = index)
    }

    fun skipToNext() = edit { current ->
        val size = current.active.size
        if (size == 0) current else current.copy(currentIndex = (current.currentIndex + 1) % size)
    }

    // ---------------------------------------------------------------- settings

    fun setLanguage(language: String) = edit { it.copy(language = language) }

    fun setDefaultDuration(seconds: Int) = edit {
        it.copy(
            defaultDurationSeconds = seconds.coerceIn(
                Identity.MIN_DURATION,
                Identity.MAX_DURATION
            )
        )
    }

    fun setDailyGoal(goal: Int) = edit { it.copy(dailyGoal = goal.coerceIn(1, 20)) }

    fun setVibrate(enabled: Boolean) = edit { it.copy(vibrate = enabled) }

    fun setChime(enabled: Boolean) = edit { it.copy(chime = enabled) }

    fun setKeepScreenOn(enabled: Boolean) = edit { it.copy(keepScreenOn = enabled) }

    fun setShowArchived(enabled: Boolean) = edit { it.copy(showArchived = enabled) }

    // ---------------------------------------------------------------- backup

    fun exportJson(onReady: (String) -> Unit) = viewModelScope.launch {
        onReady(store.encode(store.state.first()))
    }

    fun importJson(raw: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val decoded = store.decode(raw)
        if (decoded == null) {
            onResult(false)
            return@launch
        }
        // Alarms of identities that are not in the backup would otherwise keep firing.
        store.state.first().identities.forEach { scheduler.cancel(it.id) }
        store.save(decoded)
        decoded.identities
            .filter { it.hasReminder() && !it.archived }
            .forEach { scheduler.schedule(it) }
        onResult(true)
    }

    // ---------------------------------------------------------------- the timer

    /** Keeps the idle dial in sync with whatever length the identity carries. */
    fun prepare(seconds: Int) {
        if (_running.value) return
        val safe = seconds.coerceIn(Identity.MIN_DURATION, Identity.MAX_DURATION)
        _totalSeconds.value = safe
        _secondsLeft.value = safe
    }

    fun start(identity: Identity) {
        if (_running.value) return
        val seconds = identity.safeDuration
        activeIdentityId = identity.id
        _justCompleted.value = false
        _totalSeconds.value = seconds
        _secondsLeft.value = seconds
        _paused.value = false
        _running.value = true
        deadline = System.currentTimeMillis() + seconds * 1000L
        feedback.tap(state.value.vibrate)
        loop()
    }

    fun pause() {
        if (!_running.value || _paused.value) return
        timerJob?.cancel()
        timerJob = null
        _secondsLeft.value = remainingSeconds()
        _paused.value = true
    }

    fun resume() {
        if (!_running.value || !_paused.value) return
        deadline = System.currentTimeMillis() + _secondsLeft.value * 1000L
        _paused.value = false
        loop()
    }

    fun addSeconds(extra: Int) {
        _totalSeconds.value = (_totalSeconds.value + extra).coerceIn(
            Identity.MIN_DURATION,
            Identity.MAX_DURATION
        )
        if (_running.value && !_paused.value) {
            deadline += extra * 1000L
            _secondsLeft.value = remainingSeconds()
        } else {
            _secondsLeft.value = (_secondsLeft.value + extra).coerceIn(0, Identity.MAX_DURATION)
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        _running.value = false
        _paused.value = false
        _secondsLeft.value = _totalSeconds.value
    }

    fun acknowledgeCompletion() {
        _justCompleted.value = false
    }

    /** Ticks off a ritual that happened away from the phone. */
    fun logNow(identityId: Long) {
        _justCompleted.value = true
        val snapshot = state.value
        feedback.celebrate(snapshot.vibrate, snapshot.chime)
        stamp(identityId)
    }

    fun undoLast(identityId: Long) = edit { current ->
        current.copy(
            identities = current.identities.map { identity ->
                if (identity.id != identityId || identity.successes.isEmpty()) {
                    identity
                } else {
                    val latest = identity.successes.max()
                    identity.copy(successes = identity.successes.filterNot { it == latest })
                }
            }
        )
    }

    private fun remainingSeconds(): Int {
        val millis = deadline - System.currentTimeMillis()
        if (millis <= 0L) return 0
        return ceil(millis / 1000.0).toInt()
    }

    private fun loop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = remainingSeconds()
                if (remaining <= 0) {
                    _secondsLeft.value = 0
                    finish()
                    return@launch
                }
                _secondsLeft.value = remaining
                delay(200L)
            }
        }
    }

    private fun finish() {
        timerJob = null
        _running.value = false
        _paused.value = false
        _secondsLeft.value = _totalSeconds.value
        _justCompleted.value = true
        val snapshot = state.value
        feedback.celebrate(snapshot.vibrate, snapshot.chime)
        stamp(activeIdentityId ?: snapshot.current()?.id ?: return)
    }

    private fun stamp(identityId: Long) = edit { current ->
        if (current.identities.none { it.id == identityId }) return@edit current
        val stamped = current.identities.map {
            if (it.id == identityId) it.copy(successes = it.successes + System.currentTimeMillis()) else it
        }
        val activeCount = stamped.count { !it.archived }
        val nextIndex = if (activeCount == 0) 0 else (current.currentIndex + 1) % activeCount
        current.copy(identities = stamped, currentIndex = nextIndex)
    }

    // One atomic DataStore transaction: no write can be based on a stale snapshot.
    private fun edit(transform: (AppState) -> AppState): Job = viewModelScope.launch {
        store.update(transform)
    }
}
