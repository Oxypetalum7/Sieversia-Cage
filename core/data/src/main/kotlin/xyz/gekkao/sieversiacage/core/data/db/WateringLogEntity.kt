package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * 水やりの記録（ADR-0014 / ADR-0020）。Sim の入力の正のデータ。
 *
 * @property date 日付（`yyyy-MM-dd`、端末の現地時刻）
 * @property taps その日のタップ数。Sim は 1 回以上を「水をやった日」と数えるが、生の回数を残す
 */
@Entity(tableName = "watering_log")
data class WateringLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "taps")
    val taps: Int,
)
