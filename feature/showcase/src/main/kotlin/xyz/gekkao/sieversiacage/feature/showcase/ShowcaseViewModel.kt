package xyz.gekkao.sieversiacage.feature.showcase

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.gekkao.sieversiacage.core.data.GrowthRepository
import xyz.gekkao.sieversiacage.core.data.GrowthTicker
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import xyz.gekkao.sieversiacage.core.data.ReminderNotifier
import xyz.gekkao.sieversiacage.core.data.WidgetRefresher
import xyz.gekkao.sieversiacage.core.player.PlantDocument
import xyz.gekkao.sieversiacage.core.player.dumpDocument
import javax.inject.Inject

private const val TAG = "Showcase"
private const val WATERING_TAG = "Watering"
private const val KEY_AGE_DAYS = "ageDays"

/**
 * 今日の本物の株を見せる（ADR-0021）。今日の分はアルバムのファイルから読む（スパイク④の「読み直したバイト列を再生する」）。
 * 「前の日 / 次の日」は、毎日水をやった前提の先の姿のプレビューで、アルバムには書かない。
 */
@HiltViewModel
class ShowcaseViewModel
    @Inject
    constructor(
        private val documents: PlantDocumentRepository,
        private val growth: GrowthRepository,
        private val ticker: GrowthTicker,
        private val widgets: WidgetRefresher,
        private val notifier: ReminderNotifier,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                ShowcaseUiState(
                    ageDays = savedStateHandle[KEY_AGE_DAYS] ?: 0,
                    todayAgeDays = null,
                    document = null,
                    documentSizeDp = documents.documentSizeDp,
                    allowedShaders = documents.allowedShaders,
                ),
            )
        val uiState: StateFlow<ShowcaseUiState> = _uiState.asStateFlow()

        private val _effects = Channel<ShowcaseEffect>(Channel.BUFFERED)
        val effects: Flow<ShowcaseEffect> = _effects.receiveAsFlow()

        private var loadJob: Job? = null

        init {
            viewModelScope.launch {
                // 今日の分がまだアルバムになければ、ここで進める（tick より先に開いたとき）
                val today = ticker.today()
                _uiState.update { it.copy(todayAgeDays = today.ageDays) }
                show(savedStateHandle[KEY_AGE_DAYS] ?: today.ageDays)
                refreshReminderPrompt()
            }
        }

        fun onIntent(intent: ShowcaseIntent) {
            val state = _uiState.value
            val today = state.todayAgeDays ?: return
            when (intent) {
                ShowcaseIntent.NextDay -> show(state.ageDays + 1)
                ShowcaseIntent.PreviousDay -> if (state.ageDays > 0) show(state.ageDays - 1)
                ShowcaseIntent.BackToToday -> show(today)
                ShowcaseIntent.DumpDocument -> dump(state)
                ShowcaseIntent.AcceptReminder -> _effects.trySend(ShowcaseEffect.RequestNotificationPermission)
                ShowcaseIntent.DeclineReminder, is ShowcaseIntent.ReportReminderPermission -> closeReminderPrompt()
                is ShowcaseIntent.ReceiveHostAction -> onHostAction(intent)
            }
        }

        // 跳ねる反応はドキュメントの中で返っているので、ここでは水やりの記録だけをする（ADR-0014 / ADR-0020）
        private fun onHostAction(action: ShowcaseIntent.ReceiveHostAction) {
            if (action.actionId != documents.waterActionId) return
            viewModelScope.launch {
                growth.recordWatering()
                Timber.tag(WATERING_TAG).i("in-app tap recorded")
                // ADR-0021: アプリで水をやっても、ウィジェットに「今日の水やり: 済み」を出す
                widgets.refresh()
                // 初めて水をやったら、通知の案内を出す
                refreshReminderPrompt()
            }
        }

        private suspend fun refreshReminderPrompt() {
            val show = notifier.shouldAskPermission()
            _uiState.update { it.copy(showReminderPrompt = show) }
        }

        // 許可・拒否・「今はいい」のどれでも、もう案内は出さない
        private fun closeReminderPrompt() {
            notifier.markPermissionAsked()
            _uiState.update { it.copy(showReminderPrompt = false) }
        }

        private fun dump(state: ShowcaseUiState) {
            state.document?.let { Timber.tag(TAG).i("dump day-${state.ageDays}.rc\n" + dumpDocument(it)) }
        }

        // 日を続けて進めたときは、前の日の読込を止めて最後の日だけを表示する
        private fun show(ageDays: Int) {
            savedStateHandle[KEY_AGE_DAYS] = ageDays
            _uiState.update { it.copy(ageDays = ageDays) }
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    val bytes =
                        if (ageDays == _uiState.value.todayAgeDays) {
                            ticker.today().document
                        } else {
                            documents.inAppPreviewDocument(growth.plantedOn(), ageDays)
                        }
                    _uiState.update { it.copy(document = bytes?.let(::PlantDocument)) }
                }
        }
    }
