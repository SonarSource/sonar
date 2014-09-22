/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.fs.internal;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.PathUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @since 4.2
 */
public class DefaultFileSystem implements FileSystem {

  private final Cache cache;
  private final SortedSet<String> languages = new TreeSet<String>();
  private File baseDir, workDir;
  private Charset encoding;
  private final FilePredicates predicates = new DefaultFilePredicates();

  /**
   * Only for testing
   */
  public DefaultFileSystem() {
    this.cache = new MapCache();
  }

  protected DefaultFileSystem(Cache cache) {
    this.cache = cache;
  }

  public DefaultFileSystem setBaseDir(File d) {
    Preconditions.checkNotNull(d, "Base directory can't be null");
    this.baseDir = d.getAbsoluteFile();
    return this;
  }

  @Override
  public File baseDir() {
    return baseDir;
  }

  public DefaultFileSystem setEncoding(@Nullable Charset e) {
    this.encoding = e;
    return this;
  }

  @Override
  public Charset encoding() {
    return encoding == null ? Charset.defaultCharset() : encoding;
  }

  public boolean isDefaultJvmEncoding() {
    return encoding == null;
  }

  public DefaultFileSystem setWorkDir(File d) {
    this.workDir = d.getAbsoluteFile();
    return this;
  }

  @Override
  public File workDir() {
    return workDir;
  }

  @Override
  public InputFile inputFile(FilePredicate predicate) {
    doPreloadFiles();
    if (predicate instanceof RelativePathPredicate) {
      return cache.inputFile((RelativePathPredicate) predicate);
    }
    Iterable<InputFile> files = inputFiles(predicate);
    Iterator<InputFile> iterator = files.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    InputFile first = iterator.next();
    if (!iterator.hasNext()) {
      return first;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("expected one element but was: <" + first);
    for (int i = 0; i < 4 && iterator.hasNext(); i++) {
      sb.append(", " + iterator.next());
    }
    if (iterator.hasNext()) {
      sb.append(", ...");
    }
    sb.append('>');

    throw new IllegalArgumentException(sb.toString());

  }

  @Override
  public Iterable<InputFile> inputFiles(FilePredicate predicate) {
    doPreloadFiles();
    return filter(cache.inputFiles(), predicate);
  }

  @Override
  public boolean hasFiles(FilePredicate predicate) {
    doPreloadFiles();
    for (InputFile element : cache.inputFiles()) {
      if (predicate.apply(element)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Iterable<File> files(FilePredicate predicate) {
    doPreloadFiles();
    Collection<File> result = new ArrayList<File>();
    for (InputFile element : inputFiles(predicate)) {
      if (predicate.apply(element)) {
        result.add(element.file());
      }
    }
    return result;
  }

  @Override
  public InputDir inputDir(File dir) {
    doPreloadFiles();
    String relativePath = PathUtils.sanitize(new PathResolver().relativePath(baseDir, dir));
    if (relativePath == null) {
      return null;
    }
    return cache.inputDir(relativePath);
  }

  public static Collection<InputFile> filter(Iterable<InputFile> target, FilePredicate predicate) {
    Collection<InputFile> result = new ArrayList<InputFile>();
    for (InputFile element : target) {
      if (predicate.apply(element)) {
        result.add(element);
      }
    }
    return result;
  }

  /**
   * Adds InputFile to the list and registers its language, if present.
   */
  public DefaultFileSystem add(InputFile inputFile) {
    cache.add(inputFile);
    if (inputFile.language() != null) {
      languages.add(inputFile.language());
    }
    return this;
  }

  /**
   * Adds InputDir to the list.
   */
  public DefaultFileSystem add(InputDir inputDir) {
    cache.add(inputDir);
    return this;
  }

  /**
   * Adds a language to the list. To be used only for unit tests that need to use {@link #languages()} without
   * using {@link #add(org.sonar.api.batch.fs.InputFile)}.
   */
  public DefaultFileSystem addLanguages(String language, String... others) {
    languages.add(language);
    Collections.addAll(languages, others);
    return this;
  }

  @Override
  public SortedSet<String> languages() {
    doPreloadFiles();
    return languages;
  }

  @Override
  public FilePredicates predicates() {
    return predicates;
  }

  /**
   * This method is called before each search of files.
   */
  protected void doPreloadFiles() {
    // nothing to do by default
  }

  public abstract static class Cache {
    protected abstract Iterable<InputFile> inputFiles();

    @CheckForNull
    protected abstract InputFile inputFile(RelativePathPredicate predicate);

    @CheckForNull
    protected abstract InputDir inputDir(String relativePath);

    protected abstract void doAdd(InputFile inputFile);

    protected abstract void doAdd(InputDir inputDir);

    final void add(InputFile inputFile) {
      doAdd(inputFile);
    }

    public void add(InputDir inputDir) {
      doAdd(inputDir);
    }

  }

  /**
   * Used only for testing
   */
  private static class MapCache extends Cache {
    private final Map<String, InputFile> fileMap = new HashMap<String, InputFile>();
    private final Map<String, InputDir> dirMap = new HashMap<String, InputDir>();

    @Override
    public Iterable<InputFile> inputFiles() {
      return new ArrayList<InputFile>(fileMap.values());
    }

    @Override
    public InputFile inputFile(RelativePathPredicate predicate) {
      return fileMap.get(predicate.path());
    }

    @Override
    protected InputDir inputDir(String relativePath) {
      return dirMap.get(relativePath);
    }

    @Override
    protected void doAdd(InputFile inputFile) {
      fileMap.put(inputFile.relativePath(), inputFile);
    }

    @Override
    protected void doAdd(InputDir inputDir) {
      dirMap.put(inputDir.relativePath(), inputDir);
    }
  }

}
