plugins {
    alias(libs.plugins.sieversia.android.library)
}

android {
    namespace = "xyz.gekkao.sieversiacage.core.compiler"
}

dependencies {
    api(project(":core:sim"))
    api(libs.remote.core)
    implementation(libs.remote.creation.android)
}
