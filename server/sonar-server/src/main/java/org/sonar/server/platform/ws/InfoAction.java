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
package org.sonar.server.platform.ws;

import com.google.common.base.Optional;
import java.util.Map;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.platform.monitoring.Monitor;
import org.sonar.server.user.UserSession;

/**
 * Implementation of the {@code info} action for the System WebService.
 */
public class InfoAction implements SystemWsAction {

  private final Monitor[] monitors;
  private final UserSession userSession;

  public InfoAction(UserSession userSession, Monitor... monitors) {
    this.userSession = userSession;
    this.monitors = monitors;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("info")
      .setDescription("Detailed information about system configuration." +
        "<br/>" +
        "Requires user to be authenticated with Administer System permissions.")
      .setSince("5.1")
      .setResponseExample(getClass().getResource("/org/sonar/server/platform/ws/example-system-info.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);
    JsonWriter json = response.newJsonWriter();
    writeJson(json);
    json.close();
  }

  private void writeJson(JsonWriter json) {
    json.beginObject();
    for (Monitor monitor : monitors) {
      Optional<Map<String, Object>> attributes = monitor.attributes();
      if (attributes.isPresent()) {
        json.name(monitor.name());
        json.beginObject();
        for (Map.Entry<String, Object> attribute : attributes.get().entrySet()) {
          json.name(attribute.getKey()).valueObject(attribute.getValue());
        }
        json.endObject();
      }
    }
    json.endObject();
  }
}
