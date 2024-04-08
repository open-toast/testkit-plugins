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
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import java.io.File

private fun Project.instrumentedDir() = layout.buildDirectory.dir("instrumented-local-jars")
private fun Project.localJarsDir() = layout.buildDirectory.dir("local-jars")

private fun Project.jacocoAgentRuntime() = zipTree(configurations.getAt("jacocoAgent").asPath).filter {
    it.name == "jacocoagent.jar"
}.singleFile

private fun ArtifactCollection.externalPluginDependencies() = filter {
    val identifier = it.id.componentIdentifier

    identifier is ModuleComponentIdentifier && identifier.group != "org.jetbrains.kotlin"
}

fun Project.configureInstrumentation() {
    pluginManager.withPlugin("jacoco") {
        tasks.register<CopyLocalJars>("copyLocalJars") {
            artifacts = runtimeArtifacts()

            jar = tasks.named<Jar>("jar")

            dir = localJarsDir()
        }

        tasks.register<InstrumentWithJacocoOffline>("instrumentLocalJars") {
            dependsOn("copyLocalJars")

            classpath = configurations.getAt("jacocoAnt")

            jars = localJarsDir()
            dir = instrumentedDir()
        }

        tasks.named<Test>("test") {
            dependsOn("instrumentLocalJars")

            val runtimeArtifacts = runtimeArtifacts()

            inputs.files(runtimeArtifacts.artifactFiles).withPropertyName("plugin-artifacts").withPathSensitivity(
                PathSensitivity.RELATIVE
            )
            inputs.dir(instrumentedDir())

            systemProperty("testkit-plugin-instrumented-jars", instrumentedDir().get().asFile.path)
            systemProperty("testkit-plugin-external-jars", runtimeArtifacts.externalPluginDependencies()
                .joinToString(separator = File.pathSeparator) {
                    it.file.path
                })
            systemProperty("testkit-plugin-jacoco-jar", jacocoAgentRuntime().path)
        }
    }
}
