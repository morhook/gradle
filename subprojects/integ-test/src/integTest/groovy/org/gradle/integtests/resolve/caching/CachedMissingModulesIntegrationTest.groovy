/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.resolve.caching

import org.gradle.integtests.resolve.AbstractDependencyResolutionTest

class CachedMissingModulesIntegrationTest extends AbstractDependencyResolutionTest {

    def "cached not-found information is ignored if module is not available in any repo"() {
        given:
        server.start()
        def repo1 = mavenHttpRepo("repo1")
        def repo1Module = repo1.module("group", "projectA", "1.0")
        def repo2 = mavenHttpRepo("repo2")
        def repo2Module = repo2.module("group", "projectA", "1.0")

        buildFile << """
    repositories {
        maven {
            name 'repo1'
            url '${repo1.uri}'
        }
        maven {
            name 'repo2'
            url '${repo2.uri}'
        }
    }
    configurations { compile }
    dependencies {
        compile 'group:projectA:1.0'
    }

    task retrieve(type: Sync) {
        into 'libs'
        from configurations.compile
    }
    """

        when:
        repo1Module.expectPomGetMissing()
        repo1Module.expectArtifactHeadMissing()
        repo2Module.expectPomGetMissing()
        repo2Module.expectArtifactHeadMissing()

        then:
        runAndFail 'retrieve'

        when:
        server.resetExpectations()
        repo2Module.publish()
        repo1Module.expectPomGetMissing()
        repo1Module.expectArtifactHeadMissing()
        repo2Module.expectPomGet()
        repo2Module.expectArtifactGet()

        then:
        run 'retrieve'

        when:
        server.resetExpectations()

        then:
        run 'retrieve'
    }
}
