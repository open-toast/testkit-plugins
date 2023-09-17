# TestKit Plugins

Provides a simple, opinionated structure for writing [TestKit](https://docs.gradle.org/current/userguide/test_kit.html)-based 
tests for Gradle plugins and collecting code coverage from them. Contains the following components:

* The _main plugin_, `com.toasttab.testkit` is applied to the plugin project.
* The _junit5 extension_ injects the test project model into junit5 tests.
* The _jacoco plugin_, `com.toasttab.testkit.jacoco` is applied to testkit test fixture projects.

## Setup

In the plugin project, apply the main plugin and bring in the junit5 extension dependency.

```
plugins {
    kotlin("jvm")
    jacoco
    id("com.toasttab.testkit") version <<version>>
}

dependencies {
    testImplementation("com.toasttab.gradle.testkit:junit5:<<version>>")
}
```

Each test method is expected to have a corresponding test project located at `src/test/projects/<<Test>>/<<method>>`.

```shell
src/test/projects/MyTest/sometest:
   build.gradle.kts
   settings.gradle.kts # optional, but IntelliJ will complain
```

In the test `build.gradle.kts`, make sure to apply the jacoco testkit plugin.

```
plugins {
    id("com.toasttab.testkit.jacoco") version <<version>>
}
```

Now, write the actual test

```kotlin
@TestKit
class MyTest {
    @Test
    fun sometest(project: TestProject) {
        project.createRunner()
            .withArguments("check")
            .build()
    }
}
```

## Code coverage

It is notoriously difficult to collect code coverage data from testkit tests. The root of the challenge
is that by default, testkit tests launch in a separate Gradle daemon JVM, which lingers after the tests finish. 
This presents the following problems

* The TestKit JVM needs to start with the jacoco agent attached to it.
* Jacoco data from the TestKit JVM is not flushed until the JVM terminates; however, the JVM lingers past the TestKit test execution.
* The lingering TestKit JVM may continue writing out jacoco coverage data while Gradle tries to collect the jacoco output file, resulting in intermittent build failures.
* TestKit tests and other unit tests cannot use the same jacoco output file because they may overwrite each other.

To solve these problems, the junit5 extension starts a jacoco coverage tcpserver and passes the right
jacoco javaagent parameters into the testkit build. The jacoco coverage server writes coverage data into a separate file.
The jacoco plugin applied to the testkit project ensures that the coverage is flushed after the test finishes. 
And finally, the main plugin wires everything together.

See

* https://github.com/gradle/gradle/issues/1465
* https://github.com/gradle/gradle/issues/12535
