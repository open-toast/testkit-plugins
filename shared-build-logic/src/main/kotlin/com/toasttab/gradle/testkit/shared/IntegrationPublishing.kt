/*
 * Copyright (c) 2024 Toast Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.toasttab.gradle.testkit.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.internal.plugins.PluginDescriptor
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugin.devel.PluginDeclaration
import java.net.URI

sealed interface RepositoryDescriptor {
    fun isIntegration(): Boolean

    object MavenLocal : RepositoryDescriptor {
        override fun isIntegration() = false
    }

    class MavenRemote(val name: String, val url: URI) : RepositoryDescriptor {
        override fun isIntegration() = name.startsWith(INTEGRATION_REPO_NAME_PREFIX)

        val capitalizedName by lazy { name.simpleCapitalize() }
    }

    companion object {
        const val INTEGRATION_REPO_NAME_PREFIX = "testkitIntegrationFor"
    }
}

data class PublicationDescriptor(val name: String) {
    fun isPlugin() = name.endsWith("PluginMarkerMaven")

    companion object {
        const val INTEGRATION_PUBLICATION_NAME = "testkitIntegration"
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
            predicate(PublicationDescriptor(publication.name), RepositoryDescriptor.MavenRemote(repository.name, repository.url))
        }
    }
}

fun Project.integrationDirectory() = layout.buildDirectory.dir("testkit-integration-repo").get().asFile

fun Project.configureIntegrationPublishing(
    configuration: String = "runtimeClasspath"
) {
    val repo = RepositoryDescriptor.MavenRemote(
        name = RepositoryDescriptor.INTEGRATION_REPO_NAME_PREFIX + path.split(Regex("\\W+")).mapIndexed { i, s ->
            if (i == 0) {
                s
            } else {
                s.simpleCapitalize()
            }
        }.joinToString(separator = ""),
        url = integrationDirectory().toURI()
    )

    afterEvaluate {
        val coverage = project.coverage()

        configurations.getAt(configuration).incoming.artifactView {
            lenient(true)
            attributes.attribute(Attribute.of("artifactType", String::class.java), "jar")
        }.artifacts.map {
            it.id.componentIdentifier
        }.filterIsInstance<ProjectComponentIdentifier>().forEach {
            configureIntegrationPublishingForDependency(project(":${it.projectPath}"), repo, coverage)
        }

        configureIntegrationPublishingForDependency(this, repo, coverage)
    }

    tasks.named("test") {
        dependsOn("publishTestkitIntegrationPublicationTo${repo.capitalizedName}Repository")
    }
}

private fun Project.configureIntegrationPublishingForDependency(project: Project, repo: RepositoryDescriptor.MavenRemote, coverage: CoverageConfiguration) {
    project.pluginManager.apply("maven-publish")

    if (coverage is CoverageConfiguration.Jacoco) {
        project.tasks.register<InstrumentWithJacocoOfflineTask>("instrument") {
            dependsOn("jar")

            classpath = coverage.configuration

            jar = project.tasks.named<Jar>("jar").flatMap { it.archiveFile }

            dir = project.layout.buildDirectory.dir("instrumented")
        }
    }

    project.extensions.configure<PublishingExtension>("publishing") {
        repositories {
            maven {
                name = repo.name
                url = repo.url
            }
        }

        publications {
            create<MavenPublication>(PublicationDescriptor.INTEGRATION_PUBLICATION_NAME) {
                from(project.components["java"])

                if (coverage is CoverageConfiguration.Jacoco) {
                    pom {
                        injectDependency(
                            groupId = "org.jacoco",
                            artifactId = "org.jacoco.agent",
                            version = coverage.version,
                            classifier = "runtime"
                        )
                    }

                    artifacts.clear()

                    artifact(project.layout.buildDirectory.file("instrumented/${project.name}-${project.version}.jar")) {
                        builtBy(project.tasks.named("instrument"))
                    }
                }
            }
        }
    }

    tasks.named("test") {
        dependsOn("${project.path}:publishTestkitIntegrationPublicationTo${repo.capitalizedName}Repository")
    }

    tasks.named<Test>("test") {
        val extension = project.extensions.findByType<GradlePluginDevelopmentExtension>()

        if (extension == null) {
            logger.warn("No GradlePluginDevelopmentExtension found for project ${project.path}")
        } else {
            if (extension.plugins.isEmpty()) {
                logger.warn("No plugins are declared in project ${project.path}")
            } else {
                for (plugin in extension.plugins) {
                    dependsOn("${project.path}:${plugin.publishTask(repo)}")
                }
            }

            systemProperty("testkit-plugin-ids", extension.plugins.joinToString(separator = ",") { it.id })
        }

        systemProperty("testkit-project-version", "${project.version}")
    }

    if (coverage is CoverageConfiguration.Jacoco) {
        project.tasks.named<GenerateModuleMetadata>("generateMetadataFileForTestkitIntegrationPublication") {
            enabled = false
        }
    }

    project.publishOnlyIf { publication, repository ->
        if (publication == PublicationDescriptor.INTEGRATION) {
            repository.isIntegration()
        } else if (repository.isIntegration()) {
            publication.isPlugin()
        } else {
            true
        }
    }
}

private fun PluginDeclaration.publishTask(repo: RepositoryDescriptor.MavenRemote) =
    "publish${name.simpleCapitalize()}PluginMarkerMavenPublicationTo${repo.capitalizedName}Repository"

private fun String.simpleCapitalize() = replaceFirstChar(Char::titlecaseChar)