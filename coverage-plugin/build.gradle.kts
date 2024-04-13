plugins {
    `kotlin-conventions`
    `library-publishing-conventions`
    jacoco
}

dependencies {
    implementation(gradleApi())
    implementation(projects.jacocoReflect)

    testImplementation(projects.junit5)
    testImplementation(libs.junit)
    testImplementation(libs.strikt.core)
    testImplementation(libs.jacoco.core)
}
