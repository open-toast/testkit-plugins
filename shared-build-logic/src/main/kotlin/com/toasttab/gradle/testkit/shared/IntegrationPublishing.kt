package com.toasttab.gradle.testkit.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.testing.jacoco.plugins.JacocoPlugin

sealed interface RepositoryDescriptor {
    object MavenLocal : RepositoryDescriptor
    data class MavenRemote(val name: String) : RepositoryDescriptor

    companion object {
        const val INTEGRATION_REPO_NAME = "integration"
        val INTEGRATION = MavenRemote(INTEGRATION_REPO_NAME)
    }
}

data class PublicationDescriptor(val name: String) {
    fun isPlugin() = name.endsWith("PluginMarkerMaven")

    companion object {
        const val INTEGRATION_PUBLICATION_NAME = "integration"
        val INTEGRATION = PublicationDescriptor(INTEGRATION_PUBLICATION_NAME)
    }
}

fun Project.publishOnlyIf(predicate: (PublicationDescriptor, RepositoryDescriptor) -> Boolean) {
    project.tasks.withType<PublishToMavenLocal> {
        onlyIf {
            predicate(PublicationDescriptor(publication.name), RepositoryDescriptor.MavenLocal)
        }
    }

    project.tasks.withType<PublishToMavenRepository> {
        onlyIf {
            predicate(PublicationDescriptor(publication.name), RepositoryDescriptor.MavenRemote(repository.name))
        }
    }
}

val Project.integrationRepo get() = rootProject.layout.buildDirectory.dir("integration-repo").get().asFile.path

fun Project.configureIntegrationPublishing(
    configuration: String = "runtimeClasspath"
) {
    val repo = integrationRepo

    afterEvaluate {
        val jacocoAnt = project.configurations.findByName(JacocoPlugin.ANT_CONFIGURATION_NAME)

        configurations.getAt(configuration).incoming.artifactView {
            lenient(true)
            attributes.attribute(Attribute.of("artifactType", String::class.java), "jar")
        }.artifacts.map {
            it.id.componentIdentifier
        }.filterIsInstance<ProjectComponentIdentifier>().forEach {
            configureIntegrationPublishingForDependency(project(":${it.projectPath}"), repo, jacocoAnt)
        }

        configureIntegrationPublishingForDependency(this, repo, jacocoAnt)
    }

    tasks.named("test") {
        dependsOn("publishIntegrationPublicationToIntegrationRepository")
    }
}

private fun Project.configureIntegrationPublishingForDependency(project: Project, repo: Any, jacocoAnt: Configuration?) {
    project.pluginManager.apply("maven-publish")

    if (jacocoAnt != null) {
        project.tasks.register<InstrumentWithJacocoOfflineTask>("instrument") {
            dependsOn("jar")

            classpath = jacocoAnt

            jar = project.tasks.named<Jar>("jar").flatMap { it.archiveFile }

            dir = project.layout.buildDirectory.dir("instrumented")
        }
    }

    project.extensions.configure<PublishingExtension>("publishing") {
        repositories {
            maven {
                name = RepositoryDescriptor.INTEGRATION_REPO_NAME
                url = project.uri("file://$repo")
            }
        }

        publications {
            create<MavenPublication>(PublicationDescriptor.INTEGRATION_PUBLICATION_NAME) {
                from(project.components["java"])

                if (jacocoAnt != null) {
                    artifacts.clear()

                    artifact(project.layout.buildDirectory.file("instrumented/${project.name}-${project.version}.jar")) {
                        builtBy(project.tasks.named("instrument"))
                    }
                }
            }
        }
    }

    tasks.named("test") {
        dependsOn("${project.path}:publishIntegrationPublicationToIntegrationRepository")
    }

    project.extensions.findByType(
        GradlePluginDevelopmentExtension::class.java
    )?.plugins?.forEach { plugin ->
        val name = "publish" + plugin.name.capitalize() + "PluginMarkerMavenPublicationToIntegrationRepository"

        tasks.named("test") {
            dependsOn("${project.path}:$name")
        }
    }

    if (jacocoAnt != null) {
        project.tasks.named<GenerateModuleMetadata>("generateMetadataFileForIntegrationPublication") {
            enabled = false
        }
    }

    project.publishOnlyIf { publication, repository ->
        if (publication == PublicationDescriptor.INTEGRATION) {
            repository == RepositoryDescriptor.INTEGRATION
        } else if (repository == RepositoryDescriptor.INTEGRATION) {
            publication.isPlugin()
        } else {
            true
        }
    }
}
