package xyz.gekkao.sieversiacage.core.data.work

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")
private val TICK: LocalTime = LocalTime.of(0, 5)

class GrowthWorkSchedulerTest {
    private fun at(
        hour: Int,
        minute: Int,
    ) = ZonedDateTime.of(2026, 10, 29, hour, minute, 0, 0, TOKYO)

    @Test
    fun `夜は、日付が変わった 0 時 5 分まで待つ`() {
        delayUntilNext(at(23, 0), TICK) shouldBe Duration.ofMinutes(65)
    }

    @Test
    fun `0 時 5 分より前なら、その日の 0 時 5 分まで待つ`() {
        delayUntilNext(at(0, 1), TICK) shouldBe Duration.ofMinutes(4)
    }

    @Test
    fun `ちょうど 0 時 5 分なら、翌日の 0 時 5 分まで待つ`() {
        delayUntilNext(at(0, 5), TICK) shouldBe Duration.ofDays(1)
    }
}
