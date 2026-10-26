import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Android ライブラリ（:core:* など）。Kotlin は AGP 9 の組み込みを使う。src/test は JUnit 5 で動かす（ADR-0018）。 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            configureLint()
            configureUnitTest()
            extensions.configure<LibraryExtension> { configureAndroidCommon(this) }
        }
    }
}
