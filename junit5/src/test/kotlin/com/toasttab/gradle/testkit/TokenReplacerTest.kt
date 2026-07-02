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
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Path

class TokenReplacerTest {
    @Test
    fun `no includes configured filters every file`() {
        val matchers = TokenReplacer.parseMatchers(null)

        expectThat(matchers).isEmpty()
        expectThat(TokenReplacer.shouldFilter(Path.of("anything.bin"), matchers)).isTrue()
        expectThat(TokenReplacer.shouldFilter(Path.of("a/b/c.gradle.kts"), matchers)).isTrue()
    }

    @Test
    fun `blank includes filters every file`() {
        val matchers = TokenReplacer.parseMatchers("  , \n ")

        expectThat(matchers).isEmpty()
        expectThat(TokenReplacer.shouldFilter(Path.of("anything.bin"), matchers)).isTrue()
    }

    @Test
    fun `bare name globs match files at any depth`() {
        val matchers = TokenReplacer.parseMatchers("*.gradle.kts, *.gradle")

        expectThat(TokenReplacer.shouldFilter(Path.of("build.gradle.kts"), matchers)).isTrue()
        expectThat(TokenReplacer.shouldFilter(Path.of("sub/build.gradle.kts"), matchers)).isTrue()
        expectThat(TokenReplacer.shouldFilter(Path.of("settings.gradle"), matchers)).isTrue()
        expectThat(TokenReplacer.shouldFilter(Path.of("gradle.properties"), matchers)).isFalse()
        expectThat(TokenReplacer.shouldFilter(Path.of("src/main/Foo.kt"), matchers)).isFalse()
    }

    @Test
    fun `path globs match against the relative path`() {
        val matchers = TokenReplacer.parseMatchers("**/*.gradle.kts")

        expectThat(TokenReplacer.shouldFilter(Path.of("sub/build.gradle.kts"), matchers)).isTrue()
        expectThat(TokenReplacer.shouldFilter(Path.of("src/main/Foo.kt"), matchers)).isFalse()
    }
}
