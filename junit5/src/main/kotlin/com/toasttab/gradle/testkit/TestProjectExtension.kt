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
import java.io.File
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

                val coverage = CoverageSettings.settings

                if (coverage != null) {
                    val collector = get<CoverageRecorder>(NAMESPACE, COVERAGE_RECORDER)
                    tempProjectDir.resolve("gradle.properties").apply {
                        if (!exists()) {
                            createFile()
                        }

                        appendText("\nsystemProp.jacoco-agent.output=tcpclient\nsystemProp.jacoco-agent.port=${collector.port}\nsystemProp.jacoco-agent.sessionid=test\nsystemProp.jacoco-agent.includes=${coverage.includes}\nsystemProp.jacoco-agent.excludes=${coverage.excludes}\n")
                    }
                }

                TestProject(tempProjectDir, pluginClasspath(), gradleVersion, parameters.cleanup)
            }
        }

        fun pluginClasspath(): List<File> {
            val instrumentedProperty = System.getProperty("testkit-plugin-instrumented-jars")

            return if (instrumentedProperty != null) {
                val instrumented = File(instrumentedProperty).listFiles().toList()
                val external = System.getProperty("testkit-plugin-external-jars").split(File.pathSeparatorChar).map {
                    File(it)
                }
                val jacoco = File(System.getProperty("testkit-plugin-jacoco-jar"))

                instrumented + external + jacoco
            } else {
                emptyList()
            }
        }
    }
}
