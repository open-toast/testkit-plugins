plugins {
    `kotlin-conventions`
    `kotlin-dsl`
    `plugin-publishing-conventions`
    alias(libs.plugins.build.config)
}

dependencies {
    implementation(gradleApi())
    testImplementation(libs.strikt.core)
}

sourceSets {
    main {
        kotlin {
            srcDir(rootProject.layout.projectDirectory.dir("shared-build-logic/src/main/kotlin"))
        }
    }
}

buildConfig {
    packageName.set("com.toasttab.gradle.testkit")
    buildConfigField("String", "VERSION", "\"$version\"")
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
