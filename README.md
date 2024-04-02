# TestKit Plugins

[![Github Actions](https://github.com/open-toast/testkit-plugins/actions/workflows/ci.yml/badge.svg)](https://github.com/open-toast/testkit-plugins/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.toasttab.gradle.testkit/testkit-plugin)](https://search.maven.org/artifact/com.toasttab.gradle.testkit/testkit-plugin)
[![Gradle Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/toasttab/gradle/testkit/testkit-plugin/maven-metadata.xml.svg?label=gradle-portal&color=yellowgreen)](https://plugins.gradle.org/plugin/com.toasttab.testkit)

Provides a simple, opinionated structure for writing [TestKit](https://docs.gradle.org/current/userguide/test_kit.html)-based 
tests for Gradle plugins and collecting code coverage from them. Contains the following components:

* The _main plugin_, `com.toasttab.testkit` is applied to the plugin project.
* The _junit5 extension_ injects the test project model into JUnit 5 tests.
* The _coverage plugin_, `com.toasttab.testkit.coverage` is applied to TestKit fixture projects.

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

In the test project's `build.gradle.kts`, make sure to apply the coverage plugin.

```
plugins {
    id("com.toasttab.testkit.coverage") version <<version>>
}
```

Now, write the actual test. Note that a `TestProject` instance will be automatically injected into the test method.

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

## Parameterized Gradle versions

To run a test against multiple versions of Gradle, use the `@ParameterizedWithGradleVersions` annotation.
Gradle versions can be specified per class in the `@TestKit` annotation or per method in the 
`@ParameterizedWithGradleVersions` annotation. Each gradle version argument will be automatically 
injected into the runner created via `TestProject.createRunner`.

```kotlin
@TestKit(gradleVersions = ["8.6", "8.7"])
class ParameterizedTest {
    @Test
    @ParameterizedWithGradleVersions
    fun sometest(project: TestProject) {
        project.createRunner()
            .withArguments("check")
            .build() 
    }
}
```

## Code coverage

> [!WARNING]  
> Code coverage collection does not work on Gradle 8.7. 
> You have to run tests against Gradle 8.6 or below to collect coverage.
> You can use the parameterized Gradle version feature described above
> to run tests against both older and newer versions of Gradle.

It is notoriously difficult to collect code coverage data from TestKit tests. The root of the challenge
is that by default, TestKit tests launch in a separate Gradle daemon JVM, which lingers after the tests finish. 
This presents the following problems

* The TestKit JVM needs to start with the Jacoco agent attached to it.
* Jacoco data from the TestKit JVM is not flushed until the JVM terminates; however, the JVM lingers past the TestKit test execution.
* The lingering TestKit JVM may continue writing out jacoco coverage data while Gradle tries to collect the jacoco output file, resulting in intermittent build failures.
* TestKit tests and other unit tests cannot use the same jacoco output file because they may overwrite each other.

To solve these problems, the junit5 extension starts a Jacoco coverage TCP server and passes the right
Jacoco agent parameters into the TestKit build. The Jacoco coverage TCP server writes coverage data into a separate file.
The jacoco plugin applied to the TestKit project ensures that the coverage is flushed after the test finishes. 
And finally, the main plugin wires everything together.

See

* https://github.com/gradle/gradle/issues/1465
* https://github.com/gradle/gradle/issues/12535
