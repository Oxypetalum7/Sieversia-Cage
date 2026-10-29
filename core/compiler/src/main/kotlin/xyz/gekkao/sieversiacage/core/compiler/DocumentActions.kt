package xyz.gekkao.sieversiacage.core.compiler

/**
 * 水やりのタップで、ドキュメントからホスト（ランチャーやアプリ）に送るアクションの ID（ADR-0014）。
 * ウィジェットはこの ID に PendingIntent を登録し、アプリ内のプレイヤーは onAction で受け取る。
 */
const val WATER_ACTION_ID = 1
