package com.toasttab.gradle.testkit

object JacocoRt {
    /**
     * Calls `RT.getAgent().dump()` to flush Jacoco coverage data
     * Has to be done reflectively because of Gradle's classloader structure
     * See https://www.jacoco.org/jacoco/trunk/doc/api/org/jacoco/agent/rt/IAgent.html#dump(boolean)
     **/
    fun dump(reset: Boolean) {
        val rtClass = try {
            ClassLoader.getSystemClassLoader().loadClass("org.jacoco.agent.rt.RT")
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("jacoco agent is not attached to the current JVM", e)
        }

        // agent = RT.getAgent()
        val agent = rtClass.getMethod("getAgent").invoke(null)

        // agent.dump(reset)
        agent.javaClass.getMethod("dump", Boolean::class.java).invoke(agent, reset)
    }
}
