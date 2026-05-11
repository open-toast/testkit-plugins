/*
 * Copyright (c) 2026 Toast Inc.
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

import org.apache.commons.text.StringSubstitutor
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

object TokenReplacer {
    private val fileTokens: Map<String, String> by lazy {
        val path = System.getProperty("testkit-tokens") ?: return@lazy emptyMap()
        val file = Path.of(path)
        if (!Files.exists(file)) return@lazy emptyMap()

        val props = Properties()
        file.toFile().inputStream().use { props.load(it) }
        props.stringPropertyNames().associateWith { props.getProperty(it) }
    }

    fun replaceInPlace(
        root: Path,
        extraTokens: Map<String, String>
    ) {
        val tokens = if (extraTokens.isEmpty()) fileTokens else fileTokens + extraTokens
        if (tokens.isEmpty()) return

        val substitutor = StringSubstitutor(tokens, "@", "@")

        Files.walk(root).use { stream ->
            stream.filter { it.isRegularFile() }.forEach { path ->
                val original =
                    try {
                        path.readText()
                    } catch (_: MalformedInputException) {
                        return@forEach
                    }

                val replaced = substitutor.replace(original)
                if (replaced != original) {
                    path.writeText(replaced)
                }
            }
        }
    }
}
