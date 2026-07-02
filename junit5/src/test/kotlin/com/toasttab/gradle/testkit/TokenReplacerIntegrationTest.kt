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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class TokenReplacerIntegrationTest {
    @TempDir
    lateinit var source: Path

    @TempDir
    lateinit var target: Path

    // 0x00..0xFF, including bytes that are not valid UTF-8, so we prove binary files round-trip.
    private val binary = ByteArray(256) { it.toByte() }

    @Test
    fun `interpolates matching files while copying others verbatim`() {
        source.resolve("build.gradle.kts").writeText("kotlin = \"@kotlin@\"")
        source.resolve("nested").createDirectories()
        source.resolve("nested/settings.gradle.kts").writeText("name = \"@name@\"")
        source.resolve("gradle.properties").writeText("kept = @kotlin@")
        source.resolve("logo.png").writeBytes(binary)

        TokenReplacer.copyAndReplace(
            source,
            target,
            tokens = mapOf("kotlin" to "2.3.21", "name" to "demo"),
            matchers = TokenReplacer.parseMatchers("*.gradle.kts")
        )

        // build scripts at any depth have their tokens replaced
        expectThat(target.resolve("build.gradle.kts").readText()).isEqualTo("kotlin = \"2.3.21\"")
        expectThat(target.resolve("nested/settings.gradle.kts").readText()).isEqualTo("name = \"demo\"")

        // files that match no glob are copied verbatim, tokens untouched
        expectThat(target.resolve("gradle.properties").readText()).isEqualTo("kept = @kotlin@")

        // binary files round-trip byte-for-byte
        expectThat(target.resolve("logo.png").readBytes().toList()).containsExactly(binary.toList())
    }

    @Test
    fun `no matchers filters every file`() {
        source.resolve("build.gradle.kts").writeText("kotlin = \"@kotlin@\"")
        source.resolve("gradle.properties").writeText("kept = @kotlin@")

        TokenReplacer.copyAndReplace(
            source,
            target,
            tokens = mapOf("kotlin" to "2.3.21"),
            matchers = emptyList()
        )

        expectThat(target.resolve("build.gradle.kts").readText()).isEqualTo("kotlin = \"2.3.21\"")
        expectThat(target.resolve("gradle.properties").readText()).isEqualTo("kept = 2.3.21")
    }
}
