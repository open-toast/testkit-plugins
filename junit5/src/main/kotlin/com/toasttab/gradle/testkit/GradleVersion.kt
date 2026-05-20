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

@Retention(AnnotationRetention.RUNTIME)
annotation class GradleVersion(
    val version: String,
    val properties: Array<Property> = []
)

@Retention(AnnotationRetention.RUNTIME)
annotation class Property(
    val key: String,
    val value: String
)

/**
 * One axis of a property matrix. The cartesian product of all axes on a [TestKit] or
 * [ParameterizedWithGradleVersions] is expanded into one test cell per combination.
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class Axis(
    val key: String,
    val values: Array<String>
)

internal fun cartesianProduct(axes: Array<Axis>): List<Map<String, String>> =
    axes.fold(listOf(emptyMap())) { acc, axis ->
        acc.flatMap { combo ->
            axis.values.map { value -> combo + (axis.key to value) }
        }
    }
