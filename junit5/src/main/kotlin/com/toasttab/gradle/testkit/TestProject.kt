/*
 * Copyright (c) 2023 Toast Inc.
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
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.StringWriter
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

class TestProject(
    val dir: Path,
    private val cleanup: Boolean
) : Closeable {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(TestProject::class.java)
    }

    private val output = StringWriter()

    @OptIn(ExperimentalPathApi::class)
    override fun close() {
        if (cleanup) {
            dir.deleteRecursively()
        }
    }

    fun createRunner() = createRunnerWithoutPluginClasspath()
        .withPluginClasspath()

    fun createRunnerWithoutPluginClasspath() = GradleRunner.create()
        .withProjectDir(dir.toFile())
        .forwardStdOutput(output)
        .forwardStdError(output)

    fun logOutput() {
        LOGGER.warn("build output:\n{}", output)
    }
}
