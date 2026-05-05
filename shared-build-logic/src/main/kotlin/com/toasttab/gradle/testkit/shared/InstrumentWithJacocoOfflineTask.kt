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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.FileOutputStream
import javax.inject.Inject

abstract class InstrumentWithJacocoOfflineTask : DefaultTask() {
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:InputFile
    abstract val jar: RegularFileProperty

    @get:OutputDirectory
    abstract val dir: DirectoryProperty

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun instrument() {
        workerExecutor.classLoaderIsolation {
            classpath.from(this@InstrumentWithJacocoOfflineTask.classpath)
        }.submit(InstrumentAction::class.java) {
            inputJar.set(jar)
            outputDir.set(dir)
        }
    }

    interface InstrumentParameters : WorkParameters {
        val inputJar: RegularFileProperty
        val outputDir: DirectoryProperty
    }

    abstract class InstrumentAction : WorkAction<InstrumentParameters> {
        override fun execute() {
            val inputJar = parameters.inputJar.get().asFile
            val outputJar = parameters.outputDir.get().file(inputJar.name).asFile

            outputJar.parentFile.mkdirs()

            val instrumenterClass = Class.forName("org.jacoco.core.instr.Instrumenter")
            val generatorClass = Class.forName("org.jacoco.core.runtime.IExecutionDataAccessorGenerator")
            val offlineGeneratorClass = Class.forName("org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator")

            val instrumenter = instrumenterClass
                .getConstructor(generatorClass)
                .newInstance(offlineGeneratorClass.getConstructor().newInstance())

            val instrumentAll = instrumenterClass.getMethod(
                "instrumentAll",
                java.io.InputStream::class.java,
                java.io.OutputStream::class.java,
                String::class.java
            )

            inputJar.inputStream().buffered().use { input ->
                FileOutputStream(outputJar).buffered().use { output ->
                    instrumentAll.invoke(instrumenter, input, output, inputJar.name)
                }
            }
        }
    }
}
