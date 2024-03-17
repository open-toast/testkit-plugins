plugins {
    `kotlin-conventions`
    `library-publishing-conventions`
    jacoco
}

tasks {
    test {
        configure<JacocoTaskExtension> {
            includes = listOf("com.toasttab.testkit.*")
            excludes = listOf("com.unrelated")
        }
    }
}

dependencies {
    implementation(projects.common)

    testImplementation(projects.junit5)
    testImplementation(libs.junit)
    testImplementation(libs.strikt.core)
}
