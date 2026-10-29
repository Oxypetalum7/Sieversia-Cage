package xyz.gekkao.sieversiacage.feature.showcase

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.gekkao.sieversiacage.core.data.AlbumRepository
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import xyz.gekkao.sieversiacage.core.player.PlantDocument
import xyz.gekkao.sieversiacage.core.player.dumpDocument
import javax.inject.Inject

private const val TAG = "Showcase"
private const val WATERING_TAG = "Watering"
private const val KEY_AGE_DAYS = "ageDays"

/**
 * 選んだ日の株をアルバムに記録 → 読み直して表示する（スパイク①④の流れ）。
 * 表示するのは常に「ファイルから読み直したバイト列」。
 */
@HiltViewModel
class ShowcaseViewModel
    @Inject
    constructor(
        private val documents: PlantDocumentRepository,
        private val album: AlbumRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                ShowcaseUiState(
                    ageDays = savedStateHandle[KEY_AGE_DAYS] ?: 0,
                    document = null,
                    documentSizeDp = documents.documentSizeDp,
                    allowedShaders = documents.allowedShaders,
                ),
            )
        val uiState: StateFlow<ShowcaseUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            show(_uiState.value.ageDays)
        }

        fun onIntent(intent: ShowcaseIntent) {
            val state = _uiState.value
            when (intent) {
                ShowcaseIntent.NextDay -> show(state.ageDays + 1)
                ShowcaseIntent.PreviousDay -> if (state.ageDays > 0) show(state.ageDays - 1)
                ShowcaseIntent.DumpDocument -> dump(state)
                is ShowcaseIntent.ReceiveHostAction -> onHostAction(intent)
            }
        }

        // M2-1 の検証では、届いたことをログに出すだけ。記録は Sim の本実装（M2-4）で作る
        private fun onHostAction(action: ShowcaseIntent.ReceiveHostAction) {
            if (action.actionId != documents.waterActionId) return
            Timber.tag(WATERING_TAG).i("in-app tap received: metadata=${action.metadata}")
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
                    val generated = album.record(ageDays)
                    val reloaded = album.document(ageDays)
                    Timber
                        .tag(TAG)
                        .i("recorded day-$ageDays ${generated.size}B, roundTrip=${reloaded?.contentEquals(generated)}")
                    _uiState.update { it.copy(document = reloaded?.let(::PlantDocument)) }
                }
        }
    }
