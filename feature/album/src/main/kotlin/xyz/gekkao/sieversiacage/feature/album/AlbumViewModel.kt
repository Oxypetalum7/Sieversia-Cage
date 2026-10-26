package xyz.gekkao.sieversiacage.feature.album

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.gekkao.sieversiacage.core.data.AlbumRepository
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import javax.inject.Inject

/** 仮の ViewModel。窓口を画面に渡すだけ。M1 タスク3 で MVI（UiState / Intent / Effect）に書き直す。 */
@HiltViewModel
class AlbumViewModel
    @Inject
    constructor(
        val documents: PlantDocumentRepository,
        val album: AlbumRepository,
    ) : ViewModel()
