package xyz.gekkao.sieversiacage.feature.album

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
import javax.inject.Inject

private const val TAG = "Album"
private const val KEY_SELECTED_DAY = "selectedDay"

/**
 * 記録した日を一覧し、選んだ日を再生する。
 * プレイヤーが読めなかった日は、保存した Sim の出力から 1 回だけ作り直す（ADR-0011）。
 */
@HiltViewModel
class AlbumViewModel
    @Inject
    constructor(
        documents: PlantDocumentRepository,
        private val album: AlbumRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                AlbumUiState(
                    days = emptyList(),
                    selectedDay = null,
                    document = null,
                    isUnreadable = false,
                    documentSizeDp = documents.documentSizeDp,
                    allowedShaders = documents.allowedShaders,
                ),
            )
        val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

        // 作り直しても読めないときに繰り返さないよう、作り直した日を覚えておく
        private val regeneratedDays = mutableSetOf<Int>()
        private var loadJob: Job? = null

        init {
            savedStateHandle.get<Int>(KEY_SELECTED_DAY)?.let(::select)
            viewModelScope.launch {
                album.days().collect { days ->
                    _uiState.update { it.copy(days = days) }
                    // 最初は最新の日を選ぶ。そのあと日が増えても、選んでいる日は変えない
                    if (_uiState.value.selectedDay == null) days.lastOrNull()?.let(::select)
                }
            }
        }

        fun onIntent(intent: AlbumIntent) {
            when (intent) {
                is AlbumIntent.SelectDay -> select(intent.day)
                is AlbumIntent.ReportLoadFailure -> onLoadFailure(intent.day, intent.error)
            }
        }

        private fun select(day: Int) {
            savedStateHandle[KEY_SELECTED_DAY] = day
            _uiState.update { it.copy(selectedDay = day, isUnreadable = false) }
            load(day) { album.document(day) }
        }

        private fun onLoadFailure(
            day: Int,
            error: Throwable,
        ) {
            Timber.tag(TAG).w(error, "day-$day を読めなかった")
            if (day != _uiState.value.selectedDay) return
            if (regeneratedDays.add(day)) {
                load(day) { album.regenerate(day) }
            } else {
                _uiState.update { it.copy(document = null, isUnreadable = true) }
            }
        }

        // 日を続けて選んだときは、前の日の読込を止めて最後の日だけを表示する
        private fun load(
            day: Int,
            read: suspend () -> ByteArray?,
        ) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    val bytes = read()
                    _uiState.update {
                        if (it.selectedDay == day) it.copy(document = bytes?.let(::PlantDocument)) else it
                    }
                }
        }
    }
