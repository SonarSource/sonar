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
package org.sonar.server.db.migrations.v453;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.server.db.migrations.DatabaseMigration;

import static junit.framework.TestCase.fail;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddCharacteristicUsabilityAndSubCharacteristicsComplianceMigrationTest {

  @ClassRule
  public static TestDatabase db = new TestDatabase().schema(AddCharacteristicUsabilityAndSubCharacteristicsComplianceMigrationTest.class, "schema.sql");

  DatabaseMigration migration;

  System2 system = mock(System2.class);

  @Before
  public void setUp() throws Exception {
    db.executeUpdateSql("truncate table characteristics");

    when(system.now()).thenReturn(DateUtils.parseDate("2015-02-15").getTime());

    migration = new AddCharacteristicUsabilityAndSubCharacteristicsComplianceMigration(db.database(), system);
  }

  @Test
  public void migrate() throws Exception {
    db.prepareDbUnit(getClass(), "migrate.xml");
    migration.execute();
    db.assertDbUnit(getClass(), "migrate-result.xml", "characteristics");
  }

  @Test
  public void do_nothing_when_already_migrated() throws Exception {
    db.prepareDbUnit(getClass(), "do_nothing_when_already_migrated.xml");
    migration.execute();
    db.assertDbUnit(getClass(), "do_nothing_when_already_migrated.xml", "characteristics");
  }

  @Test
  public void insert_usability_at_the_top_if_security_does_exists() throws Exception {
    db.prepareDbUnit(getClass(), "insert_usability_at_the_top_if_security_does_exists.xml");
    migration.execute();
    db.assertDbUnit(getClass(), "insert_usability_at_the_top_if_security_does_exists-result.xml", "characteristics");
  }

  @Test
  public void update_usability_order_if_already_exists() throws Exception {
    db.prepareDbUnit(getClass(), "update_usability_if_already_exists.xml");
    migration.execute();
    db.assertDbUnit(getClass(), "update_usability_if_already_exists-result.xml", "characteristics");
  }

  @Test
  public void fail_if_usability_exists_as_sub_characteristic() throws Exception {
    db.prepareDbUnit(getClass(), "fail_if_usability_exists_as_sub_characteristic.xml");

   try {
     migration.execute();
     fail();
   } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("'Usability' must be a characteristic");
    }
  }

  @Test
  public void fail_if_compliance_already_exists_as_characteristic() throws Exception {
    db.prepareDbUnit(getClass(), "fail_if_compliance_already_exists_as_characteristic.xml");

    try {
      migration.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("'Compliance' must be a sub-characteristic");
    }
  }

  @Test
  public void fail_if_compliance_already_exists_under_wrong_characteristic() throws Exception {
    db.prepareDbUnit(getClass(), "fail_if_compliance_already_exists_under_wrong_characteristic.xml");

    try {
      migration.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("'Reusability Compliance' must be defined under 'Reusability'");
    }
  }

}
