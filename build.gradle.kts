plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
}

val portableBuildRoot = providers.environmentVariable("DIET_TRACKER_BUILD_ROOT").orNull
subprojects {
    portableBuildRoot?.let { root -> layout.buildDirectory.set(file("$root/${project.name}")) }
}
