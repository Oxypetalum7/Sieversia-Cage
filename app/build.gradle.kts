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
    implementation(project(":feature:showcase"))
    implementation(project(":feature:album"))
    implementation(project(":feature:widget"))
    // 起動時のアルバムの作り直し（ADR-0011）
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
}
