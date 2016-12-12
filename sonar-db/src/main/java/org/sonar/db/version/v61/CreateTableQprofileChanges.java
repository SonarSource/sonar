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
package org.sonar.db.version.v61;

import java.sql.SQLException;
import java.util.List;
import org.sonar.db.Database;
import org.sonar.db.version.CreateTableBuilder;
import org.sonar.db.version.DdlChange;

import static org.sonar.db.version.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.db.version.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.db.version.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableQprofileChanges extends DdlChange {
  public CreateTableQprofileChanges(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    List<String> stmts = new CreateTableBuilder(getDialect(), "qprofile_changes")
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(40).setIsNullable(false).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("qprofile_key").setLimit(255).setIsNullable(false).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("change_type").setLimit(20).setIsNullable(false).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("user_login").setLimit(255).setIsNullable(true).setIgnoreOracleUnit(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("change_data").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build();
    context.execute(stmts);
  }
}
