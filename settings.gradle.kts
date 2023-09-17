enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("gradle.plugin.net.vivin:gradle-semantic-build-versioning:4.0.0")
    }
}

rootProject.name = "testkit-plugins"

apply(plugin = "net.vivin.gradle-semantic-build-versioning")

include(
    ":junit5", ":common", ":testkit-plugin", ":runtime-plugin"
)