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
package org.sonar.server.platform;

import org.sonar.api.CoreProperties;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

/**
 * The server node marked as "startup leader" generates some information about startup. These
 * information are loaded by "startup follower" servers and all Compute Engine nodes.
 *
 * @see StartupMetadataProvider#load(DbClient)
 */
@ServerSide
public class StartupMetadataPersister implements Startable {

  private final StartupMetadata metadata;
  private final DbClient dbClient;

  public StartupMetadataPersister(StartupMetadata metadata, DbClient dbClient) {
    this.metadata = metadata;
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PropertiesDao dao = dbClient.propertiesDao();
      dao.insertProperty(dbSession, new PropertyDto().setKey(CoreProperties.SERVER_ID).setValue(metadata.getStartupId()));
      dao.insertProperty(dbSession, new PropertyDto().setKey(StartupMetadataProvider.PROPERTY_STARTED_AT).setValue(String.valueOf(metadata.getStartedAt())));
      dbSession.commit();
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
