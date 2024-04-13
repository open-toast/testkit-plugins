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

package com.toasttab.gradle.testkit.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import java.io.File

private const val COPY_LOCAL_JARS_TASK = "copyLocalJars"
private const val INSTRUMENT_LOCAL_JARS_TASK = "instrumentLocalJars"

private fun Project.instrumentedDir() = layout.buildDirectory.dir("instrumented-local-jars")
private fun Project.localJarsDir() = layout.buildDirectory.dir("local-jars")

private fun Project.jacocoAgentRuntime() = zipTree(configurations.getAt(JacocoPlugin.AGENT_CONFIGURATION_NAME).asPath).filter {
    it.name == "jacocoagent.jar"
}.singleFile

fun Project.configureInstrumentation(coverageArtifact: Any) {
    tasks.register<CopyLocalJarsTask>(COPY_LOCAL_JARS_TASK) {
        artifacts = runtimeArtifacts()

        jar = tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME)

        dir = localJarsDir()
    }

    tasks.register<InstrumentWithJacocoOfflineTask>(INSTRUMENT_LOCAL_JARS_TASK) {
        dependsOn(COPY_LOCAL_JARS_TASK)

        classpath = configurations.getAt(JacocoPlugin.ANT_CONFIGURATION_NAME)

        jars = localJarsDir()
        dir = instrumentedDir()
    }

    val coverage = configurations.create("_coverage_plugin_")
    dependencies {
        add(coverage.name, coverageArtifact)
    }

    tasks.named<Test>(JavaPlugin.TEST_TASK_NAME) {
        dependsOn(INSTRUMENT_LOCAL_JARS_TASK)

        val runtimeArtifacts = runtimeArtifacts()

        inputs.files(runtimeArtifacts.artifactFiles).withPropertyName("plugin-artifacts").withPathSensitivity(
            PathSensitivity.RELATIVE
        )

        inputs.files(coverage).withPropertyName("coverage-artifacts").withPathSensitivity(
            PathSensitivity.RELATIVE
        )

        inputs.dir(instrumentedDir()).withPropertyName("instrumented-artifacts")
            .withPathSensitivity(PathSensitivity.RELATIVE)

        systemProperty("testkit-plugin-instrumented-jars", instrumentedDir().get().asFile.path)
        systemProperty("testkit-plugin-external-jars",
            runtimeArtifacts.filter(ArtifactResult::isExternalPluginDependency)
                .joinToString(separator = File.pathSeparator) {
                    it.file.path
                })
        systemProperty("testkit-coverage-jars", coverage.asPath)
        systemProperty("testkit-plugin-jacoco-jar", jacocoAgentRuntime().path)
    }
}
