import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * feature モジュール（ADR-0016）。Android ライブラリ＋Compose＋Hilt＋Navigation Compose（型安全なルート）。
 * feature 同士は依存しない。画面遷移は :app が持つ。
 * ViewModel は MVI で書き、「Intent を入れて UiState の遷移を確かめる」テストを MockK と Turbine で書く（ADR-0005）。
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("sieversia.android.library")
            pluginManager.apply("sieversia.android.compose")
            pluginManager.apply("sieversia.android.hilt")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
                add("implementation", libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("kotlinx-serialization-core").get())
                add("implementation", libs.findLibrary("timber").get())

                add("testImplementation", libs.findLibrary("mockk").get())
                add("testImplementation", libs.findLibrary("turbine").get())
            }
        }
    }
}
