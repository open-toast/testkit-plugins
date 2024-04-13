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
    id("com.toasttab.testkit.coverage")
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

It is notoriously difficult to collect code coverage data from TestKit tests. By default, TestKit tests launch in 
a separate Gradle daemon JVM, which lingers after the tests finish. Gradle attaches an agent to the daemon JVM
which instruments all classes from the plugin classpath and makes it impossible for the Jacoco agent to instrument
classes on the fly.

* The main plugin pre-instruments the plugin classes and other project classes that the plugin under test depends on
  using [Jacoco offline instrumentation](https://www.jacoco.org/jacoco/trunk/doc/offline.html).
* The junit5 extension configures the TestKit build to use pre-instrumented classes, starts a Jacoco coverage TCP server, 
  and points the jacoco runtime to the TCP server. 
* The TCP server writes coverage data into a separate file and stops writing the file when the tests finish running. 
  This allows the main Gradle process to collect task outputs even though the TestKit process might still be lingering.
* The jacoco plugin applied to the TestKit project ensures that the coverage is fully flushed after the test finishes. 
  This ensures that the coverage is recorded even though the TestKit process might still be lingering.

See

* https://github.com/gradle/gradle/issues/1465
* https://github.com/gradle/gradle/issues/12535
* https://github.com/gradle/gradle/issues/27328