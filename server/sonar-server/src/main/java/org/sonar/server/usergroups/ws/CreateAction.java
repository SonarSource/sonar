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
package org.sonar.server.usergroups.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import static org.sonar.core.persistence.MyBatis.closeQuietly;

public class CreateAction extends AbstractGroupUpdate implements UserGroupsWsAction {

  public CreateAction(DbClient dbClient, UserSession userSession) {
    super(dbClient, userSession);
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("create")
      .setDescription("Create a group.")
      .setHandler(this)
      .setPost(true)
      .setResponseExample(getClass().getResource("example-create.json"))
      .setSince("5.2");

    action.createParam(PARAM_NAME)
      .setDescription(String.format("Name for the new group. A group name cannot be larger than %d characters and must be unique. " +
        "The value 'anyone' (whatever the case) is reserved and cannot be used.", NAME_MAX_LENGTH))
      .setExampleValue("sonar-users")
      .setRequired(true);

    action.createParam(PARAM_DESCRIPTION)
      .setDescription(String.format("Description for the new group. A group description cannot be larger than %d characters.", DESCRIPTION_MAX_LENGTH))
      .setExampleValue("Default group for new users");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    String name = request.mandatoryParam(PARAM_NAME);
    String description = request.param(PARAM_DESCRIPTION);

    validateName(name);
    if (description != null) {
      validateDescription(description);
    }

    DbSession session = dbClient.openSession(false);
    try {
      GroupDto newGroup = new GroupDto().setName(name).setDescription(description);
      checkNameIsUnique(name, session);
      newGroup = dbClient.groupDao().insert(session, new GroupDto().setName(name).setDescription(description));
      session.commit();

      JsonWriter json = response.newJsonWriter().beginObject();
      writeGroup(json, newGroup, 0);
      json.endObject().close();
    } finally {
      closeQuietly(session);
    }
  }
}
