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

package com.toasttab.gradle.testkit

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

abstract class TestkitExtension {
    abstract val testProjectsDir: Property<String>
    abstract val replaceTokens: MapProperty<String, String>

    // Globs restricting which files have their tokens replaced. When empty, every file is filtered.
    // Each glob is matched against both the path relative to the test project root and the bare
    // file name, so filterIncludes("*.gradle.kts", "*.gradle") narrows filtering to the build
    // scripts at any depth.
    abstract val filterIncludes: ListProperty<String>

    fun replaceToken(
        name: String,
        value: String
    ) {
        replaceTokens.put(name, value)
    }

    fun filterIncludes(vararg globs: String) {
        filterIncludes.addAll(*globs)
    }
}
