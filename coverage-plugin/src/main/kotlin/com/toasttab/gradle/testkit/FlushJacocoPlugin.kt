package com.toasttab.gradle.testkit

import com.toasttab.gradle.testkit.jacoco.JacocoRt
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Plugin
import org.gradle.api.Project

class FlushJacocoPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.gradle.addBuildListener(object : BuildAdapter() {
            override fun buildFinished(result: BuildResult) {
                JacocoRt.agent!!.dump(false)
            }
        })
    }
}
