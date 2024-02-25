package com.toasttab.gradle.testkit

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.readText

class TestkitPluginIntegrationTest {
    @TempDir
    lateinit var dir: Path

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun filtering() {
        Path.of(System.getProperty("test-projects")).copyToRecursively(target = dir, followLinks = false, overwrite = false)

        val projectDir = dir.resolve("TestkitPluginIntegrationTest/filtering")

        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("test")
            .build()

        val data = projectDir.resolve("build/test-projects/test-project/foo").readText().trim()

        Assertions.assertEquals("hello world!", data)
    }
}
