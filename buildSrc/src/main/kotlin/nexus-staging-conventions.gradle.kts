plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

if (isRelease()) {
    nexusPublishing {
        repositories {
            sonatype {
                username = Remote.username
                password = Remote.password
                nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            }
        }
    }
}
