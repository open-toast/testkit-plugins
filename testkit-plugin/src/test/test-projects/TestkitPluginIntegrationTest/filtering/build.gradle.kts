plugins {
    java
    jacoco
    id("com.toasttab.testkit")
}

repositories {
    mavenCentral()
}

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
}
