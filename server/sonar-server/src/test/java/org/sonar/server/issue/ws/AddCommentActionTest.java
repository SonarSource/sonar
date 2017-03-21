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

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.IssueUpdater;
import org.sonar.server.issue.ServerIssueStorage;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.issue.IssueChangeDto.TYPE_COMMENT;

public class AddCommentActionTest {

  private static final long NOW = 10_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private System2 system2 = mock(System2.class);

  private DbClient dbClient = dbTester.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);

  private IssueDbTester issueDbTester = new IssueDbTester(dbTester);

  private IssueIndexer issueIndexer = new IssueIndexer(esTester.client(), new IssueIteratorFactory(dbClient));
  private ServerIssueStorage serverIssueStorage = new ServerIssueStorage(system2, new DefaultRuleFinder(dbClient, defaultOrganizationProvider), dbClient, issueIndexer);
  private IssueUpdater issueUpdater = new IssueUpdater(dbClient, serverIssueStorage, mock(NotificationManager.class), defaultOrganizationProvider);
  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);

  private WsActionTester tester = new WsActionTester(
    new AddCommentAction(system2, userSession, dbClient, new IssueFinder(dbClient, userSession), issueUpdater, new IssueFieldsSetter(), responseWriter));

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void add_comment() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    userSession.logIn("john").addProjectUuidPermissions(USER, issueDto.getProjectUuid());

    call(issueDto.getKey(), "please fix it");

    verify(responseWriter).write(eq(issueDto.getKey()), any(Request.class), any(Response.class));
    IssueChangeDto issueComment = dbClient.issueChangeDao().selectByTypeAndIssueKeys(dbTester.getSession(), singletonList(issueDto.getKey()), TYPE_COMMENT).get(0);
    assertThat(issueComment.getKey()).isNotNull();
    assertThat(issueComment.getUserLogin()).isEqualTo("john");
    assertThat(issueComment.getChangeType()).isEqualTo(TYPE_COMMENT);
    assertThat(issueComment.getChangeData()).isEqualTo("please fix it");
    assertThat(issueComment.getCreatedAt()).isNotNull();
    assertThat(issueComment.getUpdatedAt()).isNotNull();
    assertThat(issueComment.getIssueKey()).isEqualTo(issueDto.getKey());
    assertThat(issueComment.getIssueChangeCreationDate()).isNotNull();

    IssueDto issueReloaded = dbClient.issueDao().selectByKey(dbTester.getSession(), issueDto.getKey()).get();
    assertThat(issueReloaded.getIssueUpdateTime()).isEqualTo(NOW);
  }

  @Test
  public void fail_when_missing_issue_key() throws Exception {
    userSession.logIn("john");

    expectedException.expect(IllegalArgumentException.class);
    call(null, "please fix it");
  }

  @Test
  public void fail_when_issue_does_not_exist() throws Exception {
    userSession.logIn("john");

    expectedException.expect(NotFoundException.class);
    call("ABCD", "please fix it");
  }

  @Test
  public void fail_when_missing_comment_text() throws Exception {
    userSession.logIn("john");

    expectedException.expect(IllegalArgumentException.class);
    call("ABCD", null);
  }

  @Test
  public void fail_when_empty_comment_text() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    userSession.logIn("john").addProjectUuidPermissions(USER, issueDto.getProjectUuid());

    expectedException.expect(IllegalArgumentException.class);
    call(issueDto.getKey(), "");
  }

  @Test
  public void fail_when_not_authenticated() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    call("ABCD", "please fix it");
  }

  @Test
  public void fail_when_not_enough_permission() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    userSession.logIn("john").addProjectUuidPermissions(CODEVIEWER, issueDto.getProjectUuid());

    expectedException.expect(ForbiddenException.class);
    call(issueDto.getKey(), "please fix it");
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("add_comment");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(2);
    assertThat(action.responseExample()).isNotNull();
  }

  private TestResponse call(@Nullable String issueKey, @Nullable String commentText) {
    TestRequest request = tester.newRequest();
    setNullable(issueKey, issue -> request.setParam("issue", issue));
    setNullable(commentText, text -> request.setParam("text", text));
    return request.execute();
  }

}
