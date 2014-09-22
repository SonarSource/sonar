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
package org.sonar.server.db;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.core.persistence.dialect.H2;
import org.sonar.core.persistence.dialect.MySql;
import org.sonar.core.persistence.dialect.Oracle;

import java.sql.SQLException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseCheckerTest {

  @Test
  public void require_oracle_11_2() throws Exception {
    Database db = mockDb(new Oracle(), "11.2.0.0.1");
    new DatabaseChecker(db).start();
    // no error

    db = mockDb(new Oracle(), "11.3.1");
    new DatabaseChecker(db).start();
    // no error

    db = mockDb(new Oracle(), "12.0.2");
    new DatabaseChecker(db).start();
    // no error

    db = mockDb(new Oracle(), "11.1.0.2");
    try {
      new DatabaseChecker(db).start();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Unsupported Oracle JDBC driver version: 11.1.0.2. Minimal required version is 11.2.");
    }
  }

  @Test
  public void log_warning_if_h2() throws Exception {
    Database db = mockDb(new H2(), "13.4");
    DatabaseChecker checker = new DatabaseChecker(db);
    checker.start();
    checker.stop();
    // TODO test log
  }

  @Test
  public void do_not_fail_if_mysql() throws Exception {
    Database db = mockDb(new MySql(), "5.7");
    new DatabaseChecker(db).start();
    // no error
  }

  private Database mockDb(Dialect dialect, String driverVersion) throws SQLException {
    Database db = mock(Database.class, Mockito.RETURNS_DEEP_STUBS);
    when(db.getDialect()).thenReturn(dialect);
    when(db.getDataSource().getConnection().getMetaData().getDriverVersion()).thenReturn(driverVersion);
    return db;
  }
}
