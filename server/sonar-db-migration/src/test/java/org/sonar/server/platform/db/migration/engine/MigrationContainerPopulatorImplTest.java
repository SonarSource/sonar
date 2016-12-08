/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.engine;

import java.sql.SQLException;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutorImpl;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MigrationContainerPopulatorImplTest {
  private MigrationContainer migrationContainer = new SimpleMigrationContainer();
  private MigrationSteps migrationSteps = mock(MigrationSteps.class);
  private MigrationContainerPopulatorImpl underTest = new MigrationContainerPopulatorImpl();

  @Before
  public void setUp() throws Exception {
    migrationContainer.add(migrationSteps);
  }

  @Test
  public void populateContainer_adds_MigrationStepsExecutorImpl() {
    when(migrationSteps.readAll()).thenReturn(Stream.empty());

    // add MigrationStepsExecutorImpl's dependencies
    migrationContainer.add(mock(MigrationHistory.class));

    underTest.populateContainer(migrationContainer);

    assertThat(migrationContainer.getComponentByType(MigrationStepsExecutorImpl.class)).isNotNull();
  }

  @Test
  public void populateContainer_adds_classes_of_all_steps_defined_in_MigrationSteps() {
    when(migrationSteps.readAll()).thenReturn(Stream.of(
        new RegisteredMigrationStep(1, "foo", MigrationStep1.class),
        new RegisteredMigrationStep(2, "bar", MigrationStep2.class),
        new RegisteredMigrationStep(3, "dor", MigrationStep3.class)
    ));

    underTest.populateContainer(migrationContainer);

    assertThat(migrationContainer.getComponentsByType(MigrationStep1.class)).isNotNull();
    assertThat(migrationContainer.getComponentsByType(MigrationStep2.class)).isNotNull();
    assertThat(migrationContainer.getComponentsByType(MigrationStep3.class)).isNotNull();
  }

  private static abstract class NoopMigrationStep implements MigrationStep {
    @Override
    public void execute() throws SQLException {
      throw new UnsupportedOperationException("execute not implemented");
    }
  }

  public static final class MigrationStep1 extends NoopMigrationStep {

  }

  public static final class MigrationStep2 extends NoopMigrationStep {

  }

  public static final class MigrationStep3 extends NoopMigrationStep {

  }

}
