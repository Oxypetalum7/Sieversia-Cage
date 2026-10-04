import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** 純 Kotlin（JVM）のモジュール。ADR-0015: Android にも Remote Compose にも依存しない壁を、このプラグインで保つ。 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureLint()

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions.jvmTarget.set(JvmTarget.fromTarget(JAVA_VERSION.toString()))
            }

            dependencies {
                add("testImplementation", platform(libs.findLibrary("junit-bom").get()))
                add("testImplementation", libs.findLibrary("junit-jupiter").get())
                add("testImplementation", libs.findLibrary("kotest-assertions-core").get())
                add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
            }
            tasks.withType<Test>().configureEach { useJUnitPlatform() }
        }
    }
}
