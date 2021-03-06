/**
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.xtext.generator;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.mwe2.runtime.Mandatory;
import org.eclipse.xtend.lib.annotations.AccessorType;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.AbstractMetamodelDeclaration;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.ReferencedMetamodel;
import org.eclipse.xtext.XtextPackage;
import org.eclipse.xtext.ecore.EcoreSupportStandaloneSetup;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.containers.IAllContainersState;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsData;
import org.eclipse.xtext.util.internal.Log;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xtext.generator.CompositeGeneratorFragment2;
import org.eclipse.xtext.xtext.generator.IXtextProjectConfig;
import org.eclipse.xtext.xtext.generator.XtextGeneratorNaming;
import org.eclipse.xtext.xtext.generator.model.GuiceModuleAccess;
import org.eclipse.xtext.xtext.generator.model.ManifestAccess;
import org.eclipse.xtext.xtext.generator.model.StandaloneSetupAccess;
import org.eclipse.xtext.xtext.generator.model.TypeReference;
import org.eclipse.xtext.xtext.generator.xbase.XbaseGeneratorFragment2;

@Log
@SuppressWarnings("all")
public class LanguageConfig2 extends CompositeGeneratorFragment2 {
  @Accessors(AccessorType.PUBLIC_GETTER)
  private String uri;
  
  @Accessors(AccessorType.PUBLIC_GETTER)
  private Grammar grammar;
  
  @Accessors(AccessorType.PUBLIC_GETTER)
  private List<String> fileExtensions;
  
  @Accessors
  private ResourceSet resourceSet;
  
  @Accessors
  private XtextGeneratorNaming naming;
  
  @Accessors
  private final List<String> loadedResources = CollectionLiterals.<String>newArrayList();
  
  @Accessors
  private final StandaloneSetupAccess runtimeGenSetup = new StandaloneSetupAccess();
  
  @Accessors
  private final GuiceModuleAccess runtimeGenModule = new GuiceModuleAccess();
  
  @Accessors
  private final GuiceModuleAccess eclipsePluginGenModule = new GuiceModuleAccess();
  
  @Inject
  private Provider<ResourceSet> resourceSetProvider;
  
  @Inject
  private IXtextProjectConfig projectConfig;
  
  @Mandatory
  public void setUri(final String uri) {
    this.uri = uri;
  }
  
  public void setFileExtensions(final String fileExtensions) {
    String _trim = fileExtensions.trim();
    String[] _split = _trim.split("\\s*,\\s*");
    List<String> _list = IterableExtensions.<String>toList(((Iterable<String>)Conversions.doWrapArray(_split)));
    this.fileExtensions = _list;
  }
  
  public List<String> getFileExtensions() {
    boolean _or = false;
    if ((this.fileExtensions == null)) {
      _or = true;
    } else {
      boolean _isEmpty = this.fileExtensions.isEmpty();
      _or = _isEmpty;
    }
    if (_or) {
      String _name = GrammarUtil.getName(this.grammar);
      final String lowerCase = _name.toLowerCase();
      LanguageConfig2.LOG.info((("No explicit fileExtensions configured. Using \'*." + lowerCase) + "\'."));
      return Collections.<String>singletonList(lowerCase);
    }
    return this.fileExtensions;
  }
  
  public void addLoadedResource(final String uri) {
    this.loadedResources.add(uri);
  }
  
  @Override
  public void initialize(final Injector injector) {
    super.initialize(injector);
    if ((this.resourceSet == null)) {
      ResourceSet _get = this.resourceSetProvider.get();
      this.resourceSet = _get;
    }
    for (final String loadedResource : this.loadedResources) {
      {
        final URI loadedResourceUri = URI.createURI(loadedResource);
        String _fileExtension = loadedResourceUri.fileExtension();
        boolean _matched = false;
        if (!_matched) {
          if (Objects.equal(_fileExtension, "genmodel")) {
            _matched=true;
            final IResourceServiceProvider resourceServiceProvider = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(loadedResourceUri);
            if ((resourceServiceProvider == null)) {
              try {
                final Class<?> genModelSupport = Class.forName("org.eclipse.emf.codegen.ecore.xtext.GenModelSupport");
                final Object instance = genModelSupport.newInstance();
                Method _declaredMethod = genModelSupport.getDeclaredMethod("createInjectorAndDoEMFRegistration");
                _declaredMethod.invoke(instance);
              } catch (final Throwable _t) {
                if (_t instanceof ClassNotFoundException) {
                  final ClassNotFoundException e = (ClassNotFoundException)_t;
                  LanguageConfig2.LOG.error("Couldn\'t initialize GenModel support. Is it on the classpath?");
                  String _message = e.getMessage();
                  LanguageConfig2.LOG.debug(_message, e);
                } else if (_t instanceof Exception) {
                  final Exception e_1 = (Exception)_t;
                  LanguageConfig2.LOG.error("Couldn\'t initialize GenModel support.", e_1);
                } else {
                  throw Exceptions.sneakyThrow(_t);
                }
              }
            }
          }
        }
        if (!_matched) {
          if (Objects.equal(_fileExtension, "ecore")) {
            _matched=true;
            final IResourceServiceProvider resourceServiceProvider_1 = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(loadedResourceUri);
            if ((resourceServiceProvider_1 == null)) {
              EcoreSupportStandaloneSetup.setup();
            }
          }
        }
        if (!_matched) {
          if (Objects.equal(_fileExtension, "xcore")) {
            _matched=true;
            final IResourceServiceProvider resourceServiceProvider_2 = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(loadedResourceUri);
            if ((resourceServiceProvider_2 == null)) {
              try {
                final Class<?> xcore = Class.forName("org.eclipse.emf.ecore.xcore.XcoreStandaloneSetup");
                Method _declaredMethod_1 = xcore.getDeclaredMethod("doSetup", new Class[] {});
                _declaredMethod_1.invoke(null);
              } catch (final Throwable _t_1) {
                if (_t_1 instanceof ClassNotFoundException) {
                  final ClassNotFoundException e_2 = (ClassNotFoundException)_t_1;
                  LanguageConfig2.LOG.error("Couldn\'t initialize Xcore support. Is it on the classpath?");
                  String _message_1 = e_2.getMessage();
                  LanguageConfig2.LOG.debug(_message_1, e_2);
                } else if (_t_1 instanceof Exception) {
                  final Exception e_3 = (Exception)_t_1;
                  LanguageConfig2.LOG.error("Couldn\'t initialize Xcore support.", e_3);
                } else {
                  throw Exceptions.sneakyThrow(_t_1);
                }
              }
            }
            final URI xcoreLangURI = URI.createPlatformResourceURI("/org.eclipse.emf.ecore.xcore.lib/model/XcoreLang.xcore", true);
            try {
              this.resourceSet.getResource(xcoreLangURI, true);
            } catch (final Throwable _t_2) {
              if (_t_2 instanceof WrappedException) {
                final WrappedException e_4 = (WrappedException)_t_2;
                LanguageConfig2.LOG.error("Could not load XcoreLang.xcore.", e_4);
                final Resource brokenResource = this.resourceSet.getResource(xcoreLangURI, false);
                EList<Resource> _resources = this.resourceSet.getResources();
                _resources.remove(brokenResource);
              } else {
                throw Exceptions.sneakyThrow(_t_2);
              }
            }
          }
        }
        this.resourceSet.getResource(loadedResourceUri, true);
      }
    }
    EList<Resource> _resources = this.resourceSet.getResources();
    boolean _isEmpty = _resources.isEmpty();
    boolean _not = (!_isEmpty);
    if (_not) {
      this.installIndex();
      {
        int i = 0;
        EList<Resource> _resources_1 = this.resourceSet.getResources();
        int size = _resources_1.size();
        boolean _while = (i < size);
        while (_while) {
          {
            EList<Resource> _resources_2 = this.resourceSet.getResources();
            final Resource res = _resources_2.get(i);
            EList<EObject> _contents = res.getContents();
            boolean _isEmpty_1 = _contents.isEmpty();
            if (_isEmpty_1) {
              URI _uRI = res.getURI();
              String _plus = ("Error loading \'" + _uRI);
              String _plus_1 = (_plus + "\'");
              LanguageConfig2.LOG.error(_plus_1);
            } else {
              EList<Resource.Diagnostic> _errors = res.getErrors();
              boolean _isEmpty_2 = _errors.isEmpty();
              boolean _not_1 = (!_isEmpty_2);
              if (_not_1) {
                URI _uRI_1 = res.getURI();
                String _plus_2 = ("Error loading \'" + _uRI_1);
                String _plus_3 = (_plus_2 + "\':\n");
                Joiner _on = Joiner.on("\n");
                EList<Resource.Diagnostic> _errors_1 = res.getErrors();
                String _join = _on.join(_errors_1);
                String _plus_4 = (_plus_3 + _join);
                LanguageConfig2.LOG.error(_plus_4);
              }
            }
          }
          i++;
          _while = (i < size);
        }
      }
      EcoreUtil.resolveAll(this.resourceSet);
    }
    URI _createURI = URI.createURI(this.uri);
    Resource _resource = this.resourceSet.getResource(_createURI, true);
    final XtextResource resource = ((XtextResource) _resource);
    EList<EObject> _contents = resource.getContents();
    boolean _isEmpty_1 = _contents.isEmpty();
    if (_isEmpty_1) {
      throw new IllegalArgumentException((("Couldn\'t load grammar for \'" + this.uri) + "\'."));
    }
    EList<Resource.Diagnostic> _errors = resource.getErrors();
    boolean _isEmpty_2 = _errors.isEmpty();
    boolean _not_1 = (!_isEmpty_2);
    if (_not_1) {
      EList<Resource.Diagnostic> _errors_1 = resource.getErrors();
      LanguageConfig2.LOG.error(_errors_1);
      Joiner _on = Joiner.on("\n");
      EList<Resource.Diagnostic> _errors_2 = resource.getErrors();
      String _join = _on.join(_errors_2);
      String _plus = ((("Problem parsing \'" + this.uri) + "\':\n") + _join);
      throw new IllegalStateException(_plus);
    }
    EList<EObject> _contents_1 = resource.getContents();
    EObject _get_1 = _contents_1.get(0);
    final Grammar grammar = ((Grammar) _get_1);
    this.validateGrammar(grammar);
    this.grammar = grammar;
    if ((this.naming == null)) {
      XtextGeneratorNaming _xtextGeneratorNaming = new XtextGeneratorNaming();
      this.naming = _xtextGeneratorNaming;
    }
    this.naming.setGrammar(grammar);
  }
  
  private void installIndex() {
    List<IResourceDescription> _emptyList = Collections.<IResourceDescription>emptyList();
    final ResourceDescriptionsData index = new ResourceDescriptionsData(_emptyList);
    EList<Resource> _resources = this.resourceSet.getResources();
    final ArrayList<Resource> resources = Lists.<Resource>newArrayList(_resources);
    for (final Resource resource : resources) {
      URI _uRI = resource.getURI();
      this.index(resource, _uRI, index);
    }
    ResourceDescriptionsData.ResourceSetAdapter.installResourceDescriptionsData(this.resourceSet, index);
  }
  
  private void index(final Resource resource, final URI uri, final ResourceDescriptionsData index) {
    final IResourceServiceProvider serviceProvider = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(uri);
    boolean _notEquals = (!Objects.equal(serviceProvider, null));
    if (_notEquals) {
      IResourceDescription.Manager _resourceDescriptionManager = serviceProvider.getResourceDescriptionManager();
      final IResourceDescription resourceDescription = _resourceDescriptionManager.getResourceDescription(resource);
      boolean _notEquals_1 = (!Objects.equal(resourceDescription, null));
      if (_notEquals_1) {
        index.addDescription(uri, resourceDescription);
      }
    }
  }
  
  protected void validateGrammar(final Grammar grammar) {
    this.validateAllImports(grammar);
    final EValidator validator = EValidator.Registry.INSTANCE.getEValidator(XtextPackage.eINSTANCE);
    if ((validator != null)) {
      final DiagnosticChain chain = new DiagnosticChain() {
        @Override
        public void add(final Diagnostic diagnostic) {
          int _severity = diagnostic.getSeverity();
          boolean _equals = (_severity == Diagnostic.ERROR);
          if (_equals) {
            Throwable _exception = diagnostic.getException();
            boolean _equals_1 = Objects.equal(_exception, null);
            if (_equals_1) {
              String _message = diagnostic.getMessage();
              throw new IllegalStateException(_message);
            } else {
              String _message_1 = diagnostic.getMessage();
              Throwable _exception_1 = diagnostic.getException();
              throw new IllegalStateException(_message_1, _exception_1);
            }
          }
        }
        
        @Override
        public void addAll(final Diagnostic diagnostic) {
          this.add(diagnostic);
        }
        
        @Override
        public void merge(final Diagnostic diagnostic) {
          throw new UnsupportedOperationException();
        }
      };
      validator.validate(grammar, chain, null);
      final TreeIterator<EObject> iterator = grammar.eAllContents();
      while (iterator.hasNext()) {
        EObject _next = iterator.next();
        HashMap<Object, Object> _hashMap = new HashMap<Object, Object>();
        validator.validate(_next, chain, _hashMap);
      }
    }
  }
  
  protected void validateAllImports(final Grammar grammar) {
    List<AbstractMetamodelDeclaration> _allMetamodelDeclarations = GrammarUtil.allMetamodelDeclarations(grammar);
    for (final AbstractMetamodelDeclaration amd : _allMetamodelDeclarations) {
      if ((amd instanceof ReferencedMetamodel)) {
        this.validateReferencedMetamodel(((ReferencedMetamodel)amd));
      }
    }
  }
  
  protected void validateReferencedMetamodel(final ReferencedMetamodel ref) {
    boolean _and = false;
    EPackage _ePackage = ref.getEPackage();
    boolean _notEquals = (!Objects.equal(_ePackage, null));
    if (!_notEquals) {
      _and = false;
    } else {
      EPackage _ePackage_1 = ref.getEPackage();
      boolean _eIsProxy = _ePackage_1.eIsProxy();
      boolean _not = (!_eIsProxy);
      _and = _not;
    }
    if (_and) {
      return;
    }
    final EReference eref = XtextPackage.Literals.ABSTRACT_METAMODEL_DECLARATION__EPACKAGE;
    final List<INode> nodes = NodeModelUtils.findNodesForFeature(ref, eref);
    String _xifexpression = null;
    boolean _isEmpty = nodes.isEmpty();
    if (_isEmpty) {
      _xifexpression = "(unknown)";
    } else {
      INode _get = nodes.get(0);
      _xifexpression = NodeModelUtils.getTokenText(_get);
    }
    final String refName = _xifexpression;
    Grammar _grammar = GrammarUtil.getGrammar(ref);
    final String grammarName = _grammar.getName();
    final String msg = ((((("The EPackage " + refName) + " in grammar ") + grammarName) + " could not be found. ") + "You might want to register that EPackage in your workflow file.");
    throw new IllegalStateException(msg);
  }
  
  @Override
  public void generate(final LanguageConfig2 language) {
    this.addImplicitContributions(language);
    super.generate(language);
  }
  
  protected void addImplicitContributions(final LanguageConfig2 language) {
    ManifestAccess _runtimeManifest = this.projectConfig.getRuntimeManifest();
    boolean _tripleNotEquals = (_runtimeManifest != null);
    if (_tripleNotEquals) {
      ManifestAccess _runtimeManifest_1 = this.projectConfig.getRuntimeManifest();
      Set<String> _requiredBundles = _runtimeManifest_1.getRequiredBundles();
      _requiredBundles.addAll(
        Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("org.eclipse.xtext", "org.eclipse.xtext.util")));
      ManifestAccess _runtimeManifest_2 = this.projectConfig.getRuntimeManifest();
      Set<String> _importedPackages = _runtimeManifest_2.getImportedPackages();
      _importedPackages.add("org.apache.log4j");
    }
    ManifestAccess _eclipsePluginManifest = this.projectConfig.getEclipsePluginManifest();
    boolean _tripleNotEquals_1 = (_eclipsePluginManifest != null);
    if (_tripleNotEquals_1) {
      ManifestAccess _eclipsePluginManifest_1 = this.projectConfig.getEclipsePluginManifest();
      Set<String> _requiredBundles_1 = _eclipsePluginManifest_1.getRequiredBundles();
      _requiredBundles_1.addAll(
        Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("org.eclipse.xtext.ui", "org.eclipse.xtext.ui.shared", "org.eclipse.ui.editors", "org.eclipse.ui")));
    }
    StringConcatenationClient _client = new StringConcatenationClient() {
      @Override
      protected void appendTo(StringConcatenationClient.TargetStringConcatenation _builder) {
        TypeReference _typeRef = TypeReference.typeRef("org.eclipse.xtext.ui.shared.Access");
        _builder.append(_typeRef, "");
        _builder.append(".getJavaProjectsState()");
      }
    };
    final StringConcatenationClient expression = _client;
    GuiceModuleAccess.BindingFactory _bindingFactory = new GuiceModuleAccess.BindingFactory();
    TypeReference _typeRef = TypeReference.typeRef(IAllContainersState.class);
    final GuiceModuleAccess.BindingFactory bindingFactory = _bindingFactory.addTypeToProviderInstance(_typeRef, expression);
    boolean _inheritsXbase = XbaseGeneratorFragment2.inheritsXbase(language.grammar);
    if (_inheritsXbase) {
      TypeReference _typeRef_1 = TypeReference.typeRef("org.eclipse.xtext.ui.editor.XtextEditor");
      TypeReference _typeRef_2 = TypeReference.typeRef("org.eclipse.xtext.xbase.ui.editor.XbaseEditor");
      GuiceModuleAccess.BindingFactory _addTypeToType = bindingFactory.addTypeToType(_typeRef_1, _typeRef_2);
      TypeReference _typeRef_3 = TypeReference.typeRef("org.eclipse.xtext.ui.editor.model.XtextDocumentProvider");
      TypeReference _typeRef_4 = TypeReference.typeRef("org.eclipse.xtext.xbase.ui.editor.XbaseDocumentProvider");
      GuiceModuleAccess.BindingFactory _addTypeToType_1 = _addTypeToType.addTypeToType(_typeRef_3, _typeRef_4);
      TypeReference _typeRef_5 = TypeReference.typeRef("org.eclipse.xtext.ui.generator.trace.OpenGeneratedFileHandler");
      TypeReference _typeRef_6 = TypeReference.typeRef("org.eclipse.xtext.xbase.ui.generator.trace.XbaseOpenGeneratedFileHandler");
      _addTypeToType_1.addTypeToType(_typeRef_5, _typeRef_6);
    }
    bindingFactory.contributeTo(this.eclipsePluginGenModule);
  }
  
  private final static Logger LOG = Logger.getLogger(LanguageConfig2.class);
  
  @Pure
  public String getUri() {
    return this.uri;
  }
  
  @Pure
  public Grammar getGrammar() {
    return this.grammar;
  }
  
  @Pure
  public ResourceSet getResourceSet() {
    return this.resourceSet;
  }
  
  public void setResourceSet(final ResourceSet resourceSet) {
    this.resourceSet = resourceSet;
  }
  
  @Pure
  public XtextGeneratorNaming getNaming() {
    return this.naming;
  }
  
  public void setNaming(final XtextGeneratorNaming naming) {
    this.naming = naming;
  }
  
  @Pure
  public List<String> getLoadedResources() {
    return this.loadedResources;
  }
  
  @Pure
  public StandaloneSetupAccess getRuntimeGenSetup() {
    return this.runtimeGenSetup;
  }
  
  @Pure
  public GuiceModuleAccess getRuntimeGenModule() {
    return this.runtimeGenModule;
  }
  
  @Pure
  public GuiceModuleAccess getEclipsePluginGenModule() {
    return this.eclipsePluginGenModule;
  }
}
