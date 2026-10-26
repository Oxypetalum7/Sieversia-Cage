import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room 3（ADR-0018）。注釈処理は KSP、ドライバは BundledSQLiteDriver。sieversia.android.library の後に当てる。
 * スキーマは `schemas/` に書き出してリポジトリに残し、マイグレーションのテスト（androidTest）が読む。
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room3")

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                add("implementation", libs.findLibrary("room3-runtime").get())
                add("implementation", libs.findLibrary("sqlite-bundled").get())
                add("ksp", libs.findLibrary("room3-compiler").get())
                add("androidTestImplementation", libs.findLibrary("room3-testing").get())
            }
        }
    }
}
