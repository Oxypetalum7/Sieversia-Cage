package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * アルバムの 1 日分の正のデータ（ADR-0011 / ADR-0018）。`.rc` はここから作り直せるキャッシュ。
 *
 * 列の型は Kotlin の enum に頼らず文字列で持ち、保存形式を Room のスキーマだけで決める。
 * stage から vigor までが Sim の出力（ADR-0020）。項目が増えたら列を足し、マイグレーションを書く。
 * 版 2 で足した列の既定値は、版 1 の行（Sim の入力の記録がない）を埋めるためのもの。
 *
 * @property ageDays 植えてからの経過日数
 * @property stage 成長段階（`GrowthStage` の名前）
 * @property cycle 周回数
 * @property stageProgress 段階の中での進み具合（0〜1）
 * @property growth 今の周の中で積み上げた成長ポイント
 * @property branchCount 枝の数
 * @property flowerCount 花の数
 * @property soilMoisture 土の湿り気（0〜1）
 * @property vigor 元気さ（0〜1）
 * @property remoteComposeVersion 生成に使った Remote Compose の版
 * @property compilerVersion 生成に使った Compiler の版
 * @property target 再生先（`DocumentTarget` の名前）
 * @property createdAt 生成した日時（エポックミリ秒）
 */
@Entity(tableName = "album_entry")
data class AlbumEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "age_days")
    val ageDays: Int,
    @ColumnInfo(name = "stage")
    val stage: String,
    @ColumnInfo(name = "cycle", defaultValue = "0")
    val cycle: Int,
    @ColumnInfo(name = "stage_progress", defaultValue = "0")
    val stageProgress: Float,
    @ColumnInfo(name = "growth", defaultValue = "0")
    val growth: Float,
    @ColumnInfo(name = "branch_count", defaultValue = "1")
    val branchCount: Int,
    @ColumnInfo(name = "flower_count", defaultValue = "1")
    val flowerCount: Int,
    @ColumnInfo(name = "soil_moisture", defaultValue = "1")
    val soilMoisture: Float,
    @ColumnInfo(name = "vigor", defaultValue = "1")
    val vigor: Float,
    @ColumnInfo(name = "remote_compose_version")
    val remoteComposeVersion: String,
    @ColumnInfo(name = "compiler_version")
    val compilerVersion: Int,
    @ColumnInfo(name = "target")
    val target: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
