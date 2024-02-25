plugins {
    `kotlin-conventions`
    `kotlin-dsl`
    `plugin-publishing-conventions`
}

dependencies {
    implementation(projects.common)
    implementation(gradleApi())
    testImplementation(libs.strikt.core)
}

tasks {
    test {
        useJUnitPlatform()
        systemProperty("test-projects", layout.projectDirectory.dir("src/test/test-projects").asFile.path)
    }
}

gradlePlugin {
    plugins {
        create("jacoco") {
            id = "com.toasttab.testkit"
            implementationClass = "com.toasttab.gradle.testkit.TestkitPlugin"
            description = ProjectInfo.description
            displayName = ProjectInfo.name
            tags = listOf("jacoco", "testkit")
        }
    }
}
