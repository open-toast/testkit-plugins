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
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private const val TIMEOUT_MS = 10_000L

private class ReaderTask(
    private val sock: Socket,
    private val writer: ExecutionDataWriter
) : Thread() {
    private val readingSession = AtomicBoolean(false)
    private val running = AtomicBoolean(true)

    private val reader = object : RemoteControlReader(sock.getInputStream()) {
        init {
            setSessionInfoVisitor { s ->
                readingSession.set(true)
            }

            setExecutionDataVisitor { ex ->
                synchronized(writer) {
                    writer.visitClassExecution(ex)
                }
            }
        }

        override fun readBlock(blockid: Byte): Boolean {
            if (blockid == 32.toByte()) {
                // OK message which follows jacoco tcpclient dumping execution data
                finishSession()
            }
            return super.readBlock(blockid)
        }

        override fun read() =
            try {
                super.read()
            } catch (e: Exception) {
                false
            }
    }

    init {
        start()
    }

    override fun run() {
        while (reader.read()) { }
    }

    fun finishSession() {
        readingSession.set(false)

        synchronized(writer) {
            writer.flush()
        }

        if (!running.get()) {
            sock.close()
        }
    }

    fun done() {
        running.set(false)

        if (!readingSession.get()) {
            sock.close()
        }

        join(TIMEOUT_MS)
    }
}

class CoverageRecorder(
    val settings: CoverageSettings
) : ExtensionContext.Store.CloseableResource {
    private val server = ServerSocket(0)

    private val output = FileOutputStream(settings.output, true)
    private val writer = ExecutionDataWriter(output)

    private val tasks = mutableListOf<ReaderTask>()

    private val runner = thread {
        while (!server.isClosed) {
            val sock = try {
                server.accept()
            } catch (e: Exception) {
                break
            }

            tasks.add(ReaderTask(sock, writer))
        }

        for (task in tasks) {
            task.done()
        }
    }

    val port: Int get() = server.localPort

    override fun close() {
        server.close()

        runner.join(TIMEOUT_MS)

        output.close()
    }
}
