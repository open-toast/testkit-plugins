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

import org.jacoco.core.data.ExecutionDataReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

class FlushJacocoPluginIntegrationTest {
    @TempDir
    lateinit var dir: Path

    @Test
    fun `coverage is flushed`() {
        val file = dir.resolve("testkit.exec")
        val recorder = CoverageRecorder(CoverageSettings("", "", file.toString()))

        for (i in 1..5) {
            val projectDir = dir.resolve("project$i")
            projectDir.createDirectory()

            projectDir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    java
                    id("com.toasttab.testkit.coverage")
                    id("com.toasttab.testkit.integration.test$i")
                }
                """.trimIndent()
            )

            TestProjectExtension.createProject(
                projectDir = projectDir,
                gradleVersion = GradleVersionArgument.of("8.7"),
                cleanup = true,
                coverageRecorder = recorder
            ).build("build", "--configuration-cache", "--stacktrace")
        }

        recorder.close()

        val classes = hashSetOf<String>()

        file.inputStream().buffered().use {
            ExecutionDataReader(it).apply {
                setExecutionDataVisitor { data ->
                    if (data.name.startsWith("com/toasttab")) {
                        classes.add(data.name)
                    }
                }

                setSessionInfoVisitor { }
            }.read()
        }

        expectThat(classes).contains(
            (1..5).map { "com/toasttab/gradle/testkit/TestPlugin$it" }
        )
    }
}
