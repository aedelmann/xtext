/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtext.generator.xbase

import com.google.inject.Inject
import com.google.inject.name.Names
import java.util.Set
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtend2.lib.StringConcatenationClient
import org.eclipse.xtext.AbstractRule
import org.eclipse.xtext.Grammar
import org.eclipse.xtext.GrammarUtil
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.resource.ILocationInFileProvider
import org.eclipse.xtext.scoping.IGlobalScopeProvider
import org.eclipse.xtext.scoping.IScopeProvider
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider
import org.eclipse.xtext.validation.IResourceValidator
import org.eclipse.xtext.xtext.UsedRulesFinder
import org.eclipse.xtext.xtext.generator.AbstractGeneratorFragment2
import org.eclipse.xtext.xtext.generator.IXtextProjectConfig
import org.eclipse.xtext.xtext.generator.LanguageConfig2
import org.eclipse.xtext.xtext.generator.model.FileAccessFactory
import org.eclipse.xtext.xtext.generator.model.FileSystemAccess
import org.eclipse.xtext.xtext.generator.model.GuiceModuleAccess
import org.eclipse.xtext.xtext.generator.model.TypeReference

import static extension org.eclipse.xtext.xtext.generator.model.TypeReference.*
import static extension org.eclipse.xtext.xtext.generator.util.GenModelUtil2.*
import static extension org.eclipse.xtext.xtext.generator.util.GrammarUtil2.*

@Accessors(PUBLIC_SETTER)
class XbaseGeneratorFragment2 extends AbstractGeneratorFragment2 {

	static def boolean inheritsXbase(Grammar grammar) {
		grammar.inherits('org.eclipse.xtext.xbase.Xbase')
	}

	static def boolean inheritsXbaseWithAnnotations(Grammar grammar) {
		grammar.inherits('org.eclipse.xtext.xbase.annotations.XbaseWithAnnotations')
	}
	
	static def boolean usesXImportSection(Grammar grammar) {
		val Set<AbstractRule> usedRules = newHashSet
		new UsedRulesFinder(usedRules).compute(grammar)
		return usedRules.exists[name == 'XImportSection' && GrammarUtil.getGrammar(it).name == 'org.eclipse.xtext.xbase.Xtype']
	}
	
	boolean generateXtendInferrer = true
	boolean useInferredJvmModel = true
	boolean jdtTypeHierarchy = true
	boolean jdtCallHierarchy = true
	boolean skipExportedPackage = false
	
	@Inject IXtextProjectConfig projectConfig
	
	@Inject FileAccessFactory fileAccessFactory
	
	@Inject extension FileSystemAccess.Extensions
	
	protected def TypeReference getJvmModelInferrer(LanguageConfig2 langConfig) {
		new TypeReference(langConfig.naming.runtimeBasePackage + '.jvmmodel.' + GrammarUtil.getName(langConfig.grammar) + 'JvmModelInferrer')
	}
	
	protected def TypeReference getImportScopeProvider(LanguageConfig2 langConfig) {
		if (langConfig.grammar.usesXImportSection)
			'org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider'.typeRef
		else
			'org.eclipse.xtext.xbase.scoping.XbaseImportedNamespaceScopeProvider'.typeRef 
	}
	
	override generate(LanguageConfig2 language) {
		if (!language.grammar.inheritsXbase)
			return;
		
		contributeRuntimeGuiceBindings(language)
		contributeEclipsePluginGuiceBindings(language)
		if (projectConfig.eclipsePluginPluginXml !== null)
			contributeEclipsePluginExtensions(language)
		if (generateXtendInferrer && !projectConfig.runtimeSrc.containsXtendFile(language.jvmModelInferrer))
			doGenerateXtendInferrer(language)
		
		if (projectConfig.runtimeManifest !== null) {
			projectConfig.runtimeManifest.requiredBundles.addAll(#[
				'org.eclipse.xtext.xbase', 'org.eclipse.xtext.xbase.lib'
			])
			if ((generateXtendInferrer || useInferredJvmModel) && !skipExportedPackage) {
				projectConfig.runtimeManifest.exportedPackages += language.jvmModelInferrer.packageName
			}
		}
		if (projectConfig.eclipsePluginManifest !== null) {
			projectConfig.eclipsePluginManifest.requiredBundles.addAll(#[
				'org.eclipse.xtext.xbase.ui', 'org.eclipse.jdt.debug.ui'
			])
		}
	}
	
	protected def contributeRuntimeGuiceBindings(LanguageConfig2 language) {
		val bindingFactory = new GuiceModuleAccess.BindingFactory()
				// overrides binding from org.eclipse.xtext.generator.exporting.QualifiedNamesFragment
				.addTypeToType(IQualifiedNameProvider.typeRef,
						'org.eclipse.xtext.xbase.scoping.XbaseQualifiedNameProvider'.typeRef)
		if (useInferredJvmModel) {
			bindingFactory
				.addTypeToType(ILocationInFileProvider.typeRef,
						'org.eclipse.xtext.xbase.jvmmodel.JvmLocationInFileProvider'.typeRef)
				.addTypeToType(IGlobalScopeProvider.typeRef,
						'org.eclipse.xtext.common.types.xtext.TypesAwareDefaultGlobalScopeProvider'.typeRef)
				.addTypeToType('org.eclipse.xtext.xbase.validation.FeatureNameValidator'.typeRef,
						'org.eclipse.xtext.xbase.validation.LogicalContainerAwareFeatureNameValidator'.typeRef)
				.addTypeToType('org.eclipse.xtext.xbase.typesystem.internal.DefaultBatchTypeResolver'.typeRef,
						'org.eclipse.xtext.xbase.typesystem.internal.LogicalContainerAwareBatchTypeResolver'.typeRef)
				.addTypeToType('org.eclipse.xtext.xbase.typesystem.internal.DefaultReentrantTypeResolver'.typeRef,
						'org.eclipse.xtext.xbase.typesystem.internal.LogicalContainerAwareReentrantTypeResolver'.typeRef)
				.addTypeToType(IResourceValidator.typeRef, 
						'org.eclipse.xtext.xbase.annotations.validation.DerivedStateAwareResourceValidator'.typeRef)
			if (generateXtendInferrer) {
				bindingFactory
					.addTypeToType('org.eclipse.xtext.xbase.jvmmodel.IJvmModelInferrer'.typeRef, language.jvmModelInferrer)
			}
		} else {
			bindingFactory
				.addTypeToType(ILocationInFileProvider.typeRef,
						'org.eclipse.xtext.xbase.resource.XbaseLocationInFileProvider'.typeRef)

		}
		if (language.grammar.usesXImportSection) {
			val StringConcatenationClient statement = '''
				binder.bind(«IScopeProvider».class).annotatedWith(«Names».named(«AbstractDeclarativeScopeProvider».NAMED_DELEGATE)).to(«language.importScopeProvider».class);
			'''
			bindingFactory
					.addConfiguredBinding(IScopeProvider.simpleName + 'Delegate', statement);
		}
		bindingFactory.contributeTo(language.runtimeGenModule)
		
		if (language.grammar.inheritsXbaseWithAnnotations)
			language.runtimeGenModule.superClass = 'org.eclipse.xtext.xbase.annotations.DefaultXbaseWithAnnotationsRuntimeModule'.typeRef
		else
			language.runtimeGenModule.superClass = 'org.eclipse.xtext.xbase.DefaultXbaseRuntimeModule'.typeRef
	}
	
	protected def contributeEclipsePluginGuiceBindings(LanguageConfig2 language) {
		val bindingFactory = new GuiceModuleAccess.BindingFactory()
		if (useInferredJvmModel) {
			val StringConcatenationClient statement = '''
				if («'org.eclipse.ui.PlatformUI'.typeRef».isWorkbenchRunning()) {
					binder.bind(«'org.eclipse.xtext.ui.editor.IURIEditorOpener'.typeRef».class).annotatedWith(«'org.eclipse.xtext.ui.LanguageSpecific'.typeRef».class).to(«'org.eclipse.xtext.xbase.ui.jvmmodel.navigation.DerivedMemberAwareEditorOpener'.typeRef».class);
					binder.bind(«'org.eclipse.xtext.common.types.ui.navigation.IDerivedMemberAwareEditorOpener'.typeRef».class).to(«'org.eclipse.xtext.xbase.ui.jvmmodel.navigation.DerivedMemberAwareEditorOpener'.typeRef».class);
				}
			'''
			// Rename refactoring
			bindingFactory
				.addTypeToType('org.eclipse.xtext.ui.editor.findrefs.FindReferencesHandler'.typeRef, 
						'org.eclipse.xtext.xbase.ui.jvmmodel.findrefs.JvmModelFindReferenceHandler'.typeRef)
				.addTypeToType('org.eclipse.xtext.ui.editor.findrefs.ReferenceQueryExecutor'.typeRef, 
						'org.eclipse.xtext.xbase.ui.jvmmodel.findrefs.JvmModelReferenceQueryExecutor'.typeRef)
						
				// overrides binding from org.eclipse.xtext.generator.exporting.QualifiedNamesFragment
				.addTypeToType('org.eclipse.xtext.ui.refactoring.IDependentElementsCalculator'.typeRef,
						'org.eclipse.xtext.xbase.ui.jvmmodel.refactoring.JvmModelDependentElementsCalculator'.typeRef)
				// overrides binding from RefactorElementNameFragment
				.addTypeToType('org.eclipse.xtext.ui.refactoring.IRenameRefactoringProvider'.typeRef, 
						'org.eclipse.xtext.xbase.ui.jvmmodel.refactoring.jdt.CombinedJvmJdtRenameRefactoringProvider'.typeRef)
				// overrides binding from RefactorElementNameFragment
				.addTypeToType('org.eclipse.xtext.ui.refactoring.IReferenceUpdater'.typeRef,
						'org.eclipse.xtext.xbase.ui.refactoring.XbaseReferenceUpdater'.typeRef)
				// overrides binding from RefactorElementNameFragment
				.addfinalTypeToType('org.eclipse.xtext.ui.refactoring.ui.IRenameContextFactory'.typeRef,
						'org.eclipse.xtext.xbase.ui.jvmmodel.refactoring.jdt.CombinedJvmJdtRenameContextFactory'.typeRef)
				// overrides binding from RefactorElementNameFragment
				.addTypeToType('org.eclipse.xtext.ui.refactoring.IRenameStrategy'.typeRef, 
						'org.eclipse.xtext.xbase.ui.jvmmodel.refactoring.DefaultJvmModelRenameStrategy'.typeRef)
				
				.addTypeToType('org.eclipse.xtext.common.types.ui.refactoring.participant.JdtRenameParticipant.ContextFactory'.typeRef,
						'org.eclipse.xtext.xbase.ui.jvmmodel.refactoring.JvmModelJdtRenameParticipantContext.ContextFactory'.typeRef)
				.addTypeToType('org.eclipse.xtext.ui.editor.outline.impl.OutlineNodeElementOpener'.typeRef, 
						'org.eclipse.xtext.xbase.ui.jvmmodel.outline.JvmOutlineNodeElementOpener'.typeRef)
				.addTypeToType('org.eclipse.xtext.ui.editor.GlobalURIEditorOpener'.typeRef, 
						'org.eclipse.xtext.common.types.ui.navigation.GlobalDerivedMemberAwareURIEditorOpener'.typeRef)
				.addTypeToType('org.eclipse.xtext.ui.editor.occurrences.IOccurrenceComputer'.typeRef, 
						'org.eclipse.xtext.xbase.ui.jvmmodel.occurrence.JvmModelOccurrenceComputer'.typeRef)
				.addTypeToType('org.eclipse.xtext.common.types.ui.query.IJavaSearchParticipation'.typeRef, 
						'org.eclipse.xtext.common.types.ui.query.IJavaSearchParticipation.No'.typeRef)
				// DerivedMemberAwareEditorOpener
				.addConfiguredBinding('LanguageSpecificURIEditorOpener', statement)
		} else {
			bindingFactory
				.addTypeToType('org.eclipse.xtext.ui.refactoring.IRenameStrategy'.typeRef, 
						'org.eclipse.xtext.xbase.ui.refactoring.XbaseRenameStrategy'.typeRef)
		}
		if (language.grammar.usesXImportSection) {
			bindingFactory
				.addTypeToType('org.eclipse.xtext.xbase.imports.IUnresolvedTypeResolver'.typeRef,
						'org.eclipse.xtext.xbase.ui.imports.InteractiveUnresolvedTypeResolver'.typeRef)
				.addTypeToType('org.eclipse.xtext.common.types.xtext.ui.ITypesProposalProvider'.typeRef,
						'org.eclipse.xtext.xbase.ui.contentassist.ImportingTypesProposalProvider'.typeRef)
				.addTypeToType('org.eclipse.xtext.ui.editor.templates.XtextTemplateContextType'.typeRef,
						'org.eclipse.xtext.xbase.ui.templates.XbaseTemplateContextType'.typeRef)
		} else {
			bindingFactory
				.addTypeToType('org.eclipse.xtext.xbase.ui.quickfix.JavaTypeQuickfixes'.typeRef,
						'org.eclipse.xtext.xbase.ui.quickfix.JavaTypeQuickfixesNoImportSection'.typeRef)
		}
		bindingFactory.contributeTo(language.eclipsePluginGenModule)
		
		if (language.grammar.inheritsXbaseWithAnnotations)
			language.eclipsePluginGenModule.superClass = 'org.eclipse.xtext.xbase.annotations.ui.DefaultXbaseWithAnnotationsUiModule'.typeRef
		else
			language.eclipsePluginGenModule.superClass = 'org.eclipse.xtext.xbase.ui.DefaultXbaseUiModule'.typeRef
	}
	
	protected def doGenerateXtendInferrer(LanguageConfig2 language) {
		val xtendFile = fileAccessFactory.createXtendFile(language.jvmModelInferrer)
		
		xtendFile.typeComment = '''
			/**
			 * <p>Infers a JVM model from the source model.</p> 
			 *
			 * <p>The JVM model should contain all elements that would appear in the Java code 
			 * which is generated from the source model. Other models link against the JVM model rather than the source model.</p>     
			 */
		'''
		val firstRuleType = language.grammar.rules.head.type.classifier.getJavaTypeName(language.grammar.eResource.resourceSet).typeRef
		xtendFile.javaContent = '''
			class «language.jvmModelInferrer.simpleName» extends «'org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer'.typeRef» {
			
			    /**
			     * convenience API to build and initialize JVM types and their members.
			     */
				@«Inject» extension «'org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder'.typeRef»
			
				/**
				 * The dispatch method {@code infer} is called for each instance of the
				 * given element's type that is contained in a resource.
				 * 
				 * @param element
				 *            the model to create one or more
				 *            {@link org.eclipse.xtext.common.types.JvmDeclaredType declared
				 *            types} from.
				 * @param acceptor
				 *            each created
				 *            {@link org.eclipse.xtext.common.types.JvmDeclaredType type}
				 *            without a container should be passed to the acceptor in order
				 *            get attached to the current resource. The acceptor's
				 *            {@link IJvmDeclaredTypeAcceptor#accept(org.eclipse.xtext.common.types.JvmDeclaredType)
				 *            accept(..)} method takes the constructed empty type for the
				 *            pre-indexing phase. This one is further initialized in the
				 *            indexing phase using the closure you pass to the returned
				 *            {@link org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor.IPostIndexingInitializing#initializeLater(org.eclipse.xtext.xbase.lib.Procedures.Procedure1)
				 *            initializeLater(..)}.
				 * @param isPreIndexingPhase
				 *            whether the method is called in a pre-indexing phase, i.e.
				 *            when the global index is not yet fully updated. You must not
				 *            rely on linking using the index if isPreIndexingPhase is
				 *            <code>true</code>.
				 */
				def dispatch void infer(«firstRuleType» element, «'org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor'.typeRef» acceptor, boolean isPreIndexingPhase) {
					// Here you explain how your model is mapped to Java elements, by writing the actual translation code.
					
					// An implementation for the initial hello world example could look like this:
			//   		acceptor.accept(element.toClass("my.company.greeting.MyGreetings")) [
			//   			for (greeting : element.greetings) {
			//   				members += greeting.toMethod("hello" + greeting.name, typeRef(String)) [
			//   					body = «"'''"»
			//							return "Hello «'«'»greeting.name«'»'»";
			//   					«"'''"»
			//   				]
			//   			}
			//   		]
				}
			}
		'''
		xtendFile.writeTo(projectConfig.runtimeSrc)
	}
	
	protected def contributeEclipsePluginExtensions(LanguageConfig2 language) {
		val name = language.grammar.name
		if (jdtTypeHierarchy) {
			projectConfig.eclipsePluginPluginXml.entries += '''
				<!-- Type Hierarchy  -->
				<extension point="org.eclipse.ui.handlers">
					<handler 
						class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.xtext.xbase.ui.hierarchy.OpenTypeHierarchyHandler"
						commandId="org.eclipse.xtext.xbase.ui.hierarchy.OpenTypeHierarchy">
						<activeWhen>
							<reference
								definitionId="«name».Editor.opened">
							</reference>
						</activeWhen>
					</handler>
					<handler 
						class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.xtext.xbase.ui.hierarchy.QuickTypeHierarchyHandler"
						commandId="org.eclipse.jdt.ui.edit.text.java.open.hierarchy">
						<activeWhen>
							<reference
								definitionId="«name».Editor.opened">
							</reference>
						</activeWhen>
					</handler>
					«IF language.grammar.usesXImportSection»
						<handler
							class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.xtext.xbase.ui.imports.OrganizeImportsHandler"
							commandId="org.eclipse.xtext.xbase.ui.organizeImports">
							<activeWhen>
								<reference
									definitionId="«name».Editor.opened">
								</reference>
							</activeWhen>
						</handler>
					«ENDIF»
				</extension>
				<extension point="org.eclipse.ui.menus">
					«IF language.grammar.usesXImportSection»
						<menuContribution
							locationURI="popup:#TextEditorContext?after=group.edit">
							 <command
								 commandId="org.eclipse.xtext.xbase.ui.organizeImports"
								 style="push"
								 tooltip="Organize Imports">
								<visibleWhen checkEnabled="false">
									<reference
										definitionId="«name».Editor.opened">
									</reference>
								</visibleWhen>
							</command>  
						</menuContribution>
					«ENDIF»
					<menuContribution
						locationURI="popup:#TextEditorContext?after=group.open">
						<command commandId="org.eclipse.xtext.xbase.ui.hierarchy.OpenTypeHierarchy"
							style="push"
							tooltip="Open Type Hierarchy">
							<visibleWhen checkEnabled="false">
								<reference definitionId="«name».Editor.opened"/>
							</visibleWhen>
						</command>
					</menuContribution>
					<menuContribution
						locationURI="popup:#TextEditorContext?after=group.open">
						<command commandId="org.eclipse.jdt.ui.edit.text.java.open.hierarchy"
							style="push"
							tooltip="Quick Type Hierarchy">
							<visibleWhen checkEnabled="false">
								<reference definitionId="«name».Editor.opened"/>
							</visibleWhen>
						</command>
					</menuContribution>
				</extension>
			'''
		}
		if (jdtCallHierarchy) {
			projectConfig.eclipsePluginPluginXml.entries += '''
				<!-- Call Hierachy -->
				<extension point="org.eclipse.ui.handlers">
					<handler 
						class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.xtext.xbase.ui.hierarchy.OpenCallHierachyHandler"
						commandId="org.eclipse.xtext.xbase.ui.hierarchy.OpenCallHierarchy">
						<activeWhen>
							<reference
								definitionId="«name».Editor.opened">
							</reference>
						</activeWhen>
					</handler>
				</extension>
				<extension point="org.eclipse.ui.menus">
					<menuContribution
						locationURI="popup:#TextEditorContext?after=group.open">
						<command commandId="org.eclipse.xtext.xbase.ui.hierarchy.OpenCallHierarchy"
							style="push"
							tooltip="Open Call Hierarchy">
							<visibleWhen checkEnabled="false">
								<reference definitionId="«name».Editor.opened"/>
							</visibleWhen>
						</command>
					</menuContribution>
				</extension>
			'''
		}
		projectConfig.eclipsePluginPluginXml.entries += '''
			<extension point="org.eclipse.core.runtime.adapters">
				<factory class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.xtext.builder.smap.StratumBreakpointAdapterFactory"
					adaptableType="org.eclipse.xtext.ui.editor.XtextEditor">
					<adapter type="org.eclipse.debug.ui.actions.IToggleBreakpointsTarget"/>
				</factory> 
			</extension>
			<extension point="org.eclipse.ui.editorActions">
				<editorContribution targetID="«name»" 
					id="«name».rulerActions">
					<action
						label="Not Used"
			 			class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.debug.ui.actions.RulerToggleBreakpointActionDelegate"
						style="push"
						actionID="RulerDoubleClick"
						id="«name».doubleClickBreakpointAction"/>
				</editorContribution>
			</extension>
			<extension point="org.eclipse.ui.popupMenus">
				<viewerContribution
					targetID="«name».RulerContext"
					id="«name».RulerPopupActions">
					<action
						label="Toggle Breakpoint"
						class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.debug.ui.actions.RulerToggleBreakpointActionDelegate"
						menubarPath="debug"
						id="«name».rulerContextMenu.toggleBreakpointAction">
					</action>
					<action
						label="Not used"
						class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.debug.ui.actions.RulerEnableDisableBreakpointActionDelegate"
						menubarPath="debug"
						id="«name».rulerContextMenu.enableDisableBreakpointAction">
					</action>
					<action
						label="Breakpoint Properties"
						helpContextId="breakpoint_properties_action_context"
						class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.jdt.debug.ui.actions.JavaBreakpointPropertiesRulerActionDelegate"
						menubarPath="group.properties"
						id="«name».rulerContextMenu.openBreapointPropertiesAction">
					</action>
				</viewerContribution>
			</extension>
			<!-- Introduce Local Variable Refactoring -->
			<extension point="org.eclipse.ui.handlers">
				<handler 
					class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.xtext.xbase.ui.refactoring.ExtractVariableHandler"
					commandId="org.eclipse.xtext.xbase.ui.refactoring.ExtractLocalVariable">
					<activeWhen>
						<reference
							definitionId="«name».Editor.opened">
						</reference>
					</activeWhen>
				</handler>
			</extension>
			<extension point="org.eclipse.ui.menus">
				<menuContribution
					locationURI="popup:#TextEditorContext?after=group.edit">
					<command commandId="org.eclipse.xtext.xbase.ui.refactoring.ExtractLocalVariable"
						style="push">
						<visibleWhen checkEnabled="false">
							<reference
								definitionId="«name».Editor.opened">
							</reference>
						</visibleWhen>
					</command>
				</menuContribution>
			</extension>  
			<!-- Open implementation -->
			<extension point="org.eclipse.ui.handlers">
				<handler
					class="«language.naming.eclipsePluginExecutableExtensionFactory»:org.eclipse.xtext.xbase.ui.navigation.OpenImplementationHandler"
					commandId="org.eclipse.xtext.xbase.ui.OpenImplementationCommand">
					<activeWhen>
						<reference
							definitionId="«name».Editor.opened">
						</reference>
					</activeWhen>
				</handler>
			</extension>
			<extension point="org.eclipse.ui.menus">
				<menuContribution
					locationURI="menu:navigate?after=open.ext4">
					<command commandId="org.eclipse.xtext.xbase.ui.OpenImplementationCommand">
						<visibleWhen checkEnabled="false">
							<reference
								definitionId="«name».Editor.opened">
							</reference>
						</visibleWhen>
					</command>
				</menuContribution>
			</extension>
		'''
	}
	
}
