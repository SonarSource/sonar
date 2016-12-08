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
package org.sonar.server.platform.platformlevel;

import org.sonar.server.organization.NoopDefaultOrganizationCache;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.db.migration.DatabaseMigrationImpl;
import org.sonar.server.platform.db.migration.MigrationEngineModule;
import org.sonar.server.platform.db.migration.version.DbVersionModule;
import org.sonar.server.platform.ws.DbMigrationStatusAction;
import org.sonar.server.platform.ws.MigrateDbAction;
import org.sonar.server.platform.ws.StatusAction;
import org.sonar.server.platform.ws.SystemWs;
import org.sonar.server.ws.WebServiceEngine;
import org.sonar.server.ws.WebServiceFilter;
import org.sonar.server.ws.WebServicesWs;

public class PlatformLevelSafeMode extends PlatformLevel {
  public PlatformLevelSafeMode(PlatformLevel parent) {
    super("Safemode", parent);
  }

  @Override
  protected void configureLevel() {
    add(
      ServerImpl.class,
      // Server WS
      StatusAction.class,
      MigrateDbAction.class,
      DbMigrationStatusAction.class,
      SystemWs.class,

      // Listing WS
      WebServicesWs.class,

      // WS engine
      WebServiceEngine.class,
      WebServiceFilter.class,

      NoopDefaultOrganizationCache.class);
    add(DatabaseMigrationImpl.class);
    add(DbVersionModule.class);
    addIfStartupLeader(MigrationEngineModule.class);
  }
}
