/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.allopen.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class SpringGradleSubplugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(AllOpenGradleSubplugin::class.java)
        val allOpenExtension = AllOpenGradleSubplugin.getAllOpenExtension(project)
        allOpenExtension.annotation("org.springframework.stereotype.Component",
                                    "org.springframework.transaction.annotation.Transactional",
                                    "org.springframework.scheduling.annotation.Async",
                                    "org.springframework.cache.annotation.Cacheable")
    }
}