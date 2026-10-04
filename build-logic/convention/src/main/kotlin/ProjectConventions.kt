import com.android.build.api.dsl.CommonExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// ADR-0003 / ADR-0017: SDK と Java の版はここだけで決める
internal const val COMPILE_SDK = 37
internal const val COMPILE_SDK_MINOR = 1
internal const val MIN_SDK = 29
internal const val TARGET_SDK = 36
internal val JAVA_VERSION = JavaVersion.VERSION_17

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** compileSdk・minSdk・Java の版。application と library の共通部分。 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.apply {
        compileSdk {
            version = release(COMPILE_SDK) { minorApiLevel = COMPILE_SDK_MINOR }
        }
        defaultConfig.minSdk = MIN_SDK
        compileOptions.sourceCompatibility = JAVA_VERSION
        compileOptions.targetCompatibility = JAVA_VERSION
    }
}

/** ktlint と detekt。どのモジュールにも当てる。 */
internal fun Project.configureLint() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    extensions.configure<KtlintExtension> {
        version.set(libs.findVersion("ktlint").get().requiredVersion)
    }
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        // ADR-0005: スパイク中は警告止まり。feature 切り出し時に false へ
        ignoreFailures = true
    }
    dependencies {
        add("detektPlugins", libs.findLibrary("compose-rules-detekt").get())
    }
}
