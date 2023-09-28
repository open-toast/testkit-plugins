package com.toasttab.gradle.testkit

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.filter
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.tasks.JacocoReportBase
import javax.inject.Inject

class TestkitPlugin @Inject constructor(
    private val fs: FileSystemOperations
) : Plugin<Project> {
    private val Project.jacocoJar get() = project.zipTree(project.configurations.getByName("jacocoAgent").asPath).filter {
        it.name == "jacocoagent.jar"
    }.singleFile

    override fun apply(project: Project) {
        val extension = project.extensions.create<TestkitExtension>("testkitTests")
        val destfile = project.layout.buildDirectory.file("jacoco/testkit.exec")
        val testProjectDir = project.layout.buildDirectory.dir("test-projects")

        project.tasks.register<Copy>("copyTestProjects") {
            from(extension.testProjectsDir)
            into(testProjectDir)

            if (extension.replaceTokens.isNotEmpty()) {
                filter<ReplaceTokens>(mapOf("tokens" to extension.replaceTokens))
            }
        }

        project.tasks.named<Test>("test") {
            dependsOn("copyTestProjects")

            doFirst(JacocoOutputCleanupTestTaskAction(fs, destfile))

            // declare an additional jacoco output file so that the JUnit JVM and the TestKit JVM
            // do not try to write to the same file
            outputs.file(destfile).withPropertyName(TESTKIT_COVERAGE_OUTPUT)

            // pipe the jacoco javaagent location into the new JVM that testkit launches
            // see TestProjectExtension and Gradle's JacocoPlugin class
            systemProperty(TESTKIT_JAVAAGENT, "${project.jacocoJar}")
            systemProperty(TESTKIT_COVERAGE_OUTPUT, "${destfile.get()}")
            systemProperty(TESTKIT_PROJECTS, "${testProjectDir.get()}")
        }

        project.pluginManager.withPlugin("jvm-test-suite") {
            // add the TestKit jacoco file to outgoing artifacts so that it can be aggregated
            project.configurations.getAt("coverageDataElementsForTest").outgoing.artifact(destfile) {
                type = ArtifactTypeDefinition.BINARY_DATA_TYPE
                builtBy("test")
            }
        }

        project.tasks.named<JacocoReportBase>("jacocoTestReport") {
            // add the TestKit jacoco file to the local jacoco report
            executionData.from(
                project.layout.buildDirectory.dir("jacoco").map {
                    project.files("test.exec", "testkit.exec")
                }
            )
        }
    }
}
