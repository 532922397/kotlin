/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.builtins.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolverImpl
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService

class JvmPlatformParameters(
        val trace: BindingTrace?,
        val languageVersionSettings: LanguageVersionSettings,
        val lookupTracker: LookupTracker,
        val useBuiltInsProvider: Boolean, // TODO: load built-ins from module dependencies in IDE
        val useLazyResolve: Boolean,
        val moduleByJavaClass: (JavaClass) -> ModuleInfo?,
        val moduleParameters: (ModuleInfo) -> JvmModuleParameters
) : PlatformAnalysisParameters {
    constructor(moduleByJavaClass: (JavaClass) -> ModuleInfo?) : this(
            null, LanguageVersionSettingsImpl.DEFAULT /* TODO: see KT-12410 */, LookupTracker.DO_NOTHING, false, true, moduleByJavaClass,
            { _ -> JvmModuleParameters(null, { emptyList() }) }
    )
}

class JvmModuleParameters(
        val customDeclarationProviderFactory: DeclarationProviderFactory?,
        val getAdditionalProviders: (ComponentProvider) -> List<PackageFragmentProvider>
)


object JvmAnalyzerFacade : AnalyzerFacade<JvmPlatformParameters>() {
    override fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContext: ModuleContext,
            moduleContent: ModuleContent,
            platformParameters: JvmPlatformParameters,
            targetEnvironment: TargetEnvironment,
            resolverForProject: ResolverForProject<M>,
            packagePartProvider: PackagePartProvider
    ): ResolverForModule {
        val (syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val moduleParameters = platformParameters.moduleParameters(moduleInfo)

        val declarationProviderFactory =
                moduleParameters.customDeclarationProviderFactory
                ?: DeclarationProviderFactoryService.createDeclarationProviderFactory(
                        project, moduleContext.storageManager, syntheticFiles,
                        if (moduleInfo.isLibrary) GlobalSearchScope.EMPTY_SCOPE else moduleContentScope
                )

        val moduleClassResolver = ModuleClassResolverImpl { javaClass ->
            val referencedClassModule = platformParameters.moduleByJavaClass(javaClass)
            // We don't have full control over idea resolve api so we allow for a situation which should not happen in Kotlin.
            // For example, type in a java library can reference a class declared in a source root (is valid but rare case)
            // Providing a fallback strategy in this case can hide future problems, so we should at least log to be able to diagnose those

            @Suppress("UNCHECKED_CAST")
            val resolverForReferencedModule = referencedClassModule?.let { resolverForProject.tryGetResolverForModule(it as M) }

            val resolverForModule = resolverForReferencedModule ?: run {
                LOG.warn("Java referenced $referencedClassModule from $moduleInfo\nReferenced class was: $javaClass\n")
                // in case referenced class lies outside of our resolver, resolve the class as if it is inside our module
                // this leads to java class being resolved several times
                resolverForProject.resolverForModule(moduleInfo)
            }
            resolverForModule.componentProvider.get<JavaDescriptorResolver>()
        }

        val trace = platformParameters.trace ?: CodeAnalyzerInitializer.getInstance(project).createTrace()
        val container = createContainerForLazyResolveWithJava(
                moduleContext,
                trace,
                declarationProviderFactory,
                moduleContentScope,
                moduleClassResolver,
                targetEnvironment,
                platformParameters.lookupTracker,
                packagePartProvider,
                platformParameters.languageVersionSettings,
                platformParameters.useBuiltInsProvider,
                platformParameters.useLazyResolve
        )

        val providersForModule = arrayListOf(container.get<ResolveSession>().packageFragmentProvider)
        providersForModule += moduleParameters.getAdditionalProviders(container)
        providersForModule += container.get<JavaDescriptorResolver>().packageFragmentProvider

        providersForModule += PackageFragmentProviderExtension.getInstances(project)
                .mapNotNull { it.getPackageFragmentProvider(project, moduleDescriptor, moduleContext.storageManager, trace, moduleInfo) }

        return ResolverForModule(CompositePackageFragmentProvider(providersForModule), container)
    }

    override val targetPlatform: TargetPlatform
        get() = JvmPlatform

    private val LOG = Logger.getInstance(JvmAnalyzerFacade::class.java)
}
