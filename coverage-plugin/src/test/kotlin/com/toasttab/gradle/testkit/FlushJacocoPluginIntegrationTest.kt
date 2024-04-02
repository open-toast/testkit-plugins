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

import com.toasttab.gradle.testkit.jacoco.JacocoRt
import org.gradle.testkit.runner.GradleRunner
import org.jacoco.core.data.ExecutionDataReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

class FlushJacocoPluginIntegrationTest {
    @TempDir
    lateinit var dir: Path

    @Test
    fun `coverage is flushed`() {
        val javaagent = System.getProperty("javaagent")
        val file = dir.resolve("build/testkit.exec")

        dir.resolve("gradle.properties").writeText(
            "org.gradle.jvmargs=-javaagent:$javaagent=destfile=$file"
        )

        dir.resolve("build.gradle.kts").writeText(
            """
                import java.lang.management.ManagementFactory
                
                plugins {
                    java
                    id("com.toasttab.testkit.coverage")
                }
                
                val loader = Class.forName("com.toasttab.gradle.testkit.jacoco.JacocoRt").classLoader
                println("loader = " + loader)
                println("urls = " + (loader as java.net.URLClassLoader).getURLs().toList())
                
                println("vm args = " + ManagementFactory.getRuntimeMXBean().getInputArguments())
                
            """.trimIndent()
        )

        GradleRunner.create()
            .withGradleVersion("8.6")
            .withProjectDir(dir.toFile())
            .withPluginClasspath().withArguments("build").build().also {
                println("output = ${it.output}")
            }

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

        expectThat(classes).contains(JacocoRt::class.java.name.replace('.', '/'))
    }
}
