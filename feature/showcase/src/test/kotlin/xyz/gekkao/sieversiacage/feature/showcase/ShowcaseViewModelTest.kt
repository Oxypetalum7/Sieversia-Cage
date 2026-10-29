package xyz.gekkao.sieversiacage.feature.showcase

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import xyz.gekkao.sieversiacage.core.data.AlbumRepository
import xyz.gekkao.sieversiacage.core.data.GrowthRepository
import xyz.gekkao.sieversiacage.core.data.GrowthTicker
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import xyz.gekkao.sieversiacage.core.data.WidgetRefresher
import xyz.gekkao.sieversiacage.core.player.PlantDocument
import java.time.LocalDate

private const val TODAY = 3
private val PLANTED_ON: LocalDate = LocalDate.of(2026, 10, 26)

class ShowcaseViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val documents =
        mockk<PlantDocumentRepository> {
            every { documentSizeDp } returns 320
            every { allowedShaders } returns setOf("shader")
            every { waterActionId } returns 1
            every { inAppPreviewDocument(PLANTED_ON, any()) } answers { previewOf(secondArg()) }
        }

    // 今日の分はアルバムのファイルから読む
    private val album = mockk<AlbumRepository> { coEvery { document(any()) } answers { albumOf(firstArg()) } }

    private val growth =
        mockk<GrowthRepository>(relaxed = true) {
            coEvery { todayAgeDays() } returns TODAY
            coEvery { plantedOn() } returns PLANTED_ON
        }

    private val ticker = mockk<GrowthTicker>(relaxed = true)
    private val widgets = mockk<WidgetRefresher>(relaxed = true)

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        ShowcaseViewModel(documents, album, growth, ticker, widgets, savedStateHandle)

    @Test
    fun `最初は抜けている日を進めてから、今日の株をアルバムから読んで表示する`() =
        runTest {
            viewModel().uiState.test {
                awaitItem() shouldBe
                    ShowcaseUiState(
                        ageDays = TODAY,
                        todayAgeDays = TODAY,
                        document = PlantDocument(albumOf(TODAY)),
                        documentSizeDp = 320,
                        allowedShaders = setOf("shader"),
                    )
            }
            coVerifyOrder {
                ticker.catchUp()
                album.document(TODAY)
            }
        }

    @Test
    fun `次の日は毎日水をやった前提のプレビューで、アルバムには書かない`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(ShowcaseIntent.NextDay)

            viewModel.uiState.value.ageDays shouldBe TODAY + 1
            viewModel.uiState.value.isPreview shouldBe true
            viewModel.uiState.value.document shouldBe PlantDocument(previewOf(TODAY + 1))
            coVerify(exactly = 0) { album.document(TODAY + 1) }
        }

    @Test
    fun `今日に戻ると、アルバムの今日の株を表示する`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onIntent(ShowcaseIntent.PreviousDay)

            viewModel.onIntent(ShowcaseIntent.BackToToday)

            viewModel.uiState.value.isPreview shouldBe false
            viewModel.uiState.value.document shouldBe PlantDocument(albumOf(TODAY))
        }

    @Test
    fun `0 日目から前の日には戻らない`() =
        runTest {
            coEvery { growth.todayAgeDays() } returns 0
            val viewModel = viewModel()

            viewModel.onIntent(ShowcaseIntent.PreviousDay)

            viewModel.uiState.value.ageDays shouldBe 0
        }

    @Test
    fun `水やりのホストアクションが届くと、水やりを記録する`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(ShowcaseIntent.ReceiveHostAction(actionId = 1, metadata = ""))

            coVerifyOrder {
                growth.recordWatering()
                widgets.refresh()
            }
        }

    @Test
    fun `水やり以外のホストアクションは記録しない`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(ShowcaseIntent.ReceiveHostAction(actionId = 99, metadata = null))

            coVerify(exactly = 0) { growth.recordWatering() }
        }

    @Test
    fun `作り直された画面では、保存しておいた日から始める`() =
        runTest {
            val viewModel = viewModel(SavedStateHandle(mapOf("ageDays" to 5)))

            viewModel.uiState.value.ageDays shouldBe 5
            viewModel.uiState.value.document shouldBe PlantDocument(previewOf(5))
        }
}

private fun albumOf(ageDays: Int): ByteArray = "album-$ageDays".encodeToByteArray()

private fun previewOf(ageDays: Int): ByteArray = "preview-$ageDays".encodeToByteArray()
