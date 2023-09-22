plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(libs.kotlin.gradle)
    implementation(libs.nexus.publish)
    implementation(libs.gradle.publish)
    implementation(libs.spotless)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}