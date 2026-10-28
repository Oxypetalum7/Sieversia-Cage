package xyz.gekkao.sieversiacage.feature.showcase

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import xyz.gekkao.sieversiacage.core.data.AlbumRepository
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import xyz.gekkao.sieversiacage.core.player.PlantDocument

class ShowcaseViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val documents =
        mockk<PlantDocumentRepository> {
            every { documentSizeDp } returns 320
            every { allowedShaders } returns setOf("shader")
        }

    // 記録したバイト列を、そのまま読み直せるアルバム
    private val album =
        mockk<AlbumRepository> {
            coEvery { record(any<Int>()) } answers { bytesOf(firstArg()) }
            coEvery { document(any()) } answers { bytesOf(firstArg()) }
        }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        ShowcaseViewModel(documents, album, savedStateHandle)

    @Test
    fun `最初は 0 日目を記録して読み直したドキュメントを表示する`() =
        runTest {
            viewModel().uiState.test {
                awaitItem() shouldBe
                    ShowcaseUiState(
                        ageDays = 0,
                        document = PlantDocument(bytesOf(0)),
                        documentSizeDp = 320,
                        allowedShaders = setOf("shader"),
                    )
            }
            coVerify { album.record(0) }
        }

    @Test
    fun `次の日に進むと、その日を記録して表示する`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(ShowcaseIntent.NextDay)

            viewModel.uiState.value.ageDays shouldBe 1
            viewModel.uiState.value.document shouldBe PlantDocument(bytesOf(1))
            coVerify { album.record(1) }
        }

    @Test
    fun `0 日目から前の日には戻らない`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(ShowcaseIntent.PreviousDay)

            viewModel.uiState.value.ageDays shouldBe 0
            coVerify(exactly = 0) { album.record(-1) }
        }

    @Test
    fun `前の日に戻ると、その日を表示する`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onIntent(ShowcaseIntent.NextDay)
            viewModel.onIntent(ShowcaseIntent.NextDay)

            viewModel.onIntent(ShowcaseIntent.PreviousDay)

            viewModel.uiState.value.ageDays shouldBe 1
            viewModel.uiState.value.document shouldBe PlantDocument(bytesOf(1))
        }

    @Test
    fun `作り直された画面では、保存しておいた日から始める`() =
        runTest {
            val viewModel = viewModel(SavedStateHandle(mapOf("ageDays" to 5)))

            viewModel.uiState.value.ageDays shouldBe 5
            viewModel.uiState.value.document shouldBe PlantDocument(bytesOf(5))
        }
}

private fun bytesOf(ageDays: Int): ByteArray = "day-$ageDays".encodeToByteArray()
