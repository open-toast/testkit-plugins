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

package com.toasttab.gradle.testkit.jacoco

interface JacocoAgent {
    val location: String
    fun dump(reset: Boolean)

    val includes: String

    val excludes: String
}

private class ReflectiveJacocoAgent(
    private val agent: Any
) : JacocoAgent {
    private val options by lazy {
        // agent.options
        agent.javaClass.getDeclaredField("options").apply { isAccessible = true }.get(agent)
    }

    override val location: String
        get() = agent.javaClass.protectionDomain.codeSource.location.file
    override val includes: String
        get() = options.javaClass.getMethod("getIncludes").invoke(options) as String
    override val excludes: String
        get() = options.javaClass.getMethod("getExcludes").invoke(options) as String

    override fun dump(reset: Boolean) {
        agent.javaClass.getMethod("dump", Boolean::class.java).invoke(agent, reset)
    }
}

/**
 * Provides reflective access to the jacoco agent attached to the current JVM
 */
object JacocoRt {
    val agent: JacocoAgent? by lazy {
        runCatching {
            val rt = ClassLoader.getSystemClassLoader().loadClass("org.jacoco.agent.rt.RT")
            ReflectiveJacocoAgent(rt.getMethod("getAgent").invoke(null))
        }.getOrNull()
    }
}
