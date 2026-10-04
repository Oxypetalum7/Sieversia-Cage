package xyz.gekkao.sieversiacage.core.compiler

/**
 * ドキュメントを再生する先。プロファイル（RestrictTo）を外に出さないための、自前の選択肢（ADR-0010）。
 *
 * - [IN_APP]: アプリ内の androidx プレイヤー
 * - [WIDGET_V6] / [WIDGET_V7]: ホーム画面のウィジェット（OS 内蔵プレイヤー）。違いは docs/spikes/README.md を参照
 */
enum class DocumentTarget {
    IN_APP,
    WIDGET_V6,
    WIDGET_V7,
}
