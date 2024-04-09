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
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME

private val ARTIFACT_TYPE_ATTRIBUTE = Attribute.of("artifactType", String::class.java)

fun Project.runtimeArtifacts() = configurations.getAt(RUNTIME_CLASSPATH_CONFIGURATION_NAME).incoming.artifactView {
    lenient(true)
    attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, "jar")
}.artifacts

fun ArtifactResult.isProject() = id.componentIdentifier is ProjectComponentIdentifier

fun ArtifactResult.isExternalPluginDependency(): Boolean {
    val identifier = id.componentIdentifier

    return identifier is ModuleComponentIdentifier && identifier.group != "org.jetbrains.kotlin"
}