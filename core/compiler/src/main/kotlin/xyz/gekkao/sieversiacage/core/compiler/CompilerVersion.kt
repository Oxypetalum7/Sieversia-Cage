package xyz.gekkao.sieversiacage.core.compiler

/**
 * Compiler の版（ADR-0011）。描き方や生成結果を変えたら手で上げる。
 * アルバムは、この版より古い版で作った日を起動時に作り直す。
 *
 * - 1: スパイクの描き方
 * - 2: 水やりのタップでホストアクションも送る（ADR-0014、M2-1）
 */
const val PLANT_COMPILER_VERSION = 2

/** ドキュメントの生成に使う Remote Compose の版。Version Catalog から作る（ADR-0018）。 */
const val REMOTE_COMPOSE_VERSION: String = BuildConfig.REMOTE_COMPOSE_VERSION
