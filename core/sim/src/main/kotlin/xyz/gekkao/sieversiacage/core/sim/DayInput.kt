package xyz.gekkao.sieversiacage.core.sim

import java.time.LocalDate

/**
 * Sim の 1 日分の入力（ADR-0020）。
 *
 * @property date その日の日付（端末の現地時刻）。季節係数はこの月から決める
 * @property waterTaps その日の水やりのタップ数。1 回以上なら「水をやった日」と数え、連打しても 1 回と同じ
 */
data class DayInput(
    val date: LocalDate,
    val waterTaps: Int,
) {
    init {
        require(waterTaps >= 0) { "waterTaps must be non-negative: $waterTaps" }
    }

    val watered: Boolean get() = waterTaps > 0
}
