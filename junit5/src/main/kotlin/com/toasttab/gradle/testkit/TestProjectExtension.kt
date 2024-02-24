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
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.appendText
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists

private val NAMESPACE = ExtensionContext.Namespace.create("testkit-project")
private const val COVERAGE_RECORDER = "coverage-recorder"
private const val PROJECT = "project"
private const val LOCATOR = "locator"

class TestProjectExtension : ParameterResolver, BeforeAllCallback, AfterTestExecutionCallback {
    override fun beforeAll(context: ExtensionContext) {
        context.getStore(NAMESPACE)
            .put(COVERAGE_RECORDER, CoverageRecorder(System.getProperty(TESTKIT_COVERAGE_OUTPUT)))
    }

    @OptIn(ExperimentalPathApi::class)
    private fun project(context: ExtensionContext): TestProject {
        val parameters = context.requiredTestClass.getAnnotation(TestKit::class.java) ?: TestKit()

        val locator = context.getStore(NAMESPACE).cache(LOCATOR) {
            parameters.locator.java.getDeclaredConstructor().newInstance() as ProjectLocator
        }

        return context.getStore(NAMESPACE).cache(PROJECT) {
            val tempProjectDir = createTempDirectory("junit-gradlekit")

            val location = locator.projectPath(System.getProperty(TESTKIT_PROJECTS), context)

            if (!location.exists()) {
                error { "expected a test project in $location" }
            }

            location.copyToRecursively(target = tempProjectDir, followLinks = false, overwrite = false)

            val collector = context.get<CoverageRecorder>(NAMESPACE, COVERAGE_RECORDER)

            // pipe the jacoco javaagent arguments into the new JVM that testkit launches
            System.getProperty(TESTKIT_JAVAAGENT)?.let { agent ->
                tempProjectDir.resolve("gradle.properties").apply {
                    if (!exists()) {
                        createFile()
                    }

                    appendText("\norg.gradle.jvmargs=-javaagent:$agent=output=tcpclient,port=${collector.port},sessionid=test\n")
                }
            }

            TestProject(tempProjectDir, parameters.cleanup)
        }
    }

    private inline fun <reified T> ExtensionContext.get(namespace: ExtensionContext.Namespace, key: String) = getStore(namespace).get(key, T::class.java)
    private inline fun <K, reified V> ExtensionContext.Store.cache(key: K, noinline f: (K) -> V) =
        getOrComputeIfAbsent(key, f, V::class.java)

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        parameterContext.parameter.type == TestProject::class.java

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        project(extensionContext)

    override fun afterTestExecution(context: ExtensionContext) {
        context.executionException.ifPresent {
            if (it !is UnexpectedBuildResultException) {
                context.get<TestProject>(NAMESPACE, PROJECT).logOutput()
            }
        }
    }
}
