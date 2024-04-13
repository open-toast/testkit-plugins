import com.toasttab.gradle.testkit.shared.configureInstrumentation

plugins {
    `kotlin-conventions`
    jacoco
}

configureInstrumentation(projects.coveragePlugin)

dependencies {
    implementation(gradleApi())
    testImplementation(libs.junit)
    testImplementation(libs.strikt.core)
    testImplementation(projects.jacocoReflect)
    testImplementation(projects.junit5)
    testImplementation(gradleTestKit())
    testImplementation(libs.jacoco.core)
}
