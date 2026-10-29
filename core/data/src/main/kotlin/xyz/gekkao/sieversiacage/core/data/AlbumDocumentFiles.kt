package xyz.gekkao.sieversiacage.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * アルバムの `.rc` を日ごとのファイルとして置く（ADR-0018）。正のデータは DB にあり、ここは作り直せるキャッシュ。
 *
 * @param dir 置き場所。テストでは一時ディレクトリを渡す
 */
class AlbumDocumentFiles(
    private val dir: File,
) {
    /** 一時ファイルに書いてからリネームし、書きかけのファイルを残さない。 */
    suspend fun save(
        ageDays: Int,
        bytes: ByteArray,
    ): Unit =
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            val temp = File(dir, "day-$ageDays.rc.tmp")
            temp.writeBytes(bytes)
            if (!temp.renameTo(fileOf(ageDays))) {
                temp.delete()
                throw IOException("failed to move ${temp.name}")
            }
        }

    suspend fun load(ageDays: Int): ByteArray? =
        withContext(Dispatchers.IO) { fileOf(ageDays).takeIf { it.exists() }?.readBytes() }

    suspend fun delete(ageDays: Int): Unit = withContext(Dispatchers.IO) { fileOf(ageDays).delete() }

    suspend fun deleteAll(): Unit = withContext(Dispatchers.IO) { dir.listFiles().orEmpty().forEach { it.delete() } }

    /** `.rc` が置いてある日。DB に記録のない日（スパイクの名残など）も含む。 */
    suspend fun savedDays(): Set<Int> =
        withContext(Dispatchers.IO) {
            dir
                .listFiles { file -> file.extension == "rc" }
                .orEmpty()
                .mapNotNull { it.nameWithoutExtension.removePrefix("day-").toIntOrNull() }
                .toSet()
        }

    private fun fileOf(ageDays: Int): File = File(dir, "day-$ageDays.rc")
}
