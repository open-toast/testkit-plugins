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

import groovy.namespace.QName
import groovy.util.Node
import org.gradle.api.publish.maven.MavenPom

fun MavenPom.injectDependency(groupId: String, artifactId: String, version: String, classifier: String) {
    withXml {
        asNode().findOrCreateChild("dependencies").appendNode("dependency").apply {
            appendNode("groupId", groupId)
            appendNode("artifactId", artifactId)
            appendNode("version", version)
            appendNode("classifier", classifier)
        }
    }
}

private fun Node.findChild(localName: String) = children().filterIsInstance<Node>().firstOrNull { (it.name() as QName).localPart == localName }
private fun Node.findOrCreateChild(localName: String) = findChild(localName) ?: appendNode(localName)