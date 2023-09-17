package com.toasttab.gradle.testkit

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

internal class JacocoOutputCleanupTestTaskAction(
    private val fs: FileSystemOperations,
    private val destinationFile: Provider<RegularFile>
) : Action<Task> {
    override fun execute(task: Task) {
        fs.delete {
            delete(destinationFile)
        }
    }
}
