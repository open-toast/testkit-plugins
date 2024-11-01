import com.gradle.publish.PublishPlugin
import com.gradle.publish.PublishTask
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
        for (i in 1..5) {
            create("test$i") {
                id = "com.toasttab.testkit.integration.test$i"
                implementationClass = "com.toasttab.gradle.testkit.TestPlugin$i"
                description = "test"
                displayName = "test"
                tags = listOf("test")
            }
        }
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks {
    test {
        systemProperty("version", "$version")
        systemProperty("testkit-integration-repo", rootProject.layout.buildDirectory.dir("integration-repo").get().asFile.path)
    }
}

configureIntegrationPublishing("testRuntimeClasspath")
publishOnlyIf { _, repo -> repo == RepositoryDescriptor.INTEGRATION }

tasks.withType<PublishTask> {
    enabled = false
}

dependencies {
    implementation(gradleApi())
    testImplementation(libs.junit)
    testImplementation(libs.strikt.core)
    testImplementation(projects.junit5)
    testImplementation(gradleTestKit())
    testImplementation(libs.jacoco.core)
    testImplementation(projects.coveragePlugin)
}
