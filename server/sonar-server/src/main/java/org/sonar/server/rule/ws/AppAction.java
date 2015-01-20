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
package org.sonar.server.rule.ws;

import com.google.common.collect.Maps;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.RuleRepositories.Repository;
import org.sonar.server.user.UserSession;

import java.util.Locale;
import java.util.Map;

/**
 * @since 4.4
 */
public class AppAction implements RulesAction {

  private final Languages languages;
  private final RuleRepositories ruleRepositories;
  private final I18n i18n;
  private final DebtModel debtModel;
  private final QProfileLoader profileLoader;

  public AppAction(Languages languages, RuleRepositories ruleRepositories, I18n i18n,
    DebtModel debtModel, QProfileLoader profileLoader) {
    this.languages = languages;
    this.ruleRepositories = ruleRepositories;
    this.i18n = i18n;
    this.debtModel = debtModel;
    this.profileLoader = profileLoader;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    addPermissions(json);
    addProfiles(json);
    addLanguages(json);
    addRuleRepositories(json);
    addStatuses(json);
    addCharacteristics(json);
    json.endObject().close();
  }

  private void addPermissions(JsonWriter json) {
    json.prop("canWrite", UserSession.get().hasGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN));
  }

  private void addProfiles(JsonWriter json) {
    json.name("qualityprofiles").beginArray();
    for (QualityProfileDto profile : profileLoader.findAll()) {
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

  private void addRuleRepositories(JsonWriter json) {
    json.name("repositories").beginArray();
    for (Repository repo : ruleRepositories.repositories()) {
      json.beginObject()
        .prop("key", repo.key())
        .prop("name", repo.name())
        .prop("language", repo.language())
        .endObject();
    }
    json.endArray();
  }

  private void addStatuses(JsonWriter json) {
    json.name("statuses").beginObject();
    for (RuleStatus status : RuleStatus.values()) {
      if (status != RuleStatus.REMOVED) {
        json.prop(status.toString(), i18n.message(Locale.getDefault(), "rules.status." + status.toString().toLowerCase(), status.toString()));
      }
    }
    json.endObject();
  }

  private void addCharacteristics(JsonWriter json) {
    Map<Integer, DefaultDebtCharacteristic> caracById = Maps.newHashMap();
    for (DebtCharacteristic carac : debtModel.allCharacteristics()) {
      DefaultDebtCharacteristic fullCarac = (DefaultDebtCharacteristic) carac;
      caracById.put(fullCarac.id(), fullCarac);
    }
    json.name("characteristics").beginArray();
    for (DefaultDebtCharacteristic carac : caracById.values()) {
      json.beginObject()
        .prop("key", carac.key())
        .prop("name", carac.name());
      if (carac.isSub()) {
        json.prop("parent", caracById.get(carac.parentId()).key());
      }
      json.endObject();
    }
    json.endArray();
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("app")
      .setDescription("Data required for rendering the page 'Coding Rules'")
      .setInternal(true)
      .setHandler(this);
  }
}
