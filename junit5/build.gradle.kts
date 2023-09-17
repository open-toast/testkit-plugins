plugins {
    `kotlin-conventions`
    `library-publishing-conventions`
}

dependencies {
    implementation(projects.common)
    implementation(libs.junit)
    implementation(gradleTestKit())
    implementation(libs.jacoco.core)
}