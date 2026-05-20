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

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import java.util.concurrent.ConcurrentHashMap

private val OBSERVED = ConcurrentHashMap.newKeySet<Pair<String, String>>()

/**
 * Demonstrates that a [TestProject] can be injected into a regular `@ParameterizedTest` method
 * driven by `@MethodSource`. JUnit's `ParameterizedTestParameterResolver` claims parameter slots
 * left-to-right starting from index 0 for arguments-source values, so the extension-injected
 * [TestProject] must be the last parameter.
 */
@TestKit
class MethodSourceInteropIntegrationTest {
    @ParameterizedTest
    @MethodSource("matrix")
    fun `TestProject is injected alongside MethodSource arguments`(
        kotlin: String,
        ktlint: String,
        project: TestProject
    ) {
        OBSERVED.add(kotlin to ktlint)

        // Sanity-check that the project is real and the cell is identified by the row args.
        check(project.dir.toFile().exists()) { "expected real test project for $kotlin/$ktlint" }
    }

    companion object {
        @JvmStatic
        fun matrix(): List<Arguments> =
            listOf("1.9.24", "2.0.0").flatMap { kotlin ->
                listOf("1.5.0", "1.7.1").map { ktlint ->
                    Arguments.of(kotlin, ktlint)
                }
            }

        @JvmStatic
        @AfterAll
        fun assertAllRowsRan() {
            expectThat(OBSERVED).containsExactlyInAnyOrder(
                "1.9.24" to "1.5.0", "1.9.24" to "1.7.1",
                "2.0.0" to "1.5.0", "2.0.0" to "1.7.1"
            )
        }
    }
}
