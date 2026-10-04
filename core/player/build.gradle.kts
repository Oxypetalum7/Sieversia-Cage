// ADR-0010 / ADR-0016: 再生側の RestrictTo はこのモジュールに閉じ込める
plugins {
    alias(libs.plugins.sieversia.android.library)
    alias(libs.plugins.sieversia.android.compose)
}

android {
    namespace = "xyz.gekkao.sieversiacage.core.player"
}

dependencies {
    implementation(libs.remote.core)
    implementation(libs.remote.player.compose)
    implementation(libs.remote.player.core)
    implementation(libs.remote.player.view)
}
