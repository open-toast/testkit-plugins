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

package com.toasttab.gradle.testkit

class CoverageSettings(
    val javaagent: String,
    val output: String
) {
    companion object {
        val settings by lazy {
            val javaagent = System.getProperty(TESTKIT_JAVAAGENT)
            val output = System.getProperty(TESTKIT_COVERAGE_OUTPUT)

            if (javaagent != null && output != null) {
                CoverageSettings(javaagent, output)
            } else {
                null
            }
        }
    }
}