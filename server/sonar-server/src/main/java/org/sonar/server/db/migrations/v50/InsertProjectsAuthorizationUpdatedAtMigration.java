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

package org.sonar.server.db.migrations.v50;

import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

import java.sql.SQLException;
import java.util.Date;

/**
 * Used in the Active Record Migration 701
 *
 * @since 5.0
 */
public class InsertProjectsAuthorizationUpdatedAtMigration extends BaseDataChange {

  private final System2 system;

  public InsertProjectsAuthorizationUpdatedAtMigration(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final Date now = new Date(system.now());

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT p.id FROM projects p WHERE p.scope=? AND p.enabled=?").setString(1, "PRJ").setBoolean(2, true);
    massUpdate.update("UPDATE projects SET authorization_updated_at=? WHERE id=?");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long id = row.getLong(1);
        update.setDate(1, now);
        update.setLong(2, id);
        return true;
      }
    });
  }

}
