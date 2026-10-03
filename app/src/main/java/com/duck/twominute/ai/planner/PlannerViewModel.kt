package com.duck.twominute.ai.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duck.twominute.AppViewModel
import com.duck.twominute.ai.AiSettings
import com.duck.twominute.ai.AiSettingsRepository
import com.duck.twominute.ai.network.FreeModelCatalog
import com.duck.twominute.ai.network.GeminiClient
import com.duck.twominute.ai.network.GeminiErrorKind
import com.duck.twominute.ai.network.GeminiException
import com.duck.twominute.ai.usage.TokenUsageRepository
import com.duck.twominute.ai.usage.UsageSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the AI habit planner: API key and model settings, the Gemini request,
 * the plan preview and importing the proposed habits as identities.
 *
 * The request pipeline is the one of Persian Subtitles (same client, same safety
 * fallback, same quota classification and exact token accounting); only the
 * prompt and the response schema are New Habit's own.
 */
class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AiSettingsRepository(application)
    private val client = GeminiClient()
    private val usageRepository = TokenUsageRepository.get(application)

    enum class ErrorKind { NO_KEY, NOT_CONNECTED, AUTH, QUOTA_MINUTE, QUOTA_DAILY, NETWORK, SERVER, SAFETY, PARSE, UNKNOWN }

    sealed interface Status {
        data object Idle : Status
        data class Working(val revising: Boolean) : Status
        data class Failed(val kind: ErrorKind, val detail: String) : Status
    }

    sealed interface KeyCheck {
        data object Idle : KeyCheck
        data object Checking : KeyCheck
        data class Ok(val models: Int) : KeyCheck
        data class Failed(val kind: ErrorKind, val detail: String) : KeyCheck
    }

    val settings: StateFlow<AiSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AiSettings())

    /**
     * The free models the current key may use.
     *
     * Empty until that key has been saved AND passed a connection test: v1.0.1
     * showed the whole allow-list before any key existed, which looked like the
     * app was connected when it was not.
     */
    val models: StateFlow<List<FreeModelCatalog.Entry>> = repository.settings
        .map { verifiedModels(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val usage: StateFlow<UsageSnapshot?> =
        usageRepository.snapshot.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _keyCheck = MutableStateFlow<KeyCheck>(KeyCheck.Idle)
    val keyCheck: StateFlow<KeyCheck> = _keyCheck.asStateFlow()

    private val _plan = MutableStateFlow<HabitPlan?>(null)
    val plan: StateFlow<HabitPlan?> = _plan.asStateFlow()

    private val _goal = MutableStateFlow("")
    val goal: StateFlow<String> = _goal.asStateFlow()

    /** Indexes of the plan's habits that were already added in this session. */
    private val _added = MutableStateFlow<Set<Int>>(emptySet())
    val added: StateFlow<Set<Int>> = _added.asStateFlow()

    private var lastRequest: PlanRequest? = null
    private var job: Job? = null
    private var checkJob: Job? = null

    init {
        // The last plan survives leaving the screen and restarting the app.
        viewModelScope.launch {
            val stored = repository.settings.first()
            if (_plan.value == null) _plan.value = PlanParser.decodeStored(stored.lastPlan)?.let { PlanParser.sanitize(it) }
            if (_goal.value.isBlank()) _goal.value = stored.lastGoal
            // A key that was saved but never confirmed (e.g. no internet at the
            // time) is tested again quietly, so its models appear once it works.
            if (stored.apiKey.isNotBlank() && !stored.modelsVerified) startCheck(stored.apiKey)
        }
    }

    // ------------------------------------------------------------ settings

    /** Saves the key and immediately tests it; models show only after the test passes. */
    fun saveApiKey(value: String) {
        val key = value.trim()
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            repository.setApiKey(key)
            if (key.isEmpty()) {
                _keyCheck.value = KeyCheck.Idle
            } else {
                checkKey(key)
            }
        }
    }

    fun setModel(id: String) = viewModelScope.launch { repository.setModel(id) }

    /** Tests the key and loads the free models it can use. */
    fun refreshModels() {
        val key = settings.value.apiKey
        if (key.isBlank()) {
            _keyCheck.value = KeyCheck.Failed(ErrorKind.NO_KEY, "")
            return
        }
        startCheck(key)
    }

    private fun startCheck(key: String) {
        checkJob?.cancel()
        checkJob = viewModelScope.launch { checkKey(key) }
    }

    private suspend fun checkKey(key: String) {
        _keyCheck.value = KeyCheck.Checking
        try {
            val free = FreeModelCatalog.filter(client.listModels(key))
            // The user may have replaced the key while this one was being tested.
            if (repository.settings.first().apiKey != key) return
            repository.applyLoadedModels(
                apiKey = key,
                encodedModels = FreeModelCatalog.encode(free),
                preferredModel = FreeModelCatalog.preferredFrom(free),
                offered = free.map { it.id }.toSet(),
            )
            _keyCheck.value = KeyCheck.Ok(free.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GeminiException) {
            val kind = kindOf(e.kind)
            // A rejected key must not keep showing models; a network or server
            // hiccup keeps the list this same key already earned.
            if (kind == ErrorKind.AUTH || kind == ErrorKind.NO_KEY) repository.clearModels()
            _keyCheck.value = KeyCheck.Failed(kind, e.message.orEmpty())
        } catch (e: Exception) {
            _keyCheck.value = KeyCheck.Failed(ErrorKind.UNKNOWN, e.message.orEmpty())
        }
    }

    fun resetUsage() = viewModelScope.launch { usageRepository.resetUsage() }

    fun lastMinute(model: String): Pair<Int, Long> = usageRepository.lastMinute(model)

    // ------------------------------------------------------------ planning

    fun generate(request: PlanRequest) {
        if (request.goal.isBlank()) return
        lastRequest = request.copy(previousPlanJson = null, feedback = "")
        run(request, revising = false)
    }

    /** Asks for a revised version of the current plan, keeping what still fits. */
    fun revise(feedback: String, persianUi: Boolean) {
        val current = _plan.value ?: return
        val base = lastRequest ?: PlanRequest(goal = _goal.value.ifBlank { current.title(persianUi) }, persianUi = persianUi)
        run(base.copy(persianUi = persianUi, previousPlanJson = PlanParser.encode(current), feedback = feedback.trim()), revising = true)
    }

    fun cancel() {
        job?.cancel()
        job = null
        _status.value = Status.Idle
    }

    fun dismissError() {
        if (_status.value is Status.Failed) _status.value = Status.Idle
    }

    fun discardPlan() = viewModelScope.launch {
        _plan.value = null
        _added.value = emptySet()
        repository.clearLastPlan()
    }

    /** Adds the given habits of the current plan to the app as identities. */
    fun addHabits(indexes: List<Int>, app: AppViewModel, onDone: (Int) -> Unit = {}) {
        val current = _plan.value ?: return
        val fresh = indexes.distinct().filter { it in current.habits.indices && it !in _added.value }
        if (fresh.isEmpty()) {
            onDone(0)
            return
        }
        _added.value = _added.value + fresh
        app.addIdentities(fresh.map { current.habits[it].toIdentity() }, onDone)
    }

    private fun run(request: PlanRequest, revising: Boolean) {
        if (job?.isActive == true) return
        val current = settings.value
        val key = current.apiKey
        if (key.isBlank()) {
            _status.value = Status.Failed(ErrorKind.NO_KEY, "")
            return
        }
        val available = verifiedModels(current)
        if (available.isEmpty()) {
            // Key saved but not confirmed yet: test it, and ask the user to retry.
            _status.value = Status.Failed(ErrorKind.NOT_CONNECTED, "")
            if (_keyCheck.value !is KeyCheck.Checking) startCheck(key)
            return
        }
        val model = available.firstOrNull { it.id == current.model }?.id
            ?: FreeModelCatalog.preferredFrom(available)
            ?: return
        job = viewModelScope.launch {
            _status.value = Status.Working(revising)
            try {
                val raw = requestWithRetry(key, model, PlanPrompt.userPayload(request))
                val parsed = PlanParser.parse(raw)
                if (parsed == null) {
                    _status.value = Status.Failed(ErrorKind.PARSE, "")
                    return@launch
                }
                _plan.value = parsed
                _added.value = emptySet()
                _goal.value = request.goal
                repository.setLastPlan(request.goal, PlanParser.encode(parsed))
                _status.value = Status.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: GeminiException) {
                _status.value = Status.Failed(kindOf(e.kind), e.message.orEmpty())
            } catch (e: Exception) {
                _status.value = Status.Failed(ErrorKind.UNKNOWN, e.message.orEmpty())
            }
        }
    }

    /** Short-lived problems (per-minute limit, server hiccup, network) get two more tries. */
    private suspend fun requestWithRetry(key: String, model: String, payload: String): String {
        var attempt = 0
        while (true) {
            try {
                return client.generateStructured(
                    apiKey = key,
                    model = model,
                    systemInstruction = PlanPrompt.systemInstruction(),
                    userPayload = payload,
                    responseSchema = PlanPrompt.schema(),
                    temperature = 0.6f,
                    maxOutputTokens = GeminiClient.PLAN_TOKENS,
                )
            } catch (e: GeminiException) {
                attempt += 1
                if (!e.kind.retryable || attempt > MAX_RETRIES) throw e
                val waitSeconds = (e.retryAfterSeconds ?: (3 * attempt)).coerceIn(1, MAX_WAIT_SECONDS)
                delay(waitSeconds * 1000L)
            }
        }
    }

    private fun kindOf(kind: GeminiErrorKind): ErrorKind = when (kind) {
        GeminiErrorKind.NO_KEY -> ErrorKind.NO_KEY
        GeminiErrorKind.AUTH -> ErrorKind.AUTH
        GeminiErrorKind.QUOTA -> ErrorKind.QUOTA_MINUTE
        GeminiErrorKind.QUOTA_DAILY -> ErrorKind.QUOTA_DAILY
        GeminiErrorKind.SERVER -> ErrorKind.SERVER
        GeminiErrorKind.NETWORK -> ErrorKind.NETWORK
        GeminiErrorKind.PARSE -> ErrorKind.PARSE
        GeminiErrorKind.SAFETY -> ErrorKind.SAFETY
        GeminiErrorKind.UNKNOWN -> ErrorKind.UNKNOWN
    }

    private companion object {
        const val MAX_RETRIES = 2
        const val MAX_WAIT_SECONDS = 30

        fun verifiedModels(settings: AiSettings): List<FreeModelCatalog.Entry> =
            if (settings.modelsVerified) FreeModelCatalog.decode(settings.cachedModels) else emptyList()
    }
}
