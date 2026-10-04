plugins {
    alias(libs.plugins.sieversia.android.application)
    alias(libs.plugins.sieversia.android.compose)
    alias(libs.plugins.sieversia.android.hilt)
}

android {
    namespace = "xyz.gekkao.sieversiacage"

    defaultConfig {
        applicationId = "xyz.gekkao.sieversiacage"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core:compiler"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.remote.player.compose)
    implementation(libs.remote.player.core)
    implementation(libs.remote.player.view)
}
