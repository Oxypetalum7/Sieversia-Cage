plugins {
    alias(libs.plugins.sieversia.android.feature)
}

android {
    namespace = "xyz.gekkao.sieversiacage.feature.showcase"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:player"))
}
