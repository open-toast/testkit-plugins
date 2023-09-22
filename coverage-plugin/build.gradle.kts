plugins {
    `kotlin-conventions`
    `plugin-publishing-conventions`
}

dependencies {
    implementation(gradleApi())
    implementation(projects.common)
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


