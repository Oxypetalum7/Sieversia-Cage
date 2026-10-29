plugins {
    alias(libs.plugins.sieversia.android.feature)
}

android {
    namespace = "xyz.gekkao.sieversiacage.feature.showcase"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:player"))
    // 通知の許可を求める（rememberLauncherForActivityResult、ADR-0021）
    implementation(libs.androidx.activity.compose)
}
