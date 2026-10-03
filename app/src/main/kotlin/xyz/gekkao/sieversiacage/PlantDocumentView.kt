@file:SuppressLint("RestrictedApi") // ADR-0010: 再生側の RestrictTo はこのファイルに閉じ込める

package xyz.gekkao.sieversiacage

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.size
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** RemoteCompose ドキュメントのバイト列を再生する。
 *
 * レイアウトを持つドキュメントでは `CoreDocument.width` / `height` がレイアウト前に 0 を返すため、
 * サイズは作成側が決めた値を受け取る。
 */
@Composable
fun PlantDocumentView(
    bytes: ByteArray,
    widthDp: Int,
    heightDp: Int,
    modifier: Modifier = Modifier,
) {
    val document = remember(bytes) { RemoteDocument(bytes).document }
    RemoteDocumentPlayer(
        document = document,
        documentWidth = widthDp,
        documentHeight = heightDp,
        // alpha20 では内部の Modifier.size が効かず 0x0 になるため、外から大きさを与える
        modifier = modifier.size(widthDp.dp, heightDp.dp),
    )
}
