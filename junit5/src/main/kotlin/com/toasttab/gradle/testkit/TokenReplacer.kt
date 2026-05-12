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

import org.codehaus.plexus.interpolation.MapBasedValueSource
import org.codehaus.plexus.interpolation.StringSearchInterpolator
import org.codehaus.plexus.interpolation.multi.MultiDelimiterInterpolatorFilterReader
import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties
import kotlin.io.path.isRegularFile

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

        val valueSource = MapBasedValueSource(tokens)

        Files.walk(root).use { stream ->
            stream.filter { it.isRegularFile() }.forEach { path -> replaceInFile(path, valueSource) }
        }
    }

    private fun replaceInFile(
        path: Path,
        valueSource: MapBasedValueSource
    ) {
        val tmp = path.resolveSibling("${path.fileName}.tokens.tmp")

        // ISO-8859-1 round-trips every byte as a distinct char, so binary files pass through
        // untouched and UTF-8 text files are preserved byte-for-byte. Only ASCII @KEY@ tokens
        // need to match, and those map identically in both encodings.
        Files.newBufferedReader(path, StandardCharsets.ISO_8859_1).use { reader ->
            val interpolator = StringSearchInterpolator("@", "@")
            interpolator.addValueSource(valueSource)

            val filtered =
                BufferedReader(
                    MultiDelimiterInterpolatorFilterReader(reader, interpolator).apply {
                        addDelimiterSpec("@*@")
                    }
                )

            Files.newBufferedWriter(tmp, StandardCharsets.ISO_8859_1).use { writer ->
                filtered.copyTo(writer)
            }
        }

        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun BufferedReader.copyTo(writer: BufferedWriter) {
        val buf = CharArray(8192)
        while (true) {
            val n = read(buf)
            if (n < 0) break
            writer.write(buf, 0, n)
        }
    }
}
