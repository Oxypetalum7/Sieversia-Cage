package xyz.gekkao.sieversiacage.core.data

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import xyz.gekkao.sieversiacage.core.sim.DayInput
import xyz.gekkao.sieversiacage.core.sim.initialState
import xyz.gekkao.sieversiacage.core.sim.step
import java.time.LocalDate

private val DAY: LocalDate = LocalDate.of(2026, 10, 29)

class ReminderPolicyTest {
    // 植えた日（湿っている）から、水をやらない日を [dryDays] 日続けた状態
    private fun afterDryDays(dryDays: Int) =
        (0 until dryDays).fold(initialState()) { s, i -> step(s, DayInput(DAY.plusDays(i.toLong()), 0)) }

    @Test
    fun `水をやらない日が 1 日だけなら、まだ知らせない`() {
        shouldRemind(afterDryDays(1), wateredToday = false) shouldBe false
    }

    @Test
    fun `水をやらない日が 2 日続くと、知らせる`() {
        shouldRemind(afterDryDays(2), wateredToday = false) shouldBe true
    }

    @Test
    fun `今日水をやっていれば、土が乾いていても知らせない`() {
        shouldRemind(afterDryDays(3), wateredToday = true) shouldBe false
    }
}
