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

class GradleVersionArgument private constructor(
    val version: String?,
    val properties: Map<String, String> = emptyMap()
) {
    fun property(key: String): String? = properties[key]

    override fun toString() =
        when {
            version == null -> "default"
            properties.isEmpty() -> version
            else -> "$version${properties.entries.joinToString(prefix = " [", postfix = "]") { "${it.key}=${it.value}" }}"
        }

    companion object {
        fun of(
            version: String,
            properties: Map<String, String> = emptyMap()
        ) = GradleVersionArgument(version, properties)

        fun of(spec: GradleVersion) = GradleVersionArgument(spec.version, spec.properties.associate { it.key to it.value })

        val DEFAULT = GradleVersionArgument(null)
    }
}
