package xyz.gekkao.sieversiacage

import java.io.File

/**
 * スパイク④: ドキュメントのバイト列を日ごとのファイルとして保存・読込する。
 * 本実装では :core:data に移し、生成元の Sim 状態も一緒に保存する（前方互換の保証がないため）。
 */
class SpikeAlbumStore(
    filesDir: File,
) {
    private val dir = File(filesDir, "album").apply { mkdirs() }

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
