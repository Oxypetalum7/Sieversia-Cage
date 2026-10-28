package xyz.gekkao.sieversiacage.core.player

import androidx.compose.runtime.Immutable

/**
 * 再生する RemoteCompose ドキュメントのバイト列。UiState に載せるための包み。
 *
 * `ByteArray` のままだと Compose が安定な型と見なさず、等しさも参照で比べてしまう。中身で比べ、作ったあとは書き換えない。
 */
@Immutable
class PlantDocument(
    val bytes: ByteArray,
) {
    val size: Int get() = bytes.size

    override fun equals(other: Any?): Boolean = other is PlantDocument && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = "PlantDocument(${bytes.size} bytes)"
}
