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

import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataWriter
import org.jacoco.core.data.IExecutionDataVisitor
import org.jacoco.core.data.ISessionInfoVisitor
import org.jacoco.core.data.SessionInfo
import org.jacoco.core.runtime.RemoteControlReader
import org.jacoco.core.runtime.RemoteControlWriter
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.FileOutputStream
import java.net.ServerSocket
import kotlin.concurrent.thread

internal class CoverageRecorder(
    outputFile: String
) : ExtensionContext.Store.CloseableResource, ISessionInfoVisitor, IExecutionDataVisitor {
    private val server = ServerSocket(0)

    private val output = FileOutputStream(outputFile)
    private val writer = ExecutionDataWriter(output)

    private val runner = thread {
        val sock = server.accept()

        RemoteControlWriter(sock.getOutputStream())

        val reader = RemoteControlReader(sock.getInputStream())
        reader.setSessionInfoVisitor(this)
        reader.setExecutionDataVisitor(this)

        while (reader.read()) {
        }

        writer.flush()
        sock.close()
    }

    val port: Int get() = server.localPort

    override fun visitSessionInfo(sess: SessionInfo) {
        writer.visitSessionInfo(sess)
    }

    override fun visitClassExecution(ex: ExecutionData) {
        writer.visitClassExecution(ex)
    }

    override fun close() {
        runner.join(10000)
        server.close()

        output.close()
    }
}
