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
package org.sonar.server.issue.ws;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import org.sonar.api.component.Component;
import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.user.User;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.debt.DebtModelService;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueChangelog;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class IssueShowAction implements RequestHandler {

  public static final String SHOW_ACTION = "show";

  private final IssueFinder issueFinder;
  private final IssueChangelogService issueChangelogService;
  private final IssueActionsWriter actionsWriter;
  private final DebtModelService debtModel;
  private final I18n i18n;
  private final Durations durations;

  public IssueShowAction(IssueFinder issueFinder, IssueChangelogService issueChangelogService, IssueActionsWriter actionsWriter,
    DebtModelService debtModel, I18n i18n, Durations durations) {
    this.issueFinder = issueFinder;
    this.issueChangelogService = issueChangelogService;
    this.actionsWriter = actionsWriter;
    this.debtModel = debtModel;
    this.i18n = i18n;
    this.durations = durations;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(SHOW_ACTION)
      .setDescription("Detail of issue")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-show.json"));
    action.createParam("key")
      .setDescription("Issue key")
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
  }

  @Override
  public void handle(Request request, Response response) {
    String issueKey = request.mandatoryParam("key");
    IssueQueryResult queryResult = issueFinder.find(IssueQuery.builder()
      .requiredRole(UserRole.USER)
      .issueKeys(Arrays.asList(issueKey)).build());
    if (queryResult.issues().size() != 1) {
      throw new NotFoundException("Issue not found: " + issueKey);
    }
    DefaultIssue issue = (DefaultIssue) queryResult.first();

    JsonWriter json = response.newJsonWriter();
    json.beginObject().name("issue").beginObject();

    writeIssue(queryResult, issue, json);
    actionsWriter.writeActions(issue, json);
    actionsWriter.writeTransitions(issue, json);
    writeComments(queryResult, issue, json);
    writeChangelog(issue, json);

    json.endObject().endObject().close();
  }

  private void writeIssue(IssueQueryResult result, DefaultIssue issue, JsonWriter json) {
    String actionPlanKey = issue.actionPlanKey();
    ActionPlan actionPlan = result.actionPlan(issue);
    Duration debt = issue.debt();
    Date updateDate = issue.updateDate();
    Date closeDate = issue.closeDate();

    json
      .prop("key", issue.key())
      .prop("rule", issue.ruleKey().toString())
      .prop("ruleName", result.rule(issue).getName())
      .prop("line", issue.line())
      .prop("message", issue.message())
      .prop("resolution", issue.resolution())
      .prop("status", issue.status())
      .prop("severity", issue.severity())
      .prop("author", issue.authorLogin())
      .prop("actionPlan", actionPlanKey)
      .prop("actionPlanName", actionPlan != null ? actionPlan.name() : null)
      .prop("debt", debt != null ? durations.encode(debt) : null)
      .prop("creationDate", DateUtils.formatDateTime(issue.creationDate()))
      .prop("fCreationDate", formatDate(issue.creationDate()))
      .prop("updateDate", updateDate != null ? DateUtils.formatDateTime(updateDate) : null)
      .prop("fUpdateDate", formatDate(updateDate))
      .prop("fUpdateAge", formatAgeDate(updateDate))
      .prop("closeDate", closeDate != null ? DateUtils.formatDateTime(closeDate) : null)
      .prop("fCloseDate", formatDate(issue.closeDate()));

    addComponents(result, issue, json);
    addUserWithLabel(result, issue.assignee(), "assignee", json);
    addUserWithLabel(result, issue.reporter(), "reporter", json);
    addCharacteristics(result, issue, json);
  }

  private void addComponents(IssueQueryResult result, DefaultIssue issue, JsonWriter json) {
    // component, module and project can be null if they were removed
    ComponentDto component = (ComponentDto) result.component(issue);
    ComponentDto subProject = (ComponentDto) getSubProject(result, component);
    ComponentDto project = (ComponentDto) geProject(result, component);

    String projectName = project != null ? project.longName() != null ? project.longName() : project.name() : null;
    // Do not display sub project long name if sub project and project are the same
    boolean displaySubProjectLongName = subProject != null && project != null && !subProject.getId().equals(project.getId());
    String subProjectName = displaySubProjectLongName ? subProject.longName() != null ? subProject.longName() : subProject.name() : null;

    json
      .prop("component", issue.componentKey())
      .prop("componentLongName", component != null ? component.longName() : null)
      .prop("componentQualifier", component != null ? component.qualifier() : null)
      .prop("project", issue.projectKey())
      .prop("projectName", projectName)
      .prop("subProjectName", subProjectName);
  }

  private void writeComments(IssueQueryResult queryResult, Issue issue, JsonWriter json) {
    json.name("comments").beginArray();
    String login = UserSession.get().login();
    for (IssueComment comment : issue.comments()) {
      String userLogin = comment.userLogin();
      User user = userLogin != null ? queryResult.user(userLogin) : null;
      json
        .beginObject()
        .prop("key", comment.key())
        .prop("userName", user != null ? user.name() : null)
        .prop("raw", comment.markdownText())
        .prop("html", Markdown.convertToHtml(comment.markdownText()))
        .prop("createdAt", DateUtils.formatDateTime(comment.createdAt()))
        .prop("fCreatedAge", formatAgeDate(comment.createdAt()))
        .prop("updatable", login != null && login.equals(userLogin))
        .endObject();
    }
    json.endArray();
  }

  private void writeChangelog(Issue issue, JsonWriter json) {
    json.name("changelog").beginArray()
      .beginObject()
      .prop("creationDate", DateUtils.formatDateTime(issue.creationDate()))
      .prop("fCreationDate", formatDate(issue.creationDate()))
      .name("diffs").beginArray()
      .value(i18n.message(UserSession.get().locale(), "created", null))
      .endArray()
      .endObject();

    IssueChangelog changelog = issueChangelogService.changelog(issue);
    for (FieldDiffs diffs : changelog.changes()) {
      User user = changelog.user(diffs);
      json
        .beginObject()
        .prop("userName", user != null ? user.name() : null)
        .prop("creationDate", DateUtils.formatDateTime(diffs.creationDate()))
        .prop("fCreationDate", formatDate(diffs.creationDate()));
      json.name("diffs").beginArray();
      List<String> diffsFormatted = issueChangelogService.formatDiffs(diffs);
      for (String diff : diffsFormatted) {
        json.value(diff);
      }
      json.endArray();
      json.endObject();
    }
    json.endArray();
  }

  private static void addUserWithLabel(IssueQueryResult result, @Nullable String value, String field, JsonWriter json) {
    if (value != null) {
      User user = result.user(value);
      json
        .prop(field, value)
        .prop(field + "Name", user != null ? user.name() : null);
    }
  }

  @CheckForNull
  private String formatDate(@Nullable Date date) {
    if (date != null) {
      return i18n.formatDateTime(UserSession.get().locale(), date);
    }
    return null;
  }

  @CheckForNull
  private String formatAgeDate(@Nullable Date date) {
    if (date != null) {
      return i18n.ageFromNow(UserSession.get().locale(), date);
    }
    return null;
  }

  /**
   * Can be null on project or on removed component
   */
  @CheckForNull
  private Component getSubProject(IssueQueryResult result, @Nullable final ComponentDto component) {
    if (component != null) {
      return Iterables.find(result.components(), new Predicate<Component>() {
        @Override
        public boolean apply(Component input) {
          Long subProjectId = component.subProjectId();
          return subProjectId != null && subProjectId.equals(((ComponentDto) input).getId());
        }
      }, null);
    }
    return null;
  }

  /**
   * Can be null on removed component
   */
  @CheckForNull
  private Component geProject(IssueQueryResult result, @Nullable final ComponentDto component) {
    if (component != null) {
      return Iterables.find(result.components(), new Predicate<Component>() {
        @Override
        public boolean apply(Component input) {
          return component.projectId().equals(((ComponentDto) input).getId());
        }
      }, null);
    }
    return null;
  }

  private void addCharacteristics(IssueQueryResult result, DefaultIssue issue, JsonWriter json) {
    String subCharacteristicKey = result.rule(issue).getCharacteristicKey() != null ? result.rule(issue).getCharacteristicKey() : result.rule(issue).getDefaultCharacteristicKey();
    DebtCharacteristic subCharacteristic = characteristicByKey(subCharacteristicKey);
    if (subCharacteristic != null) {
      json.prop("subCharacteristic", subCharacteristic.name());
      DebtCharacteristic characteristic = characteristicById(((DefaultDebtCharacteristic) subCharacteristic).parentId());
      json.prop("characteristic", characteristic != null ? characteristic.name() : null);
    }
  }

  @CheckForNull
  private DebtCharacteristic characteristicById(@Nullable Integer id) {
    if (id != null) {
      return debtModel.characteristicById(id);
    }
    return null;
  }

  @CheckForNull
  private DebtCharacteristic characteristicByKey(@Nullable String key) {
    if (key != null) {
      return debtModel.characteristicByKey(key);
    }
    return null;
  }
}
