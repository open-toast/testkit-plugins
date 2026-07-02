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
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.Properties
import kotlin.io.path.CopyActionResult
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
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

    // Globs restricting which files are interpolated. Empty means every file is filtered;
    // otherwise files that match no glob are copied verbatim. Each glob is matched against both
    // the path relative to the test project root and the bare file name, so "*.gradle.kts" filters
    // build scripts at any depth. Usually narrowed to the build scripts, e.g.
    // "*.gradle.kts,*.gradle".
    private val filterMatchers: List<PathMatcher> by lazy {
        parseMatchers(System.getProperty("testkit-filter-includes"))
    }

    internal fun parseMatchers(spec: String?): List<PathMatcher> {
        if (spec == null) return emptyList()
        return spec
            .split(',', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { FileSystems.getDefault().getPathMatcher("glob:$it") }
    }

    internal fun shouldFilter(
        relative: Path,
        matchers: List<PathMatcher>
    ) = matchers.isEmpty() || matchers.any { it.matches(relative) || it.matches(relative.fileName) }

    fun copyAndReplace(
        source: Path,
        target: Path,
        extraTokens: Map<String, String>
    ) {
        val tokens = if (extraTokens.isEmpty()) fileTokens else fileTokens + extraTokens
        copyAndReplace(source, target, tokens, filterMatchers)
    }

    @OptIn(ExperimentalPathApi::class)
    internal fun copyAndReplace(
        source: Path,
        target: Path,
        tokens: Map<String, String>,
        matchers: List<PathMatcher>
    ) {
        if (tokens.isEmpty()) {
            source.copyToRecursively(target = target, followLinks = false, overwrite = false)
            return
        }

        val valueSource = MapBasedValueSource(tokens)

        source.copyToRecursively(target = target, followLinks = false) { src, tgt ->
            when {
                !src.isRegularFile() -> Files.createDirectories(tgt)
                shouldFilter(source.relativize(src), matchers) -> filterCopy(src, tgt, valueSource)
                else -> src.copyTo(tgt, overwrite = false)
            }
            CopyActionResult.CONTINUE
        }
    }

    private fun filterCopy(
        source: Path,
        target: Path,
        valueSource: MapBasedValueSource
    ) {
        // ISO-8859-1 round-trips every byte as a distinct char, so binary files pass through
        // untouched and UTF-8 text files are preserved byte-for-byte. Only ASCII @KEY@ tokens
        // need to match, and those map identically in both encodings.
        Files.newBufferedReader(source, StandardCharsets.ISO_8859_1).use { reader ->
            val interpolator = StringSearchInterpolator("@", "@")
            interpolator.addValueSource(valueSource)

            val filtered =
                MultiDelimiterInterpolatorFilterReader(reader, interpolator).apply {
                    addDelimiterSpec("@*@")
                }

            Files.newBufferedWriter(target, StandardCharsets.ISO_8859_1).use { writer ->
                filtered.copyTo(writer)
            }
        }
    }
}
