/*
 * Copyright (c) 2024 Toast Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.toasttab.gradle.testkit

import com.toasttab.gradle.testkit.shared.configureIntegrationPublishing
import com.toasttab.gradle.testkit.shared.integrationDirectory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.tasks.JacocoReportBase
import javax.inject.Inject

class TestkitPlugin
    @Inject
    constructor(
        private val fs: FileSystemOperations
    ) : Plugin<Project> {
        override fun apply(project: Project) {
            val extension = project.extensions.create<TestkitExtension>("testkitTests")
            val destfile = project.layout.buildDirectory.file("jacoco/testkit.exec")
            val tokensFile = project.layout.buildDirectory.file("testkit/tokens.properties")
            val testProjectsDir =
                extension.testProjectsDir
                    .map { project.layout.projectDirectory.dir(it) }
                    .orElse(project.layout.projectDirectory.dir("src/test/projects"))

            val writeTokens =
                project.tasks.register<WriteTokensPropertiesTask>("writeTestkitTokens") {
                    tokens.putAll(extension.replaceTokens)
                    tokens.put("TESTKIT_PLUGIN_VERSION", BuildConfig.VERSION)
                    tokens.put("TESTKIT_INTEGRATION_REPO", project.integrationDirectory().path)
                    tokens.put("VERSION", project.provider { "${project.version}" })
                    outputFile.set(tokensFile)
                }

            project.tasks.named<Test>("test") {
                dependsOn(writeTokens)

                doFirst(JacocoOutputCleanupTestTaskAction(fs, destfile))

                inputs
                    .dir(testProjectsDir)
                    .withPropertyName("testkit-projects-input")
                    .withPathSensitivity(PathSensitivity.RELATIVE)

                inputs
                    .file(tokensFile)
                    .withPropertyName("testkit-tokens-input")
                    .withPathSensitivity(PathSensitivity.NONE)

                // declare an additional jacoco output file so that the JUnit JVM and the TestKit JVM
                // do not try to write to the same file
                outputs.file(destfile).withPropertyName("testkit-coverage-output")

                systemProperty("testkit-coverage-output", "${destfile.get()}")
                systemProperty("testkit-projects", testProjectsDir.get().asFile.path)
                systemProperty("testkit-tokens", tokensFile.get().asFile.path)
                systemProperty("testkit-integration-repo", project.integrationDirectory().path)
                systemProperty("testkit-plugin-version", BuildConfig.VERSION)
            }

            project.configureIntegrationPublishing()

            project.pluginManager.withPlugin("jacoco") {
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
                            it.files("test.exec", "testkit.exec")
                        }
                    )
                }
            }
        }
    }
