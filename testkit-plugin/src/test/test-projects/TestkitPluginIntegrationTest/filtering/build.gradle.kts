plugins {
    java
    id("com.toasttab.testkit")
}

repositories {
    mavenCentral()
}

group = "com.toasttab.testkit.test"
version = "1.0"

tasks {
    test {
        useJUnitPlatform()
    }
}

testkitTests {
    replaceToken("VALUE", "world!")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
