package xyz.gekkao.sieversiacage.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ドキュメントのバイト列を日ごとのファイルとして保存・読込する（スパイク④のまま）。
 * M1 タスク2 で、生成元の Sim 状態とメタデータも一緒に保存するように作り直す（ADR-0011）。
 */
@Singleton
class AlbumStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val dir = File(context.filesDir, "album").apply { mkdirs() }

        fun save(
            ageDays: Int,
            bytes: ByteArray,
        ): File = fileOf(ageDays).apply { writeBytes(bytes) }

        fun load(ageDays: Int): ByteArray? = fileOf(ageDays).takeIf { it.exists() }?.readBytes()

        fun savedDays(): List<Int> =
            dir
                .listFiles { file -> file.extension == "rc" }
                .orEmpty()
                .mapNotNull { it.nameWithoutExtension.removePrefix("day-").toIntOrNull() }
                .sorted()

        private fun fileOf(ageDays: Int): File = File(dir, "day-$ageDays.rc")
    }
