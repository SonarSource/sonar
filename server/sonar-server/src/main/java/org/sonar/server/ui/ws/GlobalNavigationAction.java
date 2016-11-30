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
package org.sonar.server.ui.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.db.Database;
import org.sonar.db.dialect.H2;
import org.sonar.server.ui.ViewProxy;
import org.sonar.server.ui.Views;

import static org.sonar.api.CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY;
import static org.sonar.api.CoreProperties.HOURS_IN_DAY;
import static org.sonar.api.CoreProperties.RATING_GRID;
import static org.sonar.core.config.WebConstants.SONAR_LF_ENABLE_GRAVATAR;
import static org.sonar.core.config.WebConstants.SONAR_LF_GRAVATAR_SERVER_URL;
import static org.sonar.core.config.WebConstants.SONAR_UPDATECENTER_ACTIVATE;

public class GlobalNavigationAction implements NavigationWsAction {

  private static final Set<String> SETTING_KEYS = ImmutableSet.of(
    SONAR_LF_ENABLE_GRAVATAR,
    SONAR_LF_GRAVATAR_SERVER_URL,
    SONAR_UPDATECENTER_ACTIVATE,
    HOURS_IN_DAY,
    RATING_GRID,
    CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY);

  private final Views views;
  private final Settings settings;
  private final ResourceTypes resourceTypes;
  private final Server server;
  private final Database database;

  public GlobalNavigationAction(Views views, Settings settings, ResourceTypes resourceTypes, Server server, Database database) {
    this.views = views;
    this.settings = settings;
    this.resourceTypes = resourceTypes;
    this.server = server;
    this.database = database;
  }

  @Override
  public void define(NewController context) {
    context.createAction("global")
      .setDescription("Get information concerning global navigation for the current user.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("example-global.json"))
      .setSince("5.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter json = response.newJsonWriter().beginObject();
    writePages(json);
    writeSettings(json);
    writeQualifiers(json);
    json.prop("version", server.getVersion());
    json.prop("productionDatabase", !database.getDialect().getId().equals(H2.ID));
    json.endObject().close();
  }

  private void writePages(JsonWriter json) {
    json.name("globalPages").beginArray();
    for (ViewProxy<Page> page : views.getPages(NavigationSection.HOME)) {
      if (page.isUserAuthorized()) {
        json.beginObject()
          .prop("name", page.getTitle())
          .prop("url", page.isController() ? page.getId() : String.format("/plugins/home/%s", page.getId()))
          .endObject();
      }
    }
    json.endArray();
  }

  private void writeSettings(JsonWriter json) {
    json.name("settings").beginObject();
    for (String settingKey : SETTING_KEYS) {
      json.prop(settingKey, settings.getString(settingKey));
    }
    json.endObject();
  }

  private void writeQualifiers(JsonWriter json) {
    json.name("qualifiers").beginArray();
    for (ResourceType rootType : resourceTypes.getRoots()) {
      json.value(rootType.getQualifier());
    }
    json.endArray();
  }
}
