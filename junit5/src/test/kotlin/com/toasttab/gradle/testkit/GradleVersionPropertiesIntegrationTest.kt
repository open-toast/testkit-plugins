/*
 * Copyright (c) 2026 Toast Inc.
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

import strikt.api.expectThat
import strikt.assertions.isEqualTo

@TestKit(
    versions = [
        GradleVersion(
            version = "8.6",
            properties = [Property(key = "kotlin", value = "1.9.24")]
        ),
        GradleVersion(
            version = "8.7",
            properties = [Property(key = "kotlin", value = "2.0.0")]
        )
    ]
)
class GradleVersionPropertiesIntegrationTest {
    @ParameterizedWithGradleVersions
    fun `per-version properties are attached to the test project`(project: TestProject) {
        val expectedKotlin = when (project.gradleVersion.version) {
            "8.6" -> "1.9.24"
            "8.7" -> "2.0.0"
            else -> error("unexpected gradle version ${project.gradleVersion.version}")
        }

        expectThat(project.property("kotlin")).isEqualTo(expectedKotlin)
    }
}
