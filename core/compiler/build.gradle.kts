plugins {
    alias(libs.plugins.sieversia.android.library)
}

android {
    namespace = "xyz.gekkao.sieversiacage.core.compiler"

    // ADR-0018: アルバムの生成メタデータに残す Remote Compose の版。Version Catalog から作り、書き換え忘れを防ぐ
    buildFeatures.buildConfig = true
    defaultConfig.buildConfigField(
        "String",
        "REMOTE_COMPOSE_VERSION",
        "\"${libs.versions.remoteCompose.get()}\"",
    )
}

dependencies {
    api(project(":core:sim"))
    api(libs.remote.core)
    implementation(libs.remote.creation.android)
}
