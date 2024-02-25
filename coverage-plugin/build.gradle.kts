plugins {
    `kotlin-conventions`
    `plugin-publishing-conventions`
    jacoco
}

dependencies {
    implementation(gradleApi())
    implementation(projects.common)

    testImplementation(projects.junit5)
    testImplementation(libs.junit)
    testImplementation(libs.strikt.core)
}

tasks {
    test {
        systemProperty("javaagent", project.zipTree(project.configurations.getByName("jacocoAgent").asPath).filter {
            it.name == "jacocoagent.jar"
        }.singleFile.path)
    }
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


