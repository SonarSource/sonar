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
package org.sonar.batch.scan;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.bootstrap.TaskProperties;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Class that creates a project definition based on a set of properties.
 */
public class ProjectReactorBuilder {

  private static final String INVALID_VALUE_OF_X_FOR_Y = "Invalid value of {0} for {1}";

  private static final Logger LOG = LoggerFactory.getLogger(ProjectReactorBuilder.class);

  /**
   * @since 4.1 but not yet exposed in {@link CoreProperties}
   */
  private static final String MODULE_KEY_PROPERTY = "sonar.moduleKey";

  protected static final String PROPERTY_PROJECT_BASEDIR = "sonar.projectBaseDir";
  private static final String PROPERTY_PROJECT_BUILDDIR = "sonar.projectBuildDir";
  private static final String PROPERTY_MODULES = "sonar.modules";

  /**
   * New properties, to be consistent with Sonar naming conventions
   *
   * @since 1.5
   */
  private static final String PROPERTY_SOURCES = "sonar.sources";
  private static final String PROPERTY_TESTS = "sonar.tests";
  private static final String PROPERTY_BINARIES = "sonar.binaries";
  private static final String PROPERTY_LIBRARIES = "sonar.libraries";

  /**
   * Array of all mandatory properties required for a project without child.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_SIMPLE_PROJECT = {
    PROPERTY_PROJECT_BASEDIR, CoreProperties.PROJECT_KEY_PROPERTY, CoreProperties.PROJECT_NAME_PROPERTY,
    CoreProperties.PROJECT_VERSION_PROPERTY, PROPERTY_SOURCES
  };

  /**
   * Array of all mandatory properties required for a project with children.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT = {PROPERTY_PROJECT_BASEDIR, CoreProperties.PROJECT_KEY_PROPERTY,
    CoreProperties.PROJECT_NAME_PROPERTY, CoreProperties.PROJECT_VERSION_PROPERTY};

  /**
   * Array of all mandatory properties required for a child project before its properties get merged with its parent ones.
   */
  protected static final String[] MANDATORY_PROPERTIES_FOR_CHILD = {MODULE_KEY_PROPERTY, CoreProperties.PROJECT_NAME_PROPERTY};

  /**
   * Properties that must not be passed from the parent project to its children.
   */
  private static final List<String> NON_HERITED_PROPERTIES_FOR_CHILD = Lists.newArrayList(PROPERTY_PROJECT_BASEDIR, CoreProperties.WORKING_DIRECTORY, PROPERTY_MODULES,
    CoreProperties.PROJECT_DESCRIPTION_PROPERTY);

  private TaskProperties props;
  private File rootProjectWorkDir;

  public ProjectReactorBuilder(TaskProperties props) {
    this.props = props;
  }

  public ProjectReactor execute() {
    Properties bootstrapProperties = new Properties();
    bootstrapProperties.putAll(props.properties());
    ProjectDefinition rootProject = defineProject(bootstrapProperties, null);
    rootProjectWorkDir = rootProject.getWorkDir();
    defineChildren(rootProject);
    cleanAndCheckProjectDefinitions(rootProject);
    return new ProjectReactor(rootProject);
  }

  protected ProjectDefinition defineProject(Properties properties, @Nullable ProjectDefinition parent) {
    if (properties.containsKey(PROPERTY_MODULES)) {
      checkMandatoryProperties(properties, MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT);
    } else {
      checkMandatoryProperties(properties, MANDATORY_PROPERTIES_FOR_SIMPLE_PROJECT);
    }
    File baseDir = new File(properties.getProperty(PROPERTY_PROJECT_BASEDIR));
    final String projectKey = properties.getProperty(CoreProperties.PROJECT_KEY_PROPERTY);
    File workDir;
    if (parent == null) {
      validateDirectories(properties, baseDir, projectKey);
      workDir = initRootProjectWorkDir(baseDir);
    } else {
      workDir = initModuleWorkDir(baseDir, properties);
    }

    return ProjectDefinition.create().setProperties(properties)
      .setBaseDir(baseDir)
      .setWorkDir(workDir)
      .setBuildDir(initModuleBuildDir(baseDir, properties));
  }

  @VisibleForTesting
  protected File initRootProjectWorkDir(File baseDir) {
    String workDir = props.property(CoreProperties.WORKING_DIRECTORY);
    if (StringUtils.isBlank(workDir)) {
      return new File(baseDir, CoreProperties.WORKING_DIRECTORY_DEFAULT_VALUE);
    }

    File customWorkDir = new File(workDir);
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(baseDir, customWorkDir.getPath());
  }

  @VisibleForTesting
  protected File initModuleWorkDir(File moduleBaseDir, Properties moduleProperties) {
    String workDir = moduleProperties.getProperty(CoreProperties.WORKING_DIRECTORY);
    if (StringUtils.isBlank(workDir)) {
      String cleanKey = StringUtils.deleteWhitespace(moduleProperties.getProperty(CoreProperties.PROJECT_KEY_PROPERTY));
      cleanKey = StringUtils.replace(cleanKey, ":", "_");
      return new File(rootProjectWorkDir, cleanKey);
    }

    File customWorkDir = new File(workDir);
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(moduleBaseDir, customWorkDir.getPath());
  }

  @CheckForNull
  private File initModuleBuildDir(File moduleBaseDir, Properties moduleProperties) {
    String buildDir = moduleProperties.getProperty(PROPERTY_PROJECT_BUILDDIR);
    if (StringUtils.isBlank(buildDir)) {
      return null;
    }

    File customBuildDir = new File(buildDir);
    if (customBuildDir.isAbsolute()) {
      return customBuildDir;
    }
    return new File(moduleBaseDir, customBuildDir.getPath());
  }

  private void defineChildren(ProjectDefinition parentProject) {
    Properties parentProps = parentProject.getProperties();
    if (parentProps.containsKey(PROPERTY_MODULES)) {
      for (String module : getListFromProperty(parentProps, PROPERTY_MODULES)) {
        Properties moduleProps = extractModuleProperties(module, parentProps);
        ProjectDefinition childProject = loadChildProject(parentProject, moduleProps, module);
        // check the uniqueness of the child key
        checkUniquenessOfChildKey(childProject, parentProject);
        // the child project may have children as well
        defineChildren(childProject);
        // and finally add this child project to its parent
        parentProject.addSubProject(childProject);
      }
    }
  }

  protected ProjectDefinition loadChildProject(ProjectDefinition parentProject, Properties moduleProps, String moduleId) {
    final File baseDir;
    if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
      baseDir = resolvePath(parentProject.getBaseDir(), moduleProps.getProperty(PROPERTY_PROJECT_BASEDIR));
      setProjectBaseDir(baseDir, moduleProps, moduleId);
    } else {
      baseDir = new File(parentProject.getBaseDir(), moduleId);
      setProjectBaseDir(baseDir, moduleProps, moduleId);
    }

    setModuleKeyAndNameIfNotDefined(moduleProps, moduleId, parentProject.getKey());

    // and finish
    checkMandatoryProperties(moduleProps, MANDATORY_PROPERTIES_FOR_CHILD);
    validateDirectories(moduleProps, baseDir, moduleId);

    mergeParentProperties(moduleProps, parentProject.getProperties());

    return defineProject(moduleProps, parentProject);
  }

  @VisibleForTesting
  protected static Properties toProperties(File propertyFile) {
    Properties propsFromFile = new Properties();
    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(propertyFile);
      propsFromFile.load(fileInputStream);
    } catch (IOException e) {
      throw new IllegalStateException("Impossible to read the property file: " + propertyFile.getAbsolutePath(), e);
    } finally {
      IOUtils.closeQuietly(fileInputStream);
    }
    // Trim properties
    for (String propKey : propsFromFile.stringPropertyNames()) {
      propsFromFile.setProperty(propKey, StringUtils.trim(propsFromFile.getProperty(propKey)));
    }
    return propsFromFile;
  }

  @VisibleForTesting
  protected static void setModuleKeyAndNameIfNotDefined(Properties childProps, String moduleId, String parentKey) {
    if (!childProps.containsKey(MODULE_KEY_PROPERTY)) {
      if (!childProps.containsKey(CoreProperties.PROJECT_KEY_PROPERTY)) {
        childProps.put(MODULE_KEY_PROPERTY, parentKey + ":" + moduleId);
      } else {
        String childKey = childProps.getProperty(CoreProperties.PROJECT_KEY_PROPERTY);
        childProps.put(MODULE_KEY_PROPERTY, parentKey + ":" + childKey);
      }
    }
    if (!childProps.containsKey(CoreProperties.PROJECT_NAME_PROPERTY)) {
      childProps.put(CoreProperties.PROJECT_NAME_PROPERTY, moduleId);
    }
    // For backward compatibility with ProjectDefinition
    childProps.put(CoreProperties.PROJECT_KEY_PROPERTY, childProps.getProperty(MODULE_KEY_PROPERTY));
  }

  @VisibleForTesting
  protected static void checkUniquenessOfChildKey(ProjectDefinition childProject, ProjectDefinition parentProject) {
    for (ProjectDefinition definition : parentProject.getSubProjects()) {
      if (definition.getKey().equals(childProject.getKey())) {
        throw new IllegalStateException("Project '" + parentProject.getKey() + "' can't have 2 modules with the following key: " + childProject.getKey());
      }
    }
  }

  protected static void setProjectBaseDir(File baseDir, Properties childProps, String moduleId) {
    if (!baseDir.isDirectory()) {
      throw new IllegalStateException("The base directory of the module '" + moduleId + "' does not exist: " + baseDir.getAbsolutePath());
    }
    childProps.put(PROPERTY_PROJECT_BASEDIR, baseDir.getAbsolutePath());
  }

  @VisibleForTesting
  protected static void checkMandatoryProperties(Properties props, String[] mandatoryProps) {
    StringBuilder missing = new StringBuilder();
    for (String mandatoryProperty : mandatoryProps) {
      if (!props.containsKey(mandatoryProperty)) {
        if (missing.length() > 0) {
          missing.append(", ");
        }
        missing.append(mandatoryProperty);
      }
    }
    String moduleKey = StringUtils.defaultIfBlank(props.getProperty(MODULE_KEY_PROPERTY), props.getProperty(CoreProperties.PROJECT_KEY_PROPERTY));
    if (missing.length() != 0) {
      throw new IllegalStateException("You must define the following mandatory properties for '" + (moduleKey == null ? "Unknown" : moduleKey) + "': " + missing);
    }
  }

  protected static void validateDirectories(Properties props, File baseDir, String projectId) {
    if (!props.containsKey(PROPERTY_MODULES)) {
      // SONARPLUGINS-2285 Not an aggregator project so we can validate that paths are correct if defined

      // We need to resolve patterns that may have been used in "sonar.libraries"
      for (String pattern : getListFromProperty(props, PROPERTY_LIBRARIES)) {
        File[] files = getLibraries(baseDir, pattern);
        if (files == null || files.length == 0) {
          LOG.error(MessageFormat.format(INVALID_VALUE_OF_X_FOR_Y, PROPERTY_LIBRARIES, projectId));
          throw new IllegalStateException("No files nor directories matching '" + pattern + "' in directory " + baseDir);
        }
      }

      // Check sonar.tests
      String[] testPaths = getListFromProperty(props, PROPERTY_TESTS);
      checkExistenceOfPaths(projectId, baseDir, testPaths, PROPERTY_TESTS);

      // Check sonar.binaries
      String[] binDirs = getListFromProperty(props, PROPERTY_BINARIES);
      checkExistenceOfDirectories(projectId, baseDir, binDirs, PROPERTY_BINARIES);
    }
  }

  @VisibleForTesting
  protected static void cleanAndCheckProjectDefinitions(ProjectDefinition project) {
    if (project.getSubProjects().isEmpty()) {
      cleanAndCheckModuleProperties(project);
    } else {
      cleanAndCheckAggregatorProjectProperties(project);

      // clean modules properties as well
      for (ProjectDefinition module : project.getSubProjects()) {
        cleanAndCheckProjectDefinitions(module);
      }
    }
  }

  @VisibleForTesting
  protected static void cleanAndCheckModuleProperties(ProjectDefinition project) {
    Properties properties = project.getProperties();

    // We need to check the existence of source directories
    String[] sourcePaths = getListFromProperty(properties, PROPERTY_SOURCES);
    checkExistenceOfPaths(project.getKey(), project.getBaseDir(), sourcePaths, PROPERTY_SOURCES);

    // And we need to resolve patterns that may have been used in "sonar.libraries"
    List<String> libPaths = Lists.newArrayList();
    for (String pattern : getListFromProperty(properties, PROPERTY_LIBRARIES)) {
      for (File file : getLibraries(project.getBaseDir(), pattern)) {
        libPaths.add(file.getAbsolutePath());
      }
    }
    properties.remove(PROPERTY_LIBRARIES);
    properties.put(PROPERTY_LIBRARIES, StringUtils.join(libPaths, ","));
  }

  @VisibleForTesting
  protected static void cleanAndCheckAggregatorProjectProperties(ProjectDefinition project) {
    Properties properties = project.getProperties();

    // SONARPLUGINS-2295
    String[] sourceDirs = getListFromProperty(properties, PROPERTY_SOURCES);
    for (String path : sourceDirs) {
      File sourceFolder = resolvePath(project.getBaseDir(), path);
      if (sourceFolder.isDirectory()) {
        LOG.warn("/!\\ A multi-module project can't have source folders, so '{}' won't be used for the analysis. " +
          "If you want to analyse files of this folder, you should create another sub-module and move them inside it.",
          sourceFolder.toString());
      }
    }

    // "aggregator" project must not have the following properties:
    properties.remove(PROPERTY_SOURCES);
    properties.remove(PROPERTY_TESTS);
    properties.remove(PROPERTY_BINARIES);
    properties.remove(PROPERTY_LIBRARIES);

    // and they don't need properties related to their modules either
    Properties clone = (Properties) properties.clone();
    List<String> moduleIds = Lists.newArrayList(getListFromProperty(properties, PROPERTY_MODULES));
    for (Entry<Object, Object> entry : clone.entrySet()) {
      String key = (String) entry.getKey();
      if (isKeyPrefixedByModuleId(key, moduleIds)) {
        properties.remove(key);
      }
    }
  }

  @VisibleForTesting
  protected static void mergeParentProperties(Properties childProps, Properties parentProps) {
    List<String> moduleIds = Lists.newArrayList(getListFromProperty(parentProps, PROPERTY_MODULES));
    for (Map.Entry<Object, Object> entry : parentProps.entrySet()) {
      String key = (String) entry.getKey();
      if (!childProps.containsKey(key)
        && !NON_HERITED_PROPERTIES_FOR_CHILD.contains(key)
        && !isKeyPrefixedByModuleId(key, moduleIds)) {
        childProps.put(entry.getKey(), entry.getValue());
      }
    }
  }

  private static boolean isKeyPrefixedByModuleId(String key, List<String> moduleIds) {
    for (String moduleId : moduleIds) {
      if (key.startsWith(moduleId + ".")) {
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  protected static Properties extractModuleProperties(String module, Properties properties) {
    Properties moduleProps = new Properties();
    String propertyPrefix = module + ".";
    int prefixLength = propertyPrefix.length();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      if (key.startsWith(propertyPrefix)) {
        moduleProps.put(key.substring(prefixLength), entry.getValue());
      }
    }
    return moduleProps;
  }

  @VisibleForTesting
  protected static void checkExistenceOfDirectories(String moduleRef, File baseDir, String[] dirPaths, String propName) {
    for (String path : dirPaths) {
      File sourceFolder = resolvePath(baseDir, path);
      if (!sourceFolder.isDirectory()) {
        LOG.error(MessageFormat.format(INVALID_VALUE_OF_X_FOR_Y, propName, moduleRef));
        throw new IllegalStateException("The folder '" + path + "' does not exist for '" + moduleRef +
          "' (base directory = " + baseDir.getAbsolutePath() + ")");
      }
    }

  }

  @VisibleForTesting
  protected static void checkExistenceOfPaths(String moduleRef, File baseDir, String[] paths, String propName) {
    for (String path : paths) {
      File sourceFolder = resolvePath(baseDir, path);
      if (!sourceFolder.exists()) {
        LOG.error(MessageFormat.format(INVALID_VALUE_OF_X_FOR_Y, propName, moduleRef));
        throw new IllegalStateException("The folder '" + path + "' does not exist for '" + moduleRef +
          "' (base directory = " + baseDir.getAbsolutePath() + ")");
      }
    }

  }

  /**
   * Returns files matching specified pattern.
   */
  @VisibleForTesting
  protected static File[] getLibraries(File baseDir, String pattern) {
    final int i = Math.max(pattern.lastIndexOf('/'), pattern.lastIndexOf('\\'));
    final String dirPath, filePattern;
    if (i == -1) {
      dirPath = ".";
      filePattern = pattern;
    } else {
      dirPath = pattern.substring(0, i);
      filePattern = pattern.substring(i + 1);
    }
    List<IOFileFilter> filters = new ArrayList<IOFileFilter>();
    if (pattern.indexOf('*') >= 0) {
      filters.add(FileFileFilter.FILE);
    }
    filters.add(new WildcardFileFilter(filePattern));
    File dir = resolvePath(baseDir, dirPath);
    File[] files = dir.listFiles((FileFilter) new AndFileFilter(filters));
    if (files == null) {
      files = new File[0];
    }
    return files;
  }

  protected static File resolvePath(File baseDir, String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(baseDir, path).getCanonicalFile();
      } catch (IOException e) {
        throw new IllegalStateException("Unable to resolve path \"" + path + "\"", e);
      }
    }
    return file;
  }

  /**
   * Transforms a comma-separated list String property in to a array of trimmed strings.
   *
   * This works even if they are separated by whitespace characters (space char, EOL, ...)
   *
   */
  static String[] getListFromProperty(Properties properties, String key) {
    return StringUtils.stripAll(StringUtils.split(properties.getProperty(key, ""), ','));
  }

}
