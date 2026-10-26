package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * アルバムの 1 日分の正のデータ（ADR-0011 / ADR-0018）。`.rc` はここから作り直せるキャッシュ。
 *
 * 列の型は Kotlin の enum に頼らず文字列で持ち、保存形式を Room のスキーマだけで決める。
 *
 * @property ageDays 植えてからの経過日数
 * @property stage Sim の出力（`GrowthStage` の名前）。Sim の出力が増えたら列を足し、マイグレーションを書く
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
    @ColumnInfo(name = "remote_compose_version")
    val remoteComposeVersion: String,
    @ColumnInfo(name = "compiler_version")
    val compilerVersion: Int,
    @ColumnInfo(name = "target")
    val target: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
