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
package org.sonar.application;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.MessageException;
import org.sonar.process.Props;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class JdbcSettingsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  JdbcSettings settings = new JdbcSettings();

  @Test
  public void driver_provider() throws Exception {
    assertThat(settings.driverProvider("jdbc:oracle:thin:@localhost/XE")).isEqualTo(JdbcSettings.Provider.oracle);
    assertThat(settings.driverProvider("jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance"))
      .isEqualTo(JdbcSettings.Provider.mysql);
    try {
      settings.driverProvider("jdbc:sqlserver://localhost");
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Unsupported JDBC driver provider: sqlserver. Accepted values are [h2, jtds, mysql, oracle, postgresql]");
    }
    try {
      settings.driverProvider("oracle:thin:@localhost/XE");
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Bad format of JDBC URL: oracle:thin:@localhost/XE");
    }
  }

  @Test
  public void check_mysql_parameters() throws Exception {
    Props props = new Props(new Properties());

    // minimal -> ok
    settings.checkUrlParameters(JdbcSettings.Provider.mysql,
      "jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8");

    // full -> ok
    settings.checkUrlParameters(JdbcSettings.Provider.mysql,
      "jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance");

    // missing required -> ko
    try {
      settings.checkUrlParameters(JdbcSettings.Provider.mysql,
        "jdbc:mysql://localhost:3306/sonar?characterEncoding=utf8");
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("JDBC URL must have the property 'useUnicode=true'");
    }
  }

  @Test
  public void check_oracle() throws Exception {
    File home = temp.newFolder();
    File driverFile = new File(home, "extensions/jdbc-driver/oracle/ojdbc6.jar");
    FileUtils.touch(driverFile);

    Props props = new Props(new Properties());
    props.set("sonar.jdbc.url", "jdbc:oracle:thin:@localhost/XE");
    settings.checkAndComplete(home, props);
    assertThat(props.nonNullValueAsFile(JdbcSettings.PROPERTY_DRIVER_PATH)).isEqualTo(driverFile);
  }

  @Test
  public void check_h2() throws Exception {
    File home = temp.newFolder();
    Props props = new Props(new Properties());
    props.set("sonar.jdbc.url", "jdbc:h2:tcp://localhost:9092/sonar");
    settings.checkAndComplete(home, props);
    assertThat(props.value(JdbcSettings.PROPERTY_DRIVER_PATH)).isNull();
  }

  @Test
  public void check_postgresql() throws Exception {
    File home = temp.newFolder();
    File driverFile = new File(home, "lib/jdbc/postgresql/pg.jar");
    FileUtils.touch(driverFile);

    Props props = new Props(new Properties());
    props.set("sonar.jdbc.url", "jdbc:postgresql://localhost/sonar");
    settings.checkAndComplete(home, props);
    assertThat(props.nonNullValueAsFile(JdbcSettings.PROPERTY_DRIVER_PATH)).isEqualTo(driverFile);
  }

  @Test
  public void check_mssql() throws Exception {
    File home = temp.newFolder();
    File driverFile = new File(home, "lib/jdbc/jtds/jtds.jar");
    FileUtils.touch(driverFile);

    Props props = new Props(new Properties());
    props.set("sonar.jdbc.url", "jdbc:jtds:sqlserver://localhost/sonar;SelectMethod=Cursor");
    settings.checkAndComplete(home, props);
    assertThat(props.nonNullValueAsFile(JdbcSettings.PROPERTY_DRIVER_PATH)).isEqualTo(driverFile);
  }

  @Test
  public void driver_file() throws Exception {
    File home = temp.newFolder();
    File driverFile = new File(home, "extensions/jdbc-driver/oracle/ojdbc6.jar");
    FileUtils.touch(driverFile);

    String path = settings.driverPath(home, JdbcSettings.Provider.oracle);
    assertThat(path).isEqualTo(driverFile.getAbsolutePath());
  }

  @Test
  public void driver_dir_does_not_exist() throws Exception {
    File home = temp.newFolder();
    try {
      settings.driverPath(home, JdbcSettings.Provider.oracle);
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Directory does not exist: extensions/jdbc-driver/oracle");
    }
  }

  @Test
  public void no_files_in_driver_dir() throws Exception {
    File home = temp.newFolder();
    FileUtils.forceMkdir(new File(home, "extensions/jdbc-driver/oracle"));
    try {
      settings.driverPath(home, JdbcSettings.Provider.oracle);
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Directory does not contain JDBC driver: extensions/jdbc-driver/oracle");
    }
  }

  @Test
  public void too_many_files_in_driver_dir() throws Exception {
    File home = temp.newFolder();
    FileUtils.touch(new File(home, "extensions/jdbc-driver/oracle/ojdbc5.jar"));
    FileUtils.touch(new File(home, "extensions/jdbc-driver/oracle/ojdbc6.jar"));

    try {
      settings.driverPath(home, JdbcSettings.Provider.oracle);
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Directory must contain only one JAR file: extensions/jdbc-driver/oracle");
    }
  }
}
