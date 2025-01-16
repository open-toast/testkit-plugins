plugins {
    `kotlin-conventions`
    `library-publishing-conventions`
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.projectDirectory.dir("../shared-common-logic/src/main/kotlin"))
        }
    }
}

tasks {
    test {
        useJUnitPlatform()

        systemProperty("testkit-projects", layout.projectDirectory.file("src/test/projects").asFile.path)

        inputs.dir(layout.projectDirectory.file("src/test/projects")).withPropertyName("test-projects").withPathSensitivity(PathSensitivity.RELATIVE)
    }
}

dependencies {
    implementation(projects.jacocoReflect)
    implementation(libs.junit)
    implementation(gradleTestKit())
    implementation(libs.jacoco.core)

    testImplementation(libs.strikt.core)
}
