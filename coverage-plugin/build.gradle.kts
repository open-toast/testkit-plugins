plugins {
    `kotlin-conventions`
    `plugin-publishing-conventions`
    jacoco
}

gradlePlugin {
    plugins {
        create("jacoco") {
            id = "com.toasttab.testkit.coverage"
            implementationClass = "com.toasttab.gradle.testkit.FlushJacocoPlugin"
            description = ProjectInfo.description
            displayName = ProjectInfo.name
            tags = listOf("jacoco", "testkit")
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(projects.jacocoReflect)

    testImplementation(projects.junit5)
    testImplementation(libs.junit)
    testImplementation(libs.strikt.core)
    testImplementation(libs.jacoco.core)
}
