import com.toasttab.gradle.testkit.shared.RepositoryDescriptor
import com.toasttab.gradle.testkit.shared.configureIntegrationPublishing
import com.toasttab.gradle.testkit.shared.publishOnlyIf

plugins {
    `kotlin-conventions`
    jacoco
    `plugin-publishing-conventions`
}

gradlePlugin {
    plugins {
        create("integration") {
            id = "com.toasttab.testkit.integration.test"
            implementationClass = "com.toasttab.gradle.testkit.TestPlugin"
            description = ProjectInfo.description
            displayName = ProjectInfo.name
            tags = listOf("jacoco", "testkit")
        }
    }
}

tasks {
    test {
        systemProperty("version", "$version")
        systemProperty("testkit-integration-repo", rootProject.layout.buildDirectory.dir("integration-repo").get().asFile.path)
    }
}

configureIntegrationPublishing("testRuntimeClasspath")
publishOnlyIf { _, repo -> repo == RepositoryDescriptor.INTEGRATION }

dependencies {
    implementation(gradleApi())
    testImplementation(libs.junit)
    testImplementation(libs.strikt.core)
    testImplementation(projects.jacocoReflect)
    testImplementation(projects.junit5)
    testImplementation(gradleTestKit())
    testImplementation(libs.jacoco.core)
    testImplementation(projects.coveragePlugin)
}
