/*
 * Copyright (c) 2023 Toast Inc.
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

import org.gradle.testkit.runner.UnexpectedBuildResultException
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.appendText
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists

private val NAMESPACE = ExtensionContext.Namespace.create(TestProjectExtension::class.java.name, "testkit-project")
private const val COVERAGE_RECORDER = "coverage-recorder"
private const val LOCATOR = "locator"
private const val PROJECTS = "PROJECTS"

data class ProjectKey(
    val gradleVersion: String?
) {
    constructor(gradleVersion: GradleVersionArgument) : this(gradleVersion.version)
}

class TestProjects : CloseableResource {
    private val projects = ConcurrentHashMap<ProjectKey, TestProject>()

    fun project(key: ProjectKey, create: (ProjectKey) -> TestProject) = projects.computeIfAbsent(key, create)

    override fun close() {
        projects.values.forEach(TestProject::close)
    }

    fun logOutputOnce() {
        projects.values.forEach(TestProject::logOutputOnce)
    }
}

class TestProjectExtension : ParameterResolver, BeforeAllCallback, AfterTestExecutionCallback, InvocationInterceptor, ArgumentsProvider {
    override fun beforeAll(context: ExtensionContext) {
        val coverage = CoverageSettings.settings

        if (coverage != null) {
            context.getStore(NAMESPACE).put(COVERAGE_RECORDER, CoverageRecorder(coverage))
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        parameterContext.parameter.type == TestProject::class.java &&
            extensionContext.parent.map { it::class.java.name } != Optional.of("org.junit.jupiter.engine.descriptor.TestTemplateExtensionContext")

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        extensionContext.project(GradleVersionArgument.DEFAULT)

    override fun afterTestExecution(context: ExtensionContext) {
        context.executionException.ifPresent {
            if (it !is UnexpectedBuildResultException) {
                context.get<TestProjects>(NAMESPACE, PROJECTS).logOutputOnce()
            }
        }
    }

    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        val methodAnn = context.requiredTestMethod.getAnnotation(ParameterizedWithGradleVersions::class.java)

        val versions = if (methodAnn.value.isNotEmpty()) {
            methodAnn.value
        } else {
            context.requiredTestClass.getAnnotation(TestKit::class.java).gradleVersions
        }

        return versions.map {
            Arguments.of(context.project(GradleVersionArgument.of(it)))
        }.stream()
    }

    companion object {
        private inline fun <reified T> ExtensionContext.get(namespace: ExtensionContext.Namespace, key: String) =
            getStore(namespace).get(key, T::class.java)

        private inline fun <K, reified V> ExtensionContext.Store.cache(key: K, noinline f: (K) -> V) =
            getOrComputeIfAbsent(key, f, V::class.java)

        @OptIn(ExperimentalPathApi::class)
        private fun ExtensionContext.project(gradleVersion: GradleVersionArgument): TestProject {
            val parameters = requiredTestClass.getAnnotation(TestKit::class.java) ?: TestKit()

            val locator = getStore(NAMESPACE).cache(LOCATOR) {
                parameters.locator.java.getDeclaredConstructor().newInstance() as ProjectLocator
            }

            return getStore(NAMESPACE).cache(PROJECTS) {
                TestProjects()
            }.project(ProjectKey(gradleVersion)) {
                val tempProjectDir = createTempDirectory("junit-gradlekit")

                val location = locator.projectPath(System.getProperty("testkit-projects"), this)

                if (!location.exists()) {
                    error { "expected a test project in $location" }
                }

                location.copyToRecursively(target = tempProjectDir, followLinks = false, overwrite = false)

                createProject(
                    tempProjectDir,
                    gradleVersion,
                    parameters.cleanup,
                    CoverageSettings.settings?.let { get(NAMESPACE, COVERAGE_RECORDER) }
                )
            }
        }

        fun createProject(projectDir: Path, gradleVersion: GradleVersionArgument, cleanup: Boolean = true, coverageRecorder: CoverageRecorder?): TestProject {
            val integrationRepo = System.getProperty("testkit-integration-repo")
            val projectVersion = System.getProperty("testkit-project-version")
            val pluginVersion = System.getProperty("testkit-plugin-version")
            val plugins = System.getProperty("testkit-plugin-ids")?.split(',')?.joinToString(separator = "\n") {
                """id("$it") version("$projectVersion")"""
            } ?: ""

            val initArgs = if (integrationRepo != null) {
                projectDir.appendToFile(
                    "init.gradle.kts",
                    """
    
                    settingsEvaluated {
                        pluginManagement {
                            repositories {
                                maven(url = "file://$integrationRepo")
                                gradlePluginPortal()
                            }
                            
                            plugins {
                                id("com.toasttab.testkit.coverage") version("$pluginVersion")
                                $plugins
                            }
                        }
                    }
                    """.trimIndent()
                )

                listOf("--init-script", "init.gradle.kts")
            } else {
                emptyList()
            }

            if (coverageRecorder != null) {
                projectDir.appendToFile(
                    "gradle.properties",
                    """
                            
                            # custom jacoco properties
                            systemProp.jacoco-agent.output=tcpclient
                            systemProp.jacoco-agent.port=${coverageRecorder.port}
                            systemProp.jacoco-agent.sessionid=test
                            systemProp.jacoco-agent.includes=${coverageRecorder.settings.includes}
                            systemProp.jacoco-agent.excludes=${coverageRecorder.settings.excludes}
                    """.trimIndent()
                )
            }

            return TestProject(projectDir, gradleVersion, cleanup, initArgs)
        }
    }
}

private fun Path.appendToFile(fileName: String, text: String) {
    val file = resolve(fileName)

    if (!file.exists()) {
        file.createFile()
    }

    file.appendText(text)
}
