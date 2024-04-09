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

package com.toasttab.gradle.testkit.jacoco

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class JacocoRtTest {
    @Test
    fun `jacoco includes are correct`() {
        expectThat(JacocoRt.agent).isNotNull().and {
            get { includes }.isEqualTo("com.toasttab.testkit.*")
        }
    }

    @Test
    fun `jacoco excludes are correct`() {
        expectThat(JacocoRt.agent).isNotNull().and {
            get { excludes }.isEqualTo("com.unrelated")
        }
    }
}
