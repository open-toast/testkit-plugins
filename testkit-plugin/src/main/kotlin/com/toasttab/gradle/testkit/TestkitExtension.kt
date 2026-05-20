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

    /**
     * Maven repository URLs that the testkit init script should declare under
     * `pluginManagement.repositories` for plugin resolution by the test project. If empty
     * (the default), the init script falls back to `gradlePluginPortal()`. If non-empty, the
     * listed URLs replace `gradlePluginPortal()` entirely — useful when your project resolves
     * plugins from an internal mirror and shouldn't reach out to the public portal.
     */
    abstract val pluginRepositories: ListProperty<String>

    fun replaceToken(
        name: String,
        value: String
    ) {
        replaceTokens.put(name, value)
    }
}
