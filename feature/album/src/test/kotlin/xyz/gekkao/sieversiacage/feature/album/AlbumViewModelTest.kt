package xyz.gekkao.sieversiacage.feature.album

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import xyz.gekkao.sieversiacage.core.data.AlbumRepository
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import xyz.gekkao.sieversiacage.core.player.PlantDocument

class AlbumViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val documents =
        mockk<PlantDocumentRepository> {
            every { documentSizeDp } returns 320
            every { allowedShaders } returns emptySet()
        }

    private val days = MutableStateFlow(listOf(0, 3))

    private val album =
        mockk<AlbumRepository> {
            every { days() } returns days
            coEvery { document(any()) } answers { bytesOf(firstArg()) }
            coEvery { regenerate(any()) } answers { "regenerated-${firstArg<Int>()}".encodeToByteArray() }
        }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        AlbumViewModel(documents, album, savedStateHandle)

    @Test
    fun `最初は最新の日を選んで表示する`() =
        runTest {
            viewModel().uiState.test {
                awaitItem() shouldBe
                    AlbumUiState(
                        days = listOf(0, 3),
                        selectedDay = 3,
                        document = PlantDocument(bytesOf(3)),
                        isUnreadable = false,
                        documentSizeDp = 320,
                        allowedShaders = emptySet(),
                    )
            }
        }

    @Test
    fun `日を選ぶと、その日を表示する`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(AlbumIntent.SelectDay(0))

            viewModel.uiState.value.selectedDay shouldBe 0
            viewModel.uiState.value.document shouldBe PlantDocument(bytesOf(0))
        }

    @Test
    fun `記録が増えると一覧に出るが、選んでいる日は変わらない`() =
        runTest {
            val viewModel = viewModel()

            days.value = listOf(0, 3, 7)

            viewModel.uiState.value.days shouldBe listOf(0, 3, 7)
            viewModel.uiState.value.selectedDay shouldBe 3
        }

    @Test
    fun `記録がまだないときは何も選ばない`() =
        runTest {
            days.value = emptyList()

            val state = viewModel().uiState.value

            state.selectedDay shouldBe null
            state.document shouldBe null
        }

    @Test
    fun `読めなかった日は作り直して表示する`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(AlbumIntent.ReportLoadFailure(3, IllegalStateException("Unknown operation")))

            viewModel.uiState.value.document shouldBe PlantDocument("regenerated-3".encodeToByteArray())
            viewModel.uiState.value.isUnreadable shouldBe false
            coVerify(exactly = 1) { album.regenerate(3) }
        }

    @Test
    fun `作り直しても読めない日は、繰り返さずに表示できないことを示す`() =
        runTest {
            val viewModel = viewModel()

            repeat(2) { viewModel.onIntent(AlbumIntent.ReportLoadFailure(3, IllegalStateException())) }

            viewModel.uiState.value.document shouldBe null
            viewModel.uiState.value.isUnreadable shouldBe true
            coVerify(exactly = 1) { album.regenerate(3) }
        }

    @Test
    fun `選んでいない日の読込失敗は無視する`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(AlbumIntent.ReportLoadFailure(0, IllegalStateException()))

            viewModel.uiState.value.document shouldBe PlantDocument(bytesOf(3))
            coVerify(exactly = 0) { album.regenerate(any()) }
        }

    @Test
    fun `作り直された画面では、保存しておいた日を選ぶ`() =
        runTest {
            val viewModel = viewModel(SavedStateHandle(mapOf("selectedDay" to 0)))

            viewModel.uiState.value.selectedDay shouldBe 0
            viewModel.uiState.value.document shouldBe PlantDocument(bytesOf(0))
        }
}

private fun bytesOf(day: Int): ByteArray = "day-$day".encodeToByteArray()
