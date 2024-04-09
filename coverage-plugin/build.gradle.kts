import com.toasttab.gradle.testkit.shared.configureInstrumentation
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier
import org.gradle.jvm.tasks.Jar

plugins {
    `kotlin-conventions`
    `plugin-publishing-conventions`
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

configureInstrumentation()

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


