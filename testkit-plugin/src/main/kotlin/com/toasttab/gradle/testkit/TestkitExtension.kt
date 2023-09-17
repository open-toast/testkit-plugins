package com.toasttab.gradle.testkit

open class TestkitExtension {
    var testProjectsDir: String = "src/test/projects"
    val replaceTokens = mutableMapOf<String, String>()

    fun replaceToken(name: String, value: String) {
        replaceTokens[name] = value
    }
}
