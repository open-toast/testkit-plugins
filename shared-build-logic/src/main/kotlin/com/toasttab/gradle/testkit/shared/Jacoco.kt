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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.testing.jacoco.plugins.JacocoPlugin

sealed interface CoverageConfiguration {
    object None: CoverageConfiguration

    class Jacoco(
        val configuration: Configuration
    ): CoverageConfiguration {
        val version by lazy {
            configuration.artifacts().map { it.id.componentIdentifier }
                .filterIsInstance<ModuleComponentIdentifier>()
                .first { it.group == "org.jacoco" && it.module == "org.jacoco.ant" }
                .version
        }
    }
}

fun Project.coverage() = configurations.findByName(JacocoPlugin.ANT_CONFIGURATION_NAME)?.let {
    CoverageConfiguration.Jacoco(it)
} ?: CoverageConfiguration.None