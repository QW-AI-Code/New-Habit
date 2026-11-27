package com.duck.twominute

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duck.twominute.alarms.AlarmScheduler
import com.duck.twominute.alarms.ReminderAlarmScheduler
import com.duck.twominute.alarms.ReminderNotifier
import com.duck.twominute.reminders.Reminder
import com.duck.twominute.reminders.ReminderSchedule
import java.time.LocalDate
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
    private val reminderScheduler = ReminderAlarmScheduler(application)
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

    // ---------------------------------------------------------------- reminders (v1.1.0)

    /**
     * Inserts or updates a standalone reminder and arms its next ring. Saving counts
     * as "handled up to now", so a daily reminder whose time already passed today is
     * not reported as missed after a reboot. [onDone] receives the next ring (epoch
     * millis) or null when the reminder has no future ring.
     */
    fun saveReminder(raw: Reminder, onDone: (Long?) -> Unit = {}) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        var saved: Reminder? = null
        store.update { current ->
            val exists = raw.id != 0L && current.reminders.any { it.id == raw.id }
            val id = if (exists) raw.id else maxOf(now, (current.reminders.maxOfOrNull { it.id } ?: 0L) + 1)
            val clean = raw.copy(
                id = id,
                title = raw.title.trim(),
                note = raw.note.trim(),
                weekdays = raw.weekdays.filter { it in 1..7 }.distinct().sorted(),
                intervalMinutes = if (raw.intervalMinutes <= 0) 0 else raw.intervalMinutes.coerceIn(ReminderSchedule.MIN_INTERVAL, ReminderSchedule.MAX_INTERVAL),
                untilEpochDay = raw.untilEpochDay?.takeIf { it >= raw.startEpochDay },
                doneEpochDays = ReminderSchedule.pruneDoneDays(raw.doneEpochDays, LocalDate.now()),
                enabled = true,
                lastHandledAt = now,
                createdAt = if (raw.createdAt > 0L) raw.createdAt else now
            )
            saved = clean
            if (exists) {
                current.copy(reminders = current.reminders.map { if (it.id == id) clean else it })
            } else {
                current.copy(reminders = current.reminders + clean)
            }
        }
        val written = saved
        val next = if (written == null) null else {
            ReminderNotifier.cancel(getApplication<Application>(), written.id)
            reminderScheduler.cancelSnooze(written.id)
            reminderScheduler.schedule(written, now)
        }
        onDone(next)
    }

    fun deleteReminder(id: Long) = viewModelScope.launch {
        reminderScheduler.cancel(id)
        ReminderNotifier.cancel(getApplication<Application>(), id)
        store.update { current -> current.copy(reminders = current.reminders.filterNot { it.id == id }) }
    }

    fun setReminderEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val written = store.update { current ->
            current.copy(
                reminders = current.reminders.map {
                    // Switching on again starts from now: rings of the switched-off time are not replayed.
                    if (it.id == id) it.copy(enabled = enabled, lastHandledAt = if (enabled) now else it.lastHandledAt) else it
                }
            )
        }
        val reminder = written.reminders.firstOrNull { it.id == id } ?: return@launch
        if (enabled) {
            reminderScheduler.schedule(reminder, now)
        } else {
            reminderScheduler.cancel(id)
            ReminderNotifier.cancel(getApplication<Application>(), id)
        }
    }

    /** Re-enables "the rest of today" after a mistaken "Done for today". */
    fun clearReminderDoneToday(id: Long) = viewModelScope.launch {
        val today = LocalDate.now().toEpochDay()
        val written = store.update { current ->
            current.copy(reminders = current.reminders.map { if (it.id == id) it.copy(doneEpochDays = it.doneEpochDays - today) else it })
        }
        written.reminders.firstOrNull { it.id == id }?.let { reminderScheduler.schedule(it) }
    }

    /** Fires a test notification through AlarmManager in a few seconds. */
    fun sendTestReminder(): Long = reminderScheduler.scheduleTest()

    fun canScheduleExactReminders(): Boolean = reminderScheduler.canScheduleExact()

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
        // Alarms of identities and reminders that are not in the backup would otherwise keep firing.
        val before = store.state.first()
        before.identities.forEach { scheduler.cancel(it.id) }
        before.reminders.forEach { reminderScheduler.cancel(it.id) }
        // Restored reminders start from now: rings between the backup and today are not replayed.
        val now = System.currentTimeMillis()
        val restored = decoded.copy(reminders = decoded.reminders.map { it.copy(lastHandledAt = maxOf(it.lastHandledAt, now)) })
        store.save(restored)
        restored.identities
            .filter { it.hasReminder() && !it.archived }
            .forEach { scheduler.schedule(it) }
        restored.reminders.forEach { reminderScheduler.schedule(it, now) }
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
