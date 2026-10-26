// ADR-0016: 成長データ・アルバムの保存と、ドキュメント生成の窓口
plugins {
    alias(libs.plugins.sieversia.android.library)
    alias(libs.plugins.sieversia.android.hilt)
    alias(libs.plugins.sieversia.android.room)
}

android {
    namespace = "xyz.gekkao.sieversiacage.core.data"

    // ADR-0018: DAO とマイグレーションのテストは実機（本物の SQLite）で動かす
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}

dependencies {
    implementation(project(":core:compiler"))
    implementation(project(":core:sim"))

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
