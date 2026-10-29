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
import xyz.gekkao.sieversiacage.core.data.GrowthRepository
import xyz.gekkao.sieversiacage.core.data.GrowthTicker
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import xyz.gekkao.sieversiacage.core.data.ReminderNotifier
import xyz.gekkao.sieversiacage.core.data.TodayPlant
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

    private val growth =
        mockk<GrowthRepository>(relaxed = true) {
            coEvery { todayAgeDays() } returns TODAY
            coEvery { plantedOn() } returns PLANTED_ON
        }

    // 今日の分はアルバムのファイルから読む
    private val ticker = mockk<GrowthTicker> { coEvery { today() } answers { TodayPlant(TODAY, albumOf(TODAY)) } }
    private val widgets = mockk<WidgetRefresher>(relaxed = true)
    private val notifier = mockk<ReminderNotifier>(relaxed = true) { coEvery { shouldAskPermission() } returns false }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        ShowcaseViewModel(documents, growth, ticker, widgets, notifier, savedStateHandle)

    @Test
    fun `最初は今日の株をアルバムから読んで表示する`() =
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
            coVerify { ticker.today() }
        }

    @Test
    fun `次の日は毎日水をやった前提のプレビューで、アルバムには書かない`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(ShowcaseIntent.NextDay)

            viewModel.uiState.value.ageDays shouldBe TODAY + 1
            viewModel.uiState.value.isPreview shouldBe true
            viewModel.uiState.value.document shouldBe PlantDocument(previewOf(TODAY + 1))
            coVerify(exactly = 1) { documents.inAppPreviewDocument(PLANTED_ON, TODAY + 1) }
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
            coEvery { ticker.today() } returns TodayPlant(0, albumOf(0))
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
    fun `通知の許可を尋ねる条件がそろっていれば、案内を出す`() =
        runTest {
            coEvery { notifier.shouldAskPermission() } returns true

            viewModel().uiState.value.showReminderPrompt shouldBe true
        }

    @Test
    fun `初めて水をやると、案内を出し直す`() =
        runTest {
            val viewModel = viewModel()
            coEvery { notifier.shouldAskPermission() } returns true

            viewModel.onIntent(ShowcaseIntent.ReceiveHostAction(actionId = 1, metadata = ""))

            viewModel.uiState.value.showReminderPrompt shouldBe true
        }

    @Test
    fun `知らせるを選ぶと、許可を求める Effect を出す`() =
        runTest {
            val viewModel = viewModel()

            viewModel.effects.test {
                viewModel.onIntent(ShowcaseIntent.AcceptReminder)

                awaitItem() shouldBe ShowcaseEffect.RequestNotificationPermission
            }
        }

    @Test
    fun `今はいいを選ぶか許可の結果が出たら、尋ねたことを記録して案内を閉じる`() =
        runTest {
            coEvery { notifier.shouldAskPermission() } returns true
            val viewModel = viewModel()

            viewModel.onIntent(ShowcaseIntent.DeclineReminder)
            viewModel.uiState.value.showReminderPrompt shouldBe false
            viewModel.onIntent(ShowcaseIntent.ReportReminderPermission(granted = false))

            coVerify(exactly = 2) { notifier.markPermissionAsked() }
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
