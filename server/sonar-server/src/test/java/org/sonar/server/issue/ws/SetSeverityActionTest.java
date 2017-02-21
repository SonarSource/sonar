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
package org.sonar.server.issue.ws;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.IssueUpdater;
import org.sonar.server.issue.ServerIssueStorage;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newDto;
import static org.sonar.db.rule.RuleTesting.newRuleDto;

public class SetSeverityActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create();

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private System2 system2 = mock(System2.class);

  private DbClient dbClient = dbTester.getDbClient();

  private IssueDbTester issueDbTester = new IssueDbTester(dbTester);

  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);

  private WsActionTester tester = new WsActionTester(new SetSeverityAction(userSession, dbClient, new IssueFinder(dbClient, userSession), new IssueFieldsSetter(),
    new IssueUpdater(dbClient,
      new ServerIssueStorage(system2, new DefaultRuleFinder(dbClient), dbClient, new IssueIndexer(dbClient, esTester.client())), mock(NotificationManager.class)),
    responseWriter));

  @Test
  public void set_severity() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue(newIssue().setSeverity(MAJOR));
    setUserWithBrowseAndAdministerIssuePermission(issueDto.getProjectUuid());

    call(issueDto.getKey(), MINOR);

    verify(responseWriter).write(eq(issueDto.getKey()), any(Request.class), any(Response.class));
    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getSeverity()).isEqualTo(MINOR);
    assertThat(issueReloaded.isManualSeverity()).isTrue();
  }

  @Test
  public void insert_entry_in_changelog_when_setting_severity() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue(newIssue().setSeverity(MAJOR));
    setUserWithBrowseAndAdministerIssuePermission(issueDto.getProjectUuid());

    call(issueDto.getKey(), MINOR);

    List<FieldDiffs> fieldDiffs = dbClient.issueChangeDao().selectChangelogByIssue(dbTester.getSession(), issueDto.getKey());
    assertThat(fieldDiffs).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs()).hasSize(1);
    assertThat(fieldDiffs.get(0).diffs().get("severity").newValue()).isEqualTo(MINOR);
    assertThat(fieldDiffs.get(0).diffs().get("severity").oldValue()).isEqualTo(MAJOR);
  }

  @Test
  public void fail_if_bad_severity() {
    IssueDto issueDto = issueDbTester.insertIssue(newIssue().setSeverity("unknown"));
    setUserWithBrowseAndAdministerIssuePermission(issueDto.getProjectUuid());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'severity' (unknown) must be one of: [INFO, MINOR, MAJOR, CRITICAL, BLOCKER]");
    call(issueDto.getKey(), "unknown");
  }

  @Test
  public void fail_when_not_authenticated() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    call("ABCD", MAJOR);
  }

  @Test
  public void fail_when_missing_browse_permission() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    userSession.logIn("john").addProjectUuidPermissions(ISSUE_ADMIN, issueDto.getProjectUuid());

    expectedException.expect(ForbiddenException.class);
    call(issueDto.getKey(), MAJOR);
  }

  @Test
  public void fail_when_missing_administer_issue_permission() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    userSession.logIn("john").addProjectUuidPermissions(USER, issueDto.getProjectUuid());

    expectedException.expect(ForbiddenException.class);
    call(issueDto.getKey(), MAJOR);
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("set_severity");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(2);
    assertThat(action.responseExample()).isNotNull();
  }

  private TestResponse call(@Nullable String issueKey, @Nullable String severity) {
    TestRequest request = tester.newRequest();
    setNullable(issueKey, issue -> request.setParam("issue", issue));
    setNullable(severity, value -> request.setParam("severity", value));
    return request.execute();
  }

  private IssueDto newIssue() {
    RuleDto rule = dbTester.rules().insertRule(newRuleDto());
    ComponentDto project = dbTester.components().insertProject();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    return newDto(rule, file, project);
  }

  private void setUserWithBrowseAndAdministerIssuePermission(String projectUuid) {
    userSession.logIn("john")
      .addProjectUuidPermissions(ISSUE_ADMIN, projectUuid)
      .addProjectUuidPermissions(USER, projectUuid);
  }
}
