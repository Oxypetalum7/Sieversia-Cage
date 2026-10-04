@file:SuppressLint("RestrictedApi") // ADR-0010: 再生側の RestrictTo はこのファイルに閉じ込める

package xyz.gekkao.sieversiacage

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.gekkao.sieversiacage.core.compiler.PLANT_DOCUMENT_SHADERS

/** アプリ内再生での平均フレームレートの上限。瞬間の上限（既定 60fps）と同じにして、間引きを実質なくす。 */
private const val IN_APP_MAX_AVG_FPS = 60

/** ドキュメントの中身を1行1命令のテキストにする（デバッグ・スパイク④用）。 */
fun dumpDocument(bytes: ByteArray): String = RemoteDocument(bytes).document.toNestedString()

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
    maxAvgFps: Int = IN_APP_MAX_AVG_FPS,
) {
    val document = remember(bytes) { RemoteDocument(bytes).document }
    RemoteDocumentPlayer(
        document = document,
        documentWidth = widthDp,
        documentHeight = heightDp,
        // alpha20 では内部の Modifier.size が効かず 0x0 になるため、外から大きさを与える
        modifier = modifier.size(widthDp.dp, heightDp.dp),
        // プレイヤーは既定ですべてのシェーダーを拒否する。checkShaders は setDocument の中で走るため、その前の init で許可する。
        // プレイヤーは API を確かめずに RuntimeShader（API 33〜）を作るので、33 未満では許可しない（paint の単色で描かれる）
        init = { player ->
            player.setShaderControl { source ->
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && source in PLANT_DOCUMENT_SHADERS
            }
        },
        // 既定では直近 10 秒の平均が 10fps に制限され、操作がないと数秒でカクつく。
        // setDocument のたびに既定値へ戻るため、init ではなく（setDocument の後に呼ばれる）update で設定する
        update = { player -> player.setMaxAvgFps(maxAvgFps) },
    )
}
