/**
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.xtext.generator;

import com.google.common.base.Objects;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.mwe.core.WorkflowContext;
import org.eclipse.emf.mwe.core.issues.Issues;
import org.eclipse.emf.mwe.core.lib.AbstractWorkflowComponent2;
import org.eclipse.emf.mwe.core.monitor.ProgressMonitor;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.AbstractMetamodelDeclaration;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.GeneratedMetamodel;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.generator.IFileSystemAccess2;
import org.eclipse.xtext.util.MergeableManifest;
import org.eclipse.xtext.util.internal.Log;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xtext.generator.CodeConfig;
import org.eclipse.xtext.xtext.generator.DefaultGeneratorModule;
import org.eclipse.xtext.xtext.generator.IGuiceAwareGeneratorComponent;
import org.eclipse.xtext.xtext.generator.IXtextProjectConfig;
import org.eclipse.xtext.xtext.generator.LanguageConfig2;
import org.eclipse.xtext.xtext.generator.XtextGeneratorNaming;
import org.eclipse.xtext.xtext.generator.XtextGeneratorTemplates;
import org.eclipse.xtext.xtext.generator.model.FileSystemAccess;
import org.eclipse.xtext.xtext.generator.model.JavaFileAccess;
import org.eclipse.xtext.xtext.generator.model.ManifestAccess;
import org.eclipse.xtext.xtext.generator.model.PluginXmlAccess;
import org.eclipse.xtext.xtext.generator.model.TextFileAccess;
import org.eclipse.xtext.xtext.generator.model.TypeReference;

/**
 * The Xtext language infrastructure generator. Can be configured with {@link IGeneratorFragment}
 * instances as well as with some properties declared via setter or adder methods.
 * 
 * <p><b>NOTE: This is a reimplementation of org.eclipse.xtext.generator.Generator</b></p>
 */
@Log
@SuppressWarnings("all")
public class XtextGenerator extends AbstractWorkflowComponent2 implements IGuiceAwareGeneratorComponent {
  @Accessors
  private DefaultGeneratorModule configuration;
  
  @Accessors
  private final List<LanguageConfig2> languageConfigs = CollectionLiterals.<LanguageConfig2>newArrayList();
  
  private Injector injector;
  
  @Inject
  private IXtextProjectConfig projectConfig;
  
  @Inject
  private XtextGeneratorTemplates templates;
  
  @Inject
  @Extension
  private FileSystemAccess.Extensions _extensions;
  
  public XtextGenerator() {
    XtextStandaloneSetup _xtextStandaloneSetup = new XtextStandaloneSetup();
    _xtextStandaloneSetup.createInjectorAndDoEMFRegistration();
  }
  
  /**
   * Add a language configuration to be included in the code generation process.
   */
  public void addLanguage(final LanguageConfig2 language) {
    this.languageConfigs.add(language);
  }
  
  @Override
  protected void checkConfigurationInternal(final Issues issues) {
    this.createInjector();
    if ((this.configuration != null)) {
      this.configuration.checkConfiguration(this, issues);
    }
    final HashMap<String, Grammar> uris = new HashMap<String, Grammar>();
    for (final LanguageConfig2 language : this.languageConfigs) {
      {
        language.checkConfiguration(this, issues);
        Grammar _grammar = language.getGrammar();
        EList<AbstractMetamodelDeclaration> _metamodelDeclarations = _grammar.getMetamodelDeclarations();
        List<GeneratedMetamodel> _typeSelect = EcoreUtil2.<GeneratedMetamodel>typeSelect(_metamodelDeclarations, GeneratedMetamodel.class);
        for (final GeneratedMetamodel generatedMetamodel : _typeSelect) {
          {
            EPackage _ePackage = generatedMetamodel.getEPackage();
            final String nsURI = _ePackage.getNsURI();
            boolean _containsKey = uris.containsKey(nsURI);
            if (_containsKey) {
              Grammar _get = uris.get(nsURI);
              String _name = _get.getName();
              String _plus = ((("Duplicate generated grammar with nsURI \'" + nsURI) + "\' in ") + _name);
              String _plus_1 = (_plus + " and ");
              Grammar _grammar_1 = language.getGrammar();
              String _name_1 = _grammar_1.getName();
              String _plus_2 = (_plus_1 + _name_1);
              issues.addError(this, _plus_2);
            } else {
              Grammar _grammar_2 = language.getGrammar();
              uris.put(nsURI, _grammar_2);
            }
          }
        }
      }
    }
  }
  
  protected Injector createInjector() {
    if ((this.injector == null)) {
      XtextGenerator.LOG.info("Initializing Xtext generator");
      if ((this.configuration == null)) {
        DefaultGeneratorModule _defaultGeneratorModule = new DefaultGeneratorModule();
        this.configuration = _defaultGeneratorModule;
      }
      Injector _createInjector = Guice.createInjector(this.configuration);
      this.injector = _createInjector;
      this.initialize(this.injector);
    }
    return this.injector;
  }
  
  @Override
  public void initialize(final Injector injector) {
    injector.injectMembers(this);
    this.projectConfig.initialize(injector);
    for (final LanguageConfig2 language : this.languageConfigs) {
      language.initialize(injector);
    }
    CodeConfig _instance = injector.<CodeConfig>getInstance(CodeConfig.class);
    final Procedure1<CodeConfig> _function = new Procedure1<CodeConfig>() {
      @Override
      public void apply(final CodeConfig codeConfig) {
        codeConfig.initialize(injector);
      }
    };
    ObjectExtensions.<CodeConfig>operator_doubleArrow(_instance, _function);
  }
  
  @Override
  protected void invokeInternal(final WorkflowContext ctx, final ProgressMonitor monitor, final Issues issues) {
    this.createInjector();
    for (final LanguageConfig2 language : this.languageConfigs) {
      {
        Grammar _grammar = language.getGrammar();
        String _name = _grammar.getName();
        String _plus = ("Generating " + _name);
        XtextGenerator.LOG.info(_plus);
        language.generate(language);
        this.generateRuntimeSetup(language);
        this.generateModules(language);
        this.generateExecutableExtensionFactory(language);
      }
    }
    XtextGenerator.LOG.info("Generating common infrastructure");
    this.generatePluginXmls();
    this.generateManifests();
    this.generateActivator();
  }
  
  protected void generateRuntimeSetup(final LanguageConfig2 language) {
    JavaFileAccess _createRuntimeGenSetup = this.templates.createRuntimeGenSetup(language);
    IFileSystemAccess2 _runtimeSrcGen = this.projectConfig.getRuntimeSrcGen();
    _createRuntimeGenSetup.writeTo(_runtimeSrcGen);
    IFileSystemAccess2 _runtimeSrc = this.projectConfig.getRuntimeSrc();
    XtextGeneratorNaming _naming = language.getNaming();
    TypeReference _runtimeSetup = _naming.getRuntimeSetup();
    boolean _containsJavaFile = this._extensions.containsJavaFile(_runtimeSrc, _runtimeSetup);
    boolean _not = (!_containsJavaFile);
    if (_not) {
      JavaFileAccess _createRuntimeSetup = this.templates.createRuntimeSetup(language);
      IFileSystemAccess2 _runtimeSrc_1 = this.projectConfig.getRuntimeSrc();
      _createRuntimeSetup.writeTo(_runtimeSrc_1);
    }
  }
  
  protected void generateModules(final LanguageConfig2 language) {
    JavaFileAccess _createRuntimeGenModule = this.templates.createRuntimeGenModule(language);
    IFileSystemAccess2 _runtimeSrcGen = this.projectConfig.getRuntimeSrcGen();
    _createRuntimeGenModule.writeTo(_runtimeSrcGen);
    IFileSystemAccess2 _runtimeSrc = this.projectConfig.getRuntimeSrc();
    XtextGeneratorNaming _naming = language.getNaming();
    TypeReference _runtimeModule = _naming.getRuntimeModule();
    boolean _containsJavaFile = this._extensions.containsJavaFile(_runtimeSrc, _runtimeModule);
    boolean _not = (!_containsJavaFile);
    if (_not) {
      JavaFileAccess _createRuntimeModule = this.templates.createRuntimeModule(language);
      IFileSystemAccess2 _runtimeSrc_1 = this.projectConfig.getRuntimeSrc();
      _createRuntimeModule.writeTo(_runtimeSrc_1);
    }
    IFileSystemAccess2 _eclipsePluginSrcGen = this.projectConfig.getEclipsePluginSrcGen();
    boolean _tripleNotEquals = (_eclipsePluginSrcGen != null);
    if (_tripleNotEquals) {
      JavaFileAccess _createEclipsePluginGenModule = this.templates.createEclipsePluginGenModule(language);
      IFileSystemAccess2 _eclipsePluginSrcGen_1 = this.projectConfig.getEclipsePluginSrcGen();
      _createEclipsePluginGenModule.writeTo(_eclipsePluginSrcGen_1);
    }
    boolean _and = false;
    IFileSystemAccess2 _eclipsePluginSrc = this.projectConfig.getEclipsePluginSrc();
    boolean _tripleNotEquals_1 = (_eclipsePluginSrc != null);
    if (!_tripleNotEquals_1) {
      _and = false;
    } else {
      IFileSystemAccess2 _eclipsePluginSrc_1 = this.projectConfig.getEclipsePluginSrc();
      XtextGeneratorNaming _naming_1 = language.getNaming();
      TypeReference _eclipsePluginModule = _naming_1.getEclipsePluginModule();
      boolean _containsJavaFile_1 = this._extensions.containsJavaFile(_eclipsePluginSrc_1, _eclipsePluginModule);
      boolean _not_1 = (!_containsJavaFile_1);
      _and = _not_1;
    }
    if (_and) {
      JavaFileAccess _createEclipsePluginModule = this.templates.createEclipsePluginModule(language);
      IFileSystemAccess2 _eclipsePluginSrc_2 = this.projectConfig.getEclipsePluginSrc();
      _createEclipsePluginModule.writeTo(_eclipsePluginSrc_2);
    }
  }
  
  protected void generateExecutableExtensionFactory(final LanguageConfig2 language) {
    IFileSystemAccess2 _eclipsePluginSrcGen = this.projectConfig.getEclipsePluginSrcGen();
    boolean _tripleNotEquals = (_eclipsePluginSrcGen != null);
    if (_tripleNotEquals) {
      LanguageConfig2 _head = IterableExtensions.<LanguageConfig2>head(this.languageConfigs);
      JavaFileAccess _createEclipsePluginExecutableExtensionFactory = this.templates.createEclipsePluginExecutableExtensionFactory(language, _head);
      IFileSystemAccess2 _eclipsePluginSrcGen_1 = this.projectConfig.getEclipsePluginSrcGen();
      _createEclipsePluginExecutableExtensionFactory.writeTo(_eclipsePluginSrcGen_1);
    }
  }
  
  protected void generateManifests() {
    ManifestAccess _runtimeManifest = this.projectConfig.getRuntimeManifest();
    ManifestAccess _runtimeTestManifest = this.projectConfig.getRuntimeTestManifest();
    ManifestAccess _genericIdeManifest = this.projectConfig.getGenericIdeManifest();
    ManifestAccess _genericIdeTestManifest = this.projectConfig.getGenericIdeTestManifest();
    ManifestAccess _eclipsePluginManifest = this.projectConfig.getEclipsePluginManifest();
    ManifestAccess _eclipsePluginTestManifest = this.projectConfig.getEclipsePluginTestManifest();
    ManifestAccess _ideaPluginManifest = this.projectConfig.getIdeaPluginManifest();
    ManifestAccess _ideaPluginTestManifest = this.projectConfig.getIdeaPluginTestManifest();
    ManifestAccess _webManifest = this.projectConfig.getWebManifest();
    ManifestAccess _webTestManifest = this.projectConfig.getWebTestManifest();
    final Set<ManifestAccess> manifests = Collections.<ManifestAccess>unmodifiableSet(CollectionLiterals.<ManifestAccess>newHashSet(_runtimeManifest, _runtimeTestManifest, _genericIdeManifest, _genericIdeTestManifest, _eclipsePluginManifest, _eclipsePluginTestManifest, _ideaPluginManifest, _ideaPluginTestManifest, _webManifest, _webTestManifest));
    Iterable<ManifestAccess> _filterNull = IterableExtensions.<ManifestAccess>filterNull(manifests);
    final Function1<ManifestAccess, String> _function = new Function1<ManifestAccess, String>() {
      @Override
      public String apply(final ManifestAccess it) {
        return it.getPath();
      }
    };
    List<ManifestAccess> _sortBy = IterableExtensions.<ManifestAccess, String>sortBy(_filterNull, _function);
    final Procedure1<ManifestAccess> _function_1 = new Procedure1<ManifestAccess>() {
      @Override
      public void apply(final ManifestAccess manifest) {
        try {
          String _bundleName = manifest.getBundleName();
          boolean _tripleEquals = (_bundleName == null);
          if (_tripleEquals) {
            String _path = manifest.getPath();
            final String[] segments = _path.split("/");
            boolean _and = false;
            int _length = segments.length;
            boolean _greaterEqualsThan = (_length >= 3);
            if (!_greaterEqualsThan) {
              _and = false;
            } else {
              int _length_1 = segments.length;
              int _minus = (_length_1 - 2);
              String _get = segments[_minus];
              boolean _equals = Objects.equal(_get, "META-INF");
              _and = _equals;
            }
            if (_and) {
              int _length_2 = segments.length;
              int _minus_1 = (_length_2 - 3);
              String _get_1 = segments[_minus_1];
              manifest.setBundleName(_get_1);
            }
          }
          TypeReference activator = null;
          ManifestAccess _eclipsePluginManifest = XtextGenerator.this.projectConfig.getEclipsePluginManifest();
          boolean _tripleEquals_1 = (manifest == _eclipsePluginManifest);
          if (_tripleEquals_1) {
            LanguageConfig2 _head = IterableExtensions.<LanguageConfig2>head(XtextGenerator.this.languageConfigs);
            XtextGeneratorNaming _naming = null;
            if (_head!=null) {
              _naming=_head.getNaming();
            }
            TypeReference _eclipsePluginActivator = null;
            if (_naming!=null) {
              _eclipsePluginActivator=_naming.getEclipsePluginActivator();
            }
            activator = _eclipsePluginActivator;
          }
          String _path_1 = manifest.getPath();
          final File file = new File(_path_1);
          boolean _exists = file.exists();
          if (_exists) {
            boolean _isMerge = manifest.isMerge();
            if (_isMerge) {
              XtextGenerator.this.mergeManifest(manifest, file, activator);
            } else {
              String _path_2 = manifest.getPath();
              boolean _endsWith = _path_2.endsWith(".MF");
              if (_endsWith) {
                String _path_3 = manifest.getPath();
                String _plus = (_path_3 + "_gen");
                manifest.setPath(_plus);
                TextFileAccess _createManifest = XtextGenerator.this.templates.createManifest(manifest, activator);
                _createManifest.writeToFile();
              }
            }
          } else {
            TextFileAccess _createManifest_1 = XtextGenerator.this.templates.createManifest(manifest, activator);
            _createManifest_1.writeToFile();
          }
        } catch (Throwable _e) {
          throw Exceptions.sneakyThrow(_e);
        }
      }
    };
    IterableExtensions.<ManifestAccess>forEach(_sortBy, _function_1);
  }
  
  protected void mergeManifest(final ManifestAccess manifest, final File file, final TypeReference activator) throws IOException {
    InputStream in = null;
    OutputStream out = null;
    try {
      FileInputStream _fileInputStream = new FileInputStream(file);
      in = _fileInputStream;
      String _bundleName = manifest.getBundleName();
      final MergeableManifest merge = new MergeableManifest(in, _bundleName);
      Set<String> _exportedPackages = manifest.getExportedPackages();
      merge.addExportedPackages(_exportedPackages);
      Set<String> _requiredBundles = manifest.getRequiredBundles();
      merge.addRequiredBundles(_requiredBundles);
      Set<String> _importedPackages = manifest.getImportedPackages();
      merge.addImportedPackages(_importedPackages);
      boolean _and = false;
      if (!(activator != null)) {
        _and = false;
      } else {
        Attributes _mainAttributes = merge.getMainAttributes();
        boolean _containsKey = _mainAttributes.containsKey(MergeableManifest.BUNDLE_ACTIVATOR);
        boolean _not = (!_containsKey);
        _and = _not;
      }
      if (_and) {
        Attributes _mainAttributes_1 = merge.getMainAttributes();
        String _name = activator.getName();
        _mainAttributes_1.put(MergeableManifest.BUNDLE_ACTIVATOR, _name);
      }
      boolean _isModified = merge.isModified();
      if (_isModified) {
        FileOutputStream _fileOutputStream = new FileOutputStream(file);
        out = _fileOutputStream;
        merge.write(out);
      }
    } finally {
      if ((in != null)) {
        in.close();
      }
      boolean _notEquals = (!Objects.equal(out, null));
      if (_notEquals) {
        out.close();
      }
    }
  }
  
  protected void generateActivator() {
    boolean _and = false;
    IFileSystemAccess2 _eclipsePluginSrcGen = this.projectConfig.getEclipsePluginSrcGen();
    boolean _tripleNotEquals = (_eclipsePluginSrcGen != null);
    if (!_tripleNotEquals) {
      _and = false;
    } else {
      boolean _isEmpty = this.languageConfigs.isEmpty();
      boolean _not = (!_isEmpty);
      _and = _not;
    }
    if (_and) {
      JavaFileAccess _createEclipsePluginActivator = this.templates.createEclipsePluginActivator(this.languageConfigs);
      IFileSystemAccess2 _eclipsePluginSrcGen_1 = this.projectConfig.getEclipsePluginSrcGen();
      _createEclipsePluginActivator.writeTo(_eclipsePluginSrcGen_1);
    }
  }
  
  protected void generatePluginXmls() {
    PluginXmlAccess _runtimePluginXml = this.projectConfig.getRuntimePluginXml();
    PluginXmlAccess _runtimeTestPluginXml = this.projectConfig.getRuntimeTestPluginXml();
    PluginXmlAccess _genericIdePluginXml = this.projectConfig.getGenericIdePluginXml();
    PluginXmlAccess _genericIdeTestPluginXml = this.projectConfig.getGenericIdeTestPluginXml();
    PluginXmlAccess _eclipsePluginPluginXml = this.projectConfig.getEclipsePluginPluginXml();
    PluginXmlAccess _eclipsePluginTestPluginXml = this.projectConfig.getEclipsePluginTestPluginXml();
    PluginXmlAccess _ideaPluginPluginXml = this.projectConfig.getIdeaPluginPluginXml();
    PluginXmlAccess _ideaPluginTestPluginXml = this.projectConfig.getIdeaPluginTestPluginXml();
    PluginXmlAccess _webPluginXml = this.projectConfig.getWebPluginXml();
    PluginXmlAccess _webTestPluginXml = this.projectConfig.getWebTestPluginXml();
    final Set<PluginXmlAccess> pluginXmls = Collections.<PluginXmlAccess>unmodifiableSet(CollectionLiterals.<PluginXmlAccess>newHashSet(_runtimePluginXml, _runtimeTestPluginXml, _genericIdePluginXml, _genericIdeTestPluginXml, _eclipsePluginPluginXml, _eclipsePluginTestPluginXml, _ideaPluginPluginXml, _ideaPluginTestPluginXml, _webPluginXml, _webTestPluginXml));
    Iterable<PluginXmlAccess> _filterNull = IterableExtensions.<PluginXmlAccess>filterNull(pluginXmls);
    final Function1<PluginXmlAccess, String> _function = new Function1<PluginXmlAccess, String>() {
      @Override
      public String apply(final PluginXmlAccess it) {
        return it.getPath();
      }
    };
    List<PluginXmlAccess> _sortBy = IterableExtensions.<PluginXmlAccess, String>sortBy(_filterNull, _function);
    final Procedure1<PluginXmlAccess> _function_1 = new Procedure1<PluginXmlAccess>() {
      @Override
      public void apply(final PluginXmlAccess pluginXml) {
        try {
          String _path = pluginXml.getPath();
          File _file = new File(_path);
          boolean _exists = _file.exists();
          if (_exists) {
            String _path_1 = pluginXml.getPath();
            boolean _endsWith = _path_1.endsWith(".xml");
            if (_endsWith) {
              String _path_2 = pluginXml.getPath();
              String _plus = (_path_2 + "_gen");
              pluginXml.setPath(_plus);
              TextFileAccess _createPluginXml = XtextGenerator.this.templates.createPluginXml(pluginXml);
              _createPluginXml.writeToFile();
            }
          } else {
            TextFileAccess _createPluginXml_1 = XtextGenerator.this.templates.createPluginXml(pluginXml);
            _createPluginXml_1.writeToFile();
          }
        } catch (Throwable _e) {
          throw Exceptions.sneakyThrow(_e);
        }
      }
    };
    IterableExtensions.<PluginXmlAccess>forEach(_sortBy, _function_1);
  }
  
  private final static Logger LOG = Logger.getLogger(XtextGenerator.class);
  
  @Pure
  public DefaultGeneratorModule getConfiguration() {
    return this.configuration;
  }
  
  public void setConfiguration(final DefaultGeneratorModule configuration) {
    this.configuration = configuration;
  }
  
  @Pure
  public List<LanguageConfig2> getLanguageConfigs() {
    return this.languageConfigs;
  }
}
