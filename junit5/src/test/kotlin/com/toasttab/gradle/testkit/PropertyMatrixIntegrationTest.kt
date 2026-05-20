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
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isContainedIn
import strikt.assertions.isNotNull
import java.util.concurrent.ConcurrentHashMap

private val OBSERVED = ConcurrentHashMap.newKeySet<Pair<String, String>>()

@TestKit(
    matrix = [
        Axis(key = "alpha", values = ["a1", "a2"]),
        Axis(key = "beta", values = ["b1", "b2", "b3"])
    ]
)
class PropertyMatrixIntegrationTest {
    @ParameterizedWithGradleVersions
    fun `cartesian product of axes is expanded into one cell per combination`(project: TestProject) {
        val alpha = project.property("alpha")
        val beta = project.property("beta")

        expectThat(alpha).isNotNull().isContainedIn(setOf("a1", "a2"))
        expectThat(beta).isNotNull().isContainedIn(setOf("b1", "b2", "b3"))

        OBSERVED.add(alpha!! to beta!!)
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun assertAllCellsRan() {
            expectThat(OBSERVED).containsExactlyInAnyOrder(
                "a1" to "b1", "a1" to "b2", "a1" to "b3",
                "a2" to "b1", "a2" to "b2", "a2" to "b3"
            )
        }
    }
}
