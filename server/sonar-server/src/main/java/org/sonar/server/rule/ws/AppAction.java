/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.rule.ws;

import java.util.Locale;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.user.UserSession;

import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;

public class AppAction implements RulesWsAction {

  private final Languages languages;
  private final DbClient dbClient;
  private final I18n i18n;
  private final UserSession userSession;
  private final RuleWsSupport wsSupport;

  public AppAction(Languages languages, DbClient dbClient, I18n i18n, UserSession userSession, RuleWsSupport wsSupport) {
    this.languages = languages;
    this.dbClient = dbClient;
    this.i18n = i18n;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("app")
      .setDescription("Get data required for rendering the page 'Coding Rules'.")
      .setResponseExample(getClass().getResource("app-example.json"))
      .setSince("4.5")
      .setInternal(true)
      .setHandler(this);

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setSince("6.4")
      .setExampleValue("my-org");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganizationByKey(dbSession, request.param(PARAM_ORGANIZATION));

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      addPermissions(organization, json);
      addProfiles(dbSession, organization, json);
      addLanguages(json);
      addRuleRepositories(json, dbSession);
      addStatuses(json);
      json.endObject().close();
    }
  }

  private void addPermissions(OrganizationDto organization, JsonWriter json) {
    boolean canWrite = userSession.hasPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);
    json.prop("canWrite", canWrite);
  }

  private void addProfiles(DbSession dbSession, OrganizationDto organization, JsonWriter json) {
    json.name("qualityprofiles").beginArray();
    for (QualityProfileDto profile : dbClient.qualityProfileDao().selectAll(dbSession, organization)) {
      if (languageIsSupported(profile)) {
        json
          .beginObject()
          .prop("key", profile.getKey())
          .prop("name", profile.getName())
          .prop("lang", profile.getLanguage())
          .prop("parentKey", profile.getParentKee())
          .endObject();
      }
    }
    json.endArray();
  }

  private boolean languageIsSupported(QualityProfileDto profile) {
    return languages.get(profile.getLanguage()) != null;
  }

  private void addLanguages(JsonWriter json) {
    json.name("languages").beginObject();
    for (Language language : languages.all()) {
      json.prop(language.getKey(), language.getName());
    }
    json.endObject();
  }

  private void addRuleRepositories(JsonWriter json, DbSession dbSession) {
    json.name("repositories").beginArray();
    dbClient.ruleRepositoryDao()
      .selectAll(dbSession)
      .forEach(r -> json.beginObject()
        .prop("key", r.getKey())
        .prop("name", r.getName())
        .prop("language", r.getLanguage())
        .endObject());
    json.endArray();
  }

  private void addStatuses(JsonWriter json) {
    json.name("statuses").beginObject();
    for (RuleStatus status : RuleStatus.values()) {
      if (status != RuleStatus.REMOVED) {
        json.prop(status.toString(), i18n.message(Locale.getDefault(), "rules.status." + status.toString().toLowerCase(Locale.ENGLISH), status.toString()));
      }
    }
    json.endObject();
  }
}
