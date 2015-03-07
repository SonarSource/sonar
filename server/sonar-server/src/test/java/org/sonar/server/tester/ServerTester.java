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
package org.sonar.server.tester;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.rules.ExternalResource;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessConstants;
import org.sonar.server.es.EsServerHolder;
import org.sonar.server.platform.BackendCleanup;
import org.sonar.server.platform.Platform;
import org.sonar.server.ws.WsTester;
import org.sonar.test.TestUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Entry point to implement medium tests of server components.
 * <p/>
 * The system properties starting with "mediumTests." override the programmatic settings, for example:
 * <code>-DmediumTests.sonar.log.level=TRACE</code>
 *
 * @since 4.4
 */
public class ServerTester extends ExternalResource {

  private static final Logger LOG = Loggers.get(ServerTester.class);
  private static final String PROP_PREFIX = "mediumTests.";

  private Platform platform;
  private EsServerHolder esServerHolder;
  private final File homeDir = TestUtils.newTempDir("tmp-sq-");
  private final List components = Lists.newArrayList(WsTester.class);
  private final Properties initialProps = new Properties();

  /**
   * Called only when JUnit @Rule or @ClassRule is used.
   */
  @Override
  protected void before() {
    start();
  }

  /**
   * This method should not be called by test when ServerTester is annotated with {@link org.junit.Rule}
   */
  public void start() {
    checkNotStarted();

    try {
      Properties properties = new Properties();
      properties.putAll(initialProps);
      esServerHolder = EsServerHolder.get();
      properties.setProperty(ProcessConstants.CLUSTER_NAME, esServerHolder.getClusterName());
      properties.setProperty(ProcessConstants.CLUSTER_NODE_NAME, esServerHolder.getNodeName());
      properties.setProperty(ProcessConstants.SEARCH_PORT, String.valueOf(esServerHolder.getPort()));
      properties.setProperty(ProcessConstants.WEB_HOST, String.valueOf(esServerHolder.getHostName()));
      properties.setProperty(ProcessConstants.PATH_HOME, homeDir.getAbsolutePath());
      properties.setProperty(DatabaseProperties.PROP_URL, "jdbc:h2:" + homeDir.getAbsolutePath() + "/h2");
      for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
        String key = entry.getKey().toString();
        if (key.startsWith(PROP_PREFIX)) {
          properties.put(StringUtils.substringAfter(key, PROP_PREFIX), entry.getValue());
        }
      }
      platform = new Platform();
      platform.init(properties);
      platform.addComponents(components);
      platform.doStart();
    } catch (Exception e) {
      stop();
      Throwables.propagate(e);
    }
    if (!platform.isStarted()) {
      throw new IllegalStateException("Server not started. You should check that db migrations " +
        "are correctly declared, for example in schema-h2.sql or DatabaseVersion");
    }
  }

  /**
   * Called only when JUnit @Rule or @ClassRule is used.
   */
  @Override
  protected void after() {
    stop();
  }

  /**
   * This method should not be called by test when ServerTester is annotated with {@link org.junit.Rule}
   */
  public void stop() {
    try {
      if (platform != null) {
        platform.doStop();
        platform = null;
      }
    } catch (Exception e) {
      LOG.error("Fail to stop web server", e);
    }
    esServerHolder = null;
    FileUtils.deleteQuietly(homeDir);
  }

  /**
   * Add classes or objects to IoC container, as it could be done by plugins.
   * Must be called before {@link #start()}.
   */
  public ServerTester addComponents(@Nullable Object... components) {
    checkNotStarted();
    if (components != null) {
      this.components.addAll(Arrays.asList(components));
    }
    return this;
  }

  public ServerTester addXoo() {
    addComponents(Xoo.class);
    return this;
  }

  public ServerTester addPluginJar(File jar) {
    Preconditions.checkArgument(jar.exists() && jar.isFile(), "Plugin JAR file does not exist: " + jar.getAbsolutePath());
    try {
      File pluginsDir = new File(homeDir, "extensions/plugins");
      FileUtils.forceMkdir(pluginsDir);
      FileUtils.copyFileToDirectory(jar, pluginsDir);
      return this;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to copy plugin JAR file: " + jar.getAbsolutePath(), e);
    }
  }

  /**
   * Set a property available for startup. Must be called before {@link #start()}. Does not affect
   * Elasticsearch server.
   */
  public ServerTester setProperty(String key, String value) {
    checkNotStarted();
    initialProps.setProperty(key, value);
    return this;
  }

  /**
   * Truncate all db tables and Elasticsearch indexes. Can be executed only if ServerTester is started.
   */
  public void clearDbAndIndexes() {
    checkStarted();
    get(BackendCleanup.class).clearAll();
  }

  public void clearIndexes() {
    checkStarted();
    get(BackendCleanup.class).clearIndexes();
  }

  /**
   * Get a component from the platform
   */
  public <C> C get(Class<C> component) {
    checkStarted();
    return platform.getContainer().getComponentByType(component);
  }

  public WsTester wsTester() {
    return get(WsTester.class);
  }

  public EsServerHolder getEsServerHolder() {
    return esServerHolder;
  }

  private void checkStarted() {
    if (platform == null || !platform.isStarted()) {
      throw new IllegalStateException("Not started");
    }
  }

  private void checkNotStarted() {
    if (platform != null && platform.isStarted()) {
      throw new IllegalStateException("Already started");
    }
  }

  public static class Xoo implements Language {

    public static final String KEY = "xoo";
    public static final String NAME = "Xoo";
    public static final String FILE_SUFFIX = ".xoo";

    private static final String[] XOO_SUFFIXES = {
      FILE_SUFFIX
    };

    @Override
    public String getKey() {
      return KEY;
    }

    @Override
    public String getName() {
      return NAME;
    }

    @Override
    public String[] getFileSuffixes() {
      return XOO_SUFFIXES;
    }
  }
}
