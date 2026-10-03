package xyz.gekkao.sieversiacage.core.sim

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GrowthSimTest {
    @ParameterizedTest
    @CsvSource(
        "0, SPROUT",
        "2, SPROUT",
        "3, LEAF",
        "7, BUD",
        "10, BLOOM",
        "14, SEED_HEAD",
        "100, SEED_HEAD",
    )
    fun `経過日数に応じた成長段階になる`(
        ageDays: Int,
        expected: GrowthStage,
    ) {
        simulate(ageDays).stage shouldBe expected
    }

    @Test
    fun `負の経過日数は受け付けない`() {
        shouldThrow<IllegalArgumentException> { simulate(-1) }
    }
}
