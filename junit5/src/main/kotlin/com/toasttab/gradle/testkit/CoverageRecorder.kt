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

import org.jacoco.core.data.ExecutionDataWriter
import org.jacoco.core.runtime.RemoteControlReader
import org.jacoco.core.runtime.RemoteControlWriter
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.FileOutputStream
import java.net.ServerSocket
import kotlin.concurrent.thread

internal class CoverageRecorder(
    settings: CoverageSettings
) : ExtensionContext.Store.CloseableResource {
    private val server = ServerSocket(0)

    private val output = FileOutputStream(settings.output, true)
    private val writer = ExecutionDataWriter(output)

    private val threads = mutableListOf<Thread>()

    private val runner = thread {
        while (!server.isClosed) {
            val sock = server.accept()

            threads.add(
                thread {
                    RemoteControlWriter(sock.getOutputStream())

                    val reader = RemoteControlReader(sock.getInputStream())
                    reader.setSessionInfoVisitor { }

                    reader.setExecutionDataVisitor { ex ->
                        synchronized(writer) {
                            writer.visitClassExecution(ex)
                        }
                    }

                    reader.setRemoteCommandVisitor { _, _ -> }

                    while (reader.read()) {
                    }

                    synchronized(writer) {
                        writer.flush()
                    }
                    sock.close()
                }
            )
        }
    }

    val port: Int get() = server.localPort

    override fun close() {
        for (thread in threads) {
            thread.join(10000)
        }

        server.close()
        output.close()
    }
}
