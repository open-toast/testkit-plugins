import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.projectDirectory.dir("../shared-build-logic/src/main/kotlin"))
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlin.gradle)
    implementation(libs.nexus.publish)
    implementation(libs.gradle.publish)
    implementation(libs.spotless)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}