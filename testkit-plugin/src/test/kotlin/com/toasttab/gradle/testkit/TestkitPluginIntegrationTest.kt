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

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.readText

class TestkitPluginIntegrationTest {
    @TempDir
    lateinit var dir: Path

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun filtering() {
        Path.of(System.getProperty("test-projects")).copyToRecursively(target = dir, followLinks = false, overwrite = false)

        val projectDir = dir.resolve("TestkitPluginIntegrationTest/filtering")

        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("test")
            .build()

        val data = projectDir.resolve("build/test-projects/test-project/foo").readText().trim()

        expectThat(data).isEqualTo("hello world!")
    }
}
