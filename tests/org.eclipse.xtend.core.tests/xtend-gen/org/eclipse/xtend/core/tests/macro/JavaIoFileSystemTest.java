/**
 * Copyright (c) 2013 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtend.core.tests.macro;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.List;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtend.core.macro.JavaIOFileSystemSupport;
import org.eclipse.xtend.lib.macro.file.MutableFileSystemSupport;
import org.eclipse.xtend.lib.macro.file.Path;
import org.eclipse.xtext.junit4.TemporaryFolder;
import org.eclipse.xtext.parser.IEncodingProvider;
import org.eclipse.xtext.workspace.FileProjectConfig;
import org.eclipse.xtext.workspace.FileWorkspaceConfig;
import org.eclipse.xtext.workspace.IWorkspaceConfig;
import org.eclipse.xtext.workspace.IWorkspaceConfigProvider;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
@SuppressWarnings("all")
public class JavaIoFileSystemTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  
  @Extension
  protected MutableFileSystemSupport fs;
  
  @Before
  public void setUp() {
    try {
      final File tempDir = this.temporaryFolder.newFolder();
      JavaIOFileSystemSupport _javaIOFileSystemSupport = new JavaIOFileSystemSupport();
      final Procedure1<JavaIOFileSystemSupport> _function = new Procedure1<JavaIOFileSystemSupport>() {
        @Override
        public void apply(final JavaIOFileSystemSupport it) {
          final IWorkspaceConfigProvider _function = new IWorkspaceConfigProvider() {
            @Override
            public IWorkspaceConfig getWorkspaceConfig(final ResourceSet context) {
              FileWorkspaceConfig _fileWorkspaceConfig = new FileWorkspaceConfig(tempDir);
              final Procedure1<FileWorkspaceConfig> _function = new Procedure1<FileWorkspaceConfig>() {
                @Override
                public void apply(final FileWorkspaceConfig it) {
                  FileProjectConfig _addProject = it.addProject("foo");
                  final Procedure1<FileProjectConfig> _function = new Procedure1<FileProjectConfig>() {
                    @Override
                    public void apply(final FileProjectConfig it) {
                      it.addSourceFolder("src");
                    }
                  };
                  ObjectExtensions.<FileProjectConfig>operator_doubleArrow(_addProject, _function);
                  FileProjectConfig _addProject_1 = it.addProject("bar");
                  final Procedure1<FileProjectConfig> _function_1 = new Procedure1<FileProjectConfig>() {
                    @Override
                    public void apply(final FileProjectConfig it) {
                      it.addSourceFolder("src");
                    }
                  };
                  ObjectExtensions.<FileProjectConfig>operator_doubleArrow(_addProject_1, _function_1);
                }
              };
              return ObjectExtensions.<FileWorkspaceConfig>operator_doubleArrow(_fileWorkspaceConfig, _function);
            }
          };
          it.setProjectInformationProvider(_function);
          IEncodingProvider.Runtime _runtime = new IEncodingProvider.Runtime();
          it.setEncodingProvider(_runtime);
        }
      };
      JavaIOFileSystemSupport _doubleArrow = ObjectExtensions.<JavaIOFileSystemSupport>operator_doubleArrow(_javaIOFileSystemSupport, _function);
      this.fs = _doubleArrow;
      Path _path = new Path("/foo");
      this.fs.mkdir(_path);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testMakeandDeleteFolder() {
    final Path someFolder = new Path("/foo/bar");
    boolean _exists = this.fs.exists(someFolder);
    Assert.assertFalse(_exists);
    boolean _isFile = this.fs.isFile(someFolder);
    Assert.assertFalse(_isFile);
    boolean _isFolder = this.fs.isFolder(someFolder);
    Assert.assertFalse(_isFolder);
    this.fs.mkdir(someFolder);
    boolean _isFile_1 = this.fs.isFile(someFolder);
    Assert.assertFalse(_isFile_1);
    boolean _isFolder_1 = this.fs.isFolder(someFolder);
    Assert.assertTrue(_isFolder_1);
    boolean _exists_1 = this.fs.exists(someFolder);
    Assert.assertTrue(_exists_1);
    this.fs.delete(someFolder);
    boolean _exists_2 = this.fs.exists(someFolder);
    Assert.assertFalse(_exists_2);
  }
  
  @Test
  public void testMakeandDeleteFile() {
    final Path path = new Path("/foo/src/my/pack/Foo.txt");
    boolean _exists = this.fs.exists(path);
    Assert.assertFalse(_exists);
    this.fs.setContents(path, "Hello Foo");
    boolean _exists_1 = this.fs.exists(path);
    Assert.assertTrue(_exists_1);
    CharSequence _contents = this.fs.getContents(path);
    Assert.assertEquals("Hello Foo", _contents);
    boolean _isFile = this.fs.isFile(path);
    Assert.assertTrue(_isFile);
    boolean _isFolder = this.fs.isFolder(path);
    Assert.assertFalse(_isFolder);
    this.fs.delete(path);
    boolean _exists_2 = this.fs.exists(path);
    Assert.assertFalse(_exists_2);
    boolean _isFile_1 = this.fs.isFile(path);
    Assert.assertFalse(_isFile_1);
    boolean _isFolder_1 = this.fs.isFolder(path);
    Assert.assertFalse(_isFolder_1);
  }
  
  @Test
  public void testModificationStamp() {
    try {
      final Path path = new Path("/foo/src/my/pack/Foo.txt");
      long _lastModification = this.fs.getLastModification(path);
      Assert.assertEquals(0L, _lastModification);
      this.fs.setContents(path, "Hello Foo");
      final long mod = this.fs.getLastModification(path);
      CharSequence _contents = this.fs.getContents(path);
      Assert.assertEquals("Hello Foo", _contents);
      long _lastModification_1 = this.fs.getLastModification(path);
      Assert.assertEquals(mod, _lastModification_1);
      Thread.sleep(1000);
      this.fs.setContents(path, "Hello Foo");
      long _lastModification_2 = this.fs.getLastModification(path);
      boolean _lessThan = (mod < _lastModification_2);
      Assert.assertTrue(_lessThan);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testGetWorkspaceChildren() {
    Iterable<? extends Path> _children = this.fs.getChildren(Path.ROOT);
    final Function1<Path, CharSequence> _function = new Function1<Path, CharSequence>() {
      @Override
      public CharSequence apply(final Path it) {
        List<String> _segments = it.getSegments();
        return IterableExtensions.join(_segments, ".");
      }
    };
    String _join = IterableExtensions.join(_children, "[", ", ", "]", _function);
    Iterable<? extends Path> _children_1 = this.fs.getChildren(Path.ROOT);
    int _size = IterableExtensions.size(_children_1);
    Assert.assertEquals(_join, 2, _size);
    final Path path = new Path("/bar");
    this.fs.mkdir(path);
    boolean _exists = this.fs.exists(path);
    Assert.assertTrue(_exists);
    Iterable<? extends Path> _children_2 = this.fs.getChildren(Path.ROOT);
    final Function1<Path, CharSequence> _function_1 = new Function1<Path, CharSequence>() {
      @Override
      public CharSequence apply(final Path it) {
        List<String> _segments = it.getSegments();
        return IterableExtensions.join(_segments, ".");
      }
    };
    String _join_1 = IterableExtensions.join(_children_2, "[", ", ", "]", _function_1);
    Iterable<? extends Path> _children_3 = this.fs.getChildren(Path.ROOT);
    int _size_1 = IterableExtensions.size(_children_3);
    Assert.assertEquals(_join_1, 3, _size_1);
  }
  
  @Test
  public void testGetProjectChildren() {
    final Path projectFolder = new Path("/foo");
    boolean _exists = this.fs.exists(projectFolder);
    Assert.assertTrue(_exists);
    Iterable<? extends Path> _children = this.fs.getChildren(projectFolder);
    int _size = IterableExtensions.size(_children);
    final int expectedChildrenSize = (_size + 1);
    Path _path = new Path("/foo/Foo.text");
    this.fs.setContents(_path, "Hello Foo");
    Iterable<? extends Path> _children_1 = this.fs.getChildren(projectFolder);
    int _size_1 = IterableExtensions.size(_children_1);
    Assert.assertEquals(expectedChildrenSize, _size_1);
  }
  
  @Test
  public void testGetFolderChildren() {
    final Path folder = new Path("/foo/bar");
    boolean _exists = this.fs.exists(folder);
    Assert.assertFalse(_exists);
    this.fs.mkdir(folder);
    boolean _exists_1 = this.fs.exists(folder);
    Assert.assertTrue(_exists_1);
    Iterable<? extends Path> _children = this.fs.getChildren(folder);
    int _size = IterableExtensions.size(_children);
    Assert.assertEquals(0, _size);
    Path _path = new Path("/foo/bar/Foo.text");
    this.fs.setContents(_path, "Hello Foo");
    Iterable<? extends Path> _children_1 = this.fs.getChildren(folder);
    int _size_1 = IterableExtensions.size(_children_1);
    Assert.assertEquals(1, _size_1);
  }
  
  @Test
  public void testGetFileChildren() {
    final Path file = new Path("/foo/bar/Foo.text");
    boolean _exists = this.fs.exists(file);
    Assert.assertFalse(_exists);
    Iterable<? extends Path> _children = this.fs.getChildren(file);
    int _size = IterableExtensions.size(_children);
    Assert.assertEquals(0, _size);
    this.fs.setContents(file, "Hello Foo");
    boolean _exists_1 = this.fs.exists(file);
    Assert.assertTrue(_exists_1);
    Iterable<? extends Path> _children_1 = this.fs.getChildren(file);
    int _size_1 = IterableExtensions.size(_children_1);
    Assert.assertEquals(0, _size_1);
  }
  
  @Test
  public void testGetURI() {
    try {
      final Path file = new Path("/foo/bar/Foo.text");
      boolean _exists = this.fs.exists(file);
      Assert.assertFalse(_exists);
      URI _uRI = this.fs.toURI(file);
      Assert.assertNotNull(_uRI);
      this.fs.setContents(file, "Hello Foo");
      boolean _exists_1 = this.fs.exists(file);
      Assert.assertTrue(_exists_1);
      URI _uRI_1 = this.fs.toURI(file);
      Assert.assertNotNull(_uRI_1);
      URI _uRI_2 = this.fs.toURI(file);
      final File javaIoFile = new File(_uRI_2);
      boolean _exists_2 = javaIoFile.exists();
      Assert.assertTrue(_exists_2);
      long _length = javaIoFile.length();
      final byte[] data = new byte[((int) _length)];
      final FileInputStream fis = new FileInputStream(javaIoFile);
      fis.read(data);
      fis.close();
      String _string = new String(data);
      Assert.assertEquals("Hello Foo", _string);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
