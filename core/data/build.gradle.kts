// ADR-0016: 成長データ・アルバムの保存と、ドキュメント生成の窓口
plugins {
    alias(libs.plugins.sieversia.android.library)
    alias(libs.plugins.sieversia.android.hilt)
}

android {
    namespace = "xyz.gekkao.sieversiacage.core.data"
}

dependencies {
    implementation(project(":core:compiler"))
    implementation(project(":core:sim"))
}
