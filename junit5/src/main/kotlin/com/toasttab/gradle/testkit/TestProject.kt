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
import java.io.StringWriter
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

class TestProject(
    val dir: Path,
    private val gradleVersion: GradleVersionArgument,
    private val cleanup: Boolean,
    private val initArgs: List<String> = emptyList()
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(TestProject::class.java)
    }

    private val output = StringWriter()
    private val outputLogged = AtomicBoolean()

    @OptIn(ExperimentalPathApi::class)
    fun close() {
        if (cleanup) {
            dir.deleteRecursively()
        }
    }

    fun build(vararg args: String) = createRunner(*args).build()

    fun buildAndFail(vararg args: String) = createRunner(*args).buildAndFail()

    private fun createRunner(vararg args: String) = createRunner().withArguments(initArgs + args)

    private fun createRunner() = GradleRunner.create()
        .withProjectDir(dir.toFile())
        .forwardStdOutput(output)
        .forwardStdError(output).apply {
            if (gradleVersion.version != null) {
                withGradleVersion(gradleVersion.version)
            }
        }
        .withArguments()

    fun logOutputOnce() {
        if (!outputLogged.getAndSet(true)) {
            LOGGER.warn("build output:\n{}", output)
        }
    }

    override fun toString() = "project(gradle: $gradleVersion)"
}
