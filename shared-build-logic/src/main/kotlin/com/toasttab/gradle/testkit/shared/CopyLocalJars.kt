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

package com.toasttab.gradle.testkit.shared

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

abstract class CopyLocalJars : DefaultTask() {
    @get:InputFiles
    val artifactFiles get() = artifacts.artifactFiles

    @Internal
    lateinit var artifacts: ArtifactCollection

    @Internal
    lateinit var jar: TaskProvider<Jar>

    @get:InputFile
    val jarFile get() = jar.map { it.archiveFile }

    @OutputDirectory
    lateinit var dir: Any

    @TaskAction
    fun copy() {
        project.copy {
            from(artifacts.filter(ArtifactResult::isProject).map {
                it.file
            })

            from(jar)

            into(dir)
        }
    }
}