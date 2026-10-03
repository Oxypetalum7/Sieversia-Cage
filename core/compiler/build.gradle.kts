plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "xyz.gekkao.sieversiacage.core.compiler"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:sim"))
    api(libs.remote.core)
    implementation(libs.remote.creation.android)
}
