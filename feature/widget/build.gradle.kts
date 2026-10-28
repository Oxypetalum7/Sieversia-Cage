// ウィジェットは画面遷移を持たないので、feature 用ではなく library + Hilt で足りる
plugins {
    alias(libs.plugins.sieversia.android.library)
    alias(libs.plugins.sieversia.android.hilt)
}

android {
    namespace = "xyz.gekkao.sieversiacage.feature.widget"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.timber)
}
