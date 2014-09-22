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

package org.sonar.server.issue;

import com.google.common.collect.Multiset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.user.UserSession;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IssueServiceTest {

  @Mock
  DefaultIssueFinder finder;

  @Mock
  IssueWorkflow workflow;

  @Mock
  IssueUpdater issueUpdater;

  @Mock
  IssueStorage issueStorage;

  @Mock
  IssueNotifications issueNotifications;

  @Mock
  ActionPlanService actionPlanService;

  @Mock
  RuleFinder ruleFinder;

  @Mock
  ResourceDao resourceDao;

  @Mock
  IssueDao issueDao;

  @Mock
  AuthorizationDao authorizationDao;

  @Mock
  UserFinder userFinder;

  @Mock
  UserSession userSession;

  @Mock
  IssueQueryResult issueQueryResult;

  @Mock
  ResourceDto resource;

  @Mock
  ResourceDto project;

  Transition transition = Transition.create("reopen", Issue.STATUS_RESOLVED, Issue.STATUS_REOPENED);

  DefaultIssue issue = new DefaultIssue().setKey("ABCD");

  IssueService issueService;

  @Before
  public void before() {
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.userId()).thenReturn(10);
    when(userSession.login()).thenReturn("arthur");

    when(authorizationDao.isAuthorizedComponentKey(anyString(), eq(10), anyString())).thenReturn(true);
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);
    when(issueQueryResult.issues()).thenReturn(newArrayList((Issue) issue));
    when(issueQueryResult.first()).thenReturn(issue);

    when(resource.getKey()).thenReturn("org.sonar.Sample");
    when(project.getKey()).thenReturn("Sample");

    issueService = new IssueService(finder, workflow, issueStorage, issueUpdater, issueNotifications, actionPlanService, ruleFinder, resourceDao, issueDao,
      authorizationDao, userFinder, mock(PreviewCache.class));
  }

  @Test
  public void load_issue() {
    IssueQueryResult result = issueService.loadIssue("ABCD");
    assertThat(result).isEqualTo(issueQueryResult);
  }

  @Test
  public void fail_to_load_issue() {
    when(issueQueryResult.issues()).thenReturn(Collections.<Issue>emptyList());
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    try {
      issueService.loadIssue("ABCD");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Issue not found: ABCD");
    }
  }

  @Test
  public void list_status() {
    issueService.listStatus();
    verify(workflow).statusKeys();
  }

  @Test
  public void list_transitions() {
    List<Transition> transitions = newArrayList(transition);
    when(workflow.outTransitions(issue)).thenReturn(transitions);

    List<Transition> result = issueService.listTransitions("ABCD", userSession);
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(transition);
  }

  @Test
  public void return_no_transition() {
    when(issueQueryResult.first()).thenReturn(null);
    when(issueQueryResult.issues()).thenReturn(newArrayList((Issue) new DefaultIssue()));

    assertThat(issueService.listTransitions("ABCD", userSession)).isEmpty();
    verifyZeroInteractions(workflow);
  }

  @Test
  public void do_transition() {
    when(workflow.doTransition(eq(issue), eq(transition.key()), any(IssueChangeContext.class))).thenReturn(true);

    Issue result = issueService.doTransition("ABCD", transition.key(), userSession);
    assertThat(result).isNotNull();

    ArgumentCaptor<IssueChangeContext> measureCaptor = ArgumentCaptor.forClass(IssueChangeContext.class);
    verify(workflow).doTransition(eq(issue), eq(transition.key()), measureCaptor.capture());
    verify(issueStorage).save(issue);

    IssueChangeContext issueChangeContext = measureCaptor.getValue();
    assertThat(issueChangeContext.login()).isEqualTo("arthur");
    assertThat(issueChangeContext.date()).isNotNull();

    verify(issueNotifications).sendChanges(eq(issue), eq(issueChangeContext), eq(issueQueryResult));
  }

  @Test
  public void not_do_transition() {
    when(workflow.doTransition(eq(issue), eq(transition.key()), any(IssueChangeContext.class))).thenReturn(false);

    Issue result = issueService.doTransition("ABCD", transition.key(), userSession);
    assertThat(result).isNotNull();
    verify(workflow).doTransition(eq(issue), eq(transition.key()), any(IssueChangeContext.class));
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void fail_do_transition_if_not_logged() {
    when(userSession.isLoggedIn()).thenReturn(false);
    try {
      issueService.doTransition("ABCD", transition.key(), userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("User is not logged in");
    }
    verifyZeroInteractions(authorizationDao);
  }

  @Test
  public void assign() {
    String assignee = "perceval";
    User user = new DefaultUser();

    when(userFinder.findByLogin(assignee)).thenReturn(user);
    when(issueUpdater.assign(eq(issue), eq(user), any(IssueChangeContext.class))).thenReturn(true);

    Issue result = issueService.assign("ABCD", assignee, userSession);
    assertThat(result).isNotNull();

    ArgumentCaptor<IssueChangeContext> measureCaptor = ArgumentCaptor.forClass(IssueChangeContext.class);
    verify(issueUpdater).assign(eq(issue), eq(user), measureCaptor.capture());
    verify(issueStorage).save(issue);

    IssueChangeContext issueChangeContext = measureCaptor.getValue();
    assertThat(issueChangeContext.login()).isEqualTo("arthur");
    assertThat(issueChangeContext.date()).isNotNull();

    verify(issueNotifications).sendChanges(eq(issue), eq(issueChangeContext), eq(issueQueryResult));
  }

  @Test
  public void unassign() {
    when(issueUpdater.assign(eq(issue), eq((User) null), any(IssueChangeContext.class))).thenReturn(true);

    Issue result = issueService.assign("ABCD", null, userSession);
    assertThat(result).isNotNull();

    ArgumentCaptor<IssueChangeContext> measureCaptor = ArgumentCaptor.forClass(IssueChangeContext.class);
    verify(issueUpdater).assign(eq(issue), eq((User) null), measureCaptor.capture());
    verify(issueStorage).save(issue);

    IssueChangeContext issueChangeContext = measureCaptor.getValue();
    assertThat(issueChangeContext.login()).isEqualTo("arthur");
    assertThat(issueChangeContext.date()).isNotNull();

    verify(issueNotifications).sendChanges(eq(issue), eq(issueChangeContext), eq(issueQueryResult));
    verify(userFinder, never()).findByLogin(anyString());
  }

  @Test
  public void not_assign() {
    String assignee = "perceval";
    User user = new DefaultUser();

    when(userFinder.findByLogin(assignee)).thenReturn(user);
    when(issueUpdater.assign(eq(issue), eq(user), any(IssueChangeContext.class))).thenReturn(false);

    Issue result = issueService.assign("ABCD", assignee, userSession);
    assertThat(result).isNotNull();

    verify(issueUpdater).assign(eq(issue), eq(user), any(IssueChangeContext.class));
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void fail_assign_if_assignee_not_found() {
    String assignee = "perceval";

    when(userFinder.findByLogin(assignee)).thenReturn(null);

    try {
      issueService.assign("ABCD", assignee, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown user: perceval");
    }

    verifyZeroInteractions(issueUpdater);
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void plan() {
    String actionPlanKey = "EFGH";

    ActionPlan actionPlan = new DefaultActionPlan();

    when(actionPlanService.findByKey(actionPlanKey, userSession)).thenReturn(actionPlan);
    when(issueUpdater.plan(eq(issue), eq(actionPlan), any(IssueChangeContext.class))).thenReturn(true);

    Issue result = issueService.plan("ABCD", actionPlanKey, userSession);
    assertThat(result).isNotNull();

    ArgumentCaptor<IssueChangeContext> measureCaptor = ArgumentCaptor.forClass(IssueChangeContext.class);
    verify(issueUpdater).plan(eq(issue), eq(actionPlan), measureCaptor.capture());
    verify(issueStorage).save(issue);

    IssueChangeContext issueChangeContext = measureCaptor.getValue();
    assertThat(issueChangeContext.login()).isEqualTo("arthur");
    assertThat(issueChangeContext.date()).isNotNull();

    verify(issueNotifications).sendChanges(eq(issue), eq(issueChangeContext), eq(issueQueryResult));
  }

  @Test
  public void unplan() {
    when(issueUpdater.plan(eq(issue), eq((ActionPlan) null), any(IssueChangeContext.class))).thenReturn(true);

    Issue result = issueService.plan("ABCD", null, userSession);
    assertThat(result).isNotNull();

    ArgumentCaptor<IssueChangeContext> measureCaptor = ArgumentCaptor.forClass(IssueChangeContext.class);
    verify(issueUpdater).plan(eq(issue), eq((ActionPlan) null), measureCaptor.capture());
    verify(issueStorage).save(issue);

    IssueChangeContext issueChangeContext = measureCaptor.getValue();
    assertThat(issueChangeContext.login()).isEqualTo("arthur");
    assertThat(issueChangeContext.date()).isNotNull();

    verify(issueNotifications).sendChanges(eq(issue), eq(issueChangeContext), eq(issueQueryResult));
    verify(actionPlanService, never()).findByKey(anyString(), any(UserSession.class));
  }

  @Test
  public void not_plan() {
    String actionPlanKey = "EFGH";

    ActionPlan actionPlan = new DefaultActionPlan();

    when(actionPlanService.findByKey(actionPlanKey, userSession)).thenReturn(actionPlan);
    when(issueUpdater.plan(eq(issue), eq(actionPlan), any(IssueChangeContext.class))).thenReturn(false);

    Issue result = issueService.plan("ABCD", actionPlanKey, userSession);
    assertThat(result).isNotNull();
    verify(issueUpdater).plan(eq(issue), eq(actionPlan), any(IssueChangeContext.class));
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void fail_plan_if_action_plan_not_found() {
    String actionPlanKey = "EFGH";

    when(actionPlanService.findByKey(actionPlanKey, userSession)).thenReturn(null);
    try {
      issueService.plan("ABCD", actionPlanKey, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown action plan: EFGH");
    }

    verifyZeroInteractions(issueUpdater);
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void set_severity() {
    String severity = "MINOR";
    when(issueUpdater.setManualSeverity(eq(issue), eq(severity), any(IssueChangeContext.class))).thenReturn(true);

    Issue result = issueService.setSeverity("ABCD", severity, userSession);
    assertThat(result).isNotNull();

    ArgumentCaptor<IssueChangeContext> measureCaptor = ArgumentCaptor.forClass(IssueChangeContext.class);
    verify(issueUpdater).setManualSeverity(eq(issue), eq(severity), measureCaptor.capture());
    verify(issueStorage).save(issue);

    IssueChangeContext issueChangeContext = measureCaptor.getValue();
    assertThat(issueChangeContext.login()).isEqualTo("arthur");
    assertThat(issueChangeContext.date()).isNotNull();

    verify(issueNotifications).sendChanges(eq(issue), eq(issueChangeContext), eq(issueQueryResult));
  }

  @Test
  public void not_set_severity() {
    String severity = "MINOR";
    when(issueUpdater.setManualSeverity(eq(issue), eq(severity), any(IssueChangeContext.class))).thenReturn(false);

    Issue result = issueService.setSeverity("ABCD", severity, userSession);
    assertThat(result).isNotNull();
    verify(issueUpdater).setManualSeverity(eq(issue), eq(severity), any(IssueChangeContext.class));
    verifyZeroInteractions(issueStorage);
    verifyZeroInteractions(issueNotifications);
  }

  @Test
  public void create_manual_issue() {
    RuleKey ruleKey = RuleKey.of("manual", "manualRuleKey");
    when(ruleFinder.findByKey(ruleKey)).thenReturn(Rule.create("manual", "manualRuleKey"));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(resource);
    when(resourceDao.getRootProjectByComponentKey(anyString())).thenReturn(project);

    Issue result = issueService.createManualIssue("org.sonar.Sample", RuleKey.of("manual", "manualRuleKey"), null, "Fix it", null, null, userSession);
    assertThat(result).isNotNull();
    assertThat(result.message()).isEqualTo("Fix it");
    assertThat(result.creationDate()).isNotNull();
    assertThat(result.updateDate()).isNotNull();

    verify(issueStorage).save(any(DefaultIssue.class));
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.USER));
  }

  @Test
  public void create_manual_issue_use_rule_name_if_no_message() {
    RuleKey ruleKey = RuleKey.of("manual", "manualRuleKey");
    when(ruleFinder.findByKey(ruleKey)).thenReturn(Rule.create("manual", "manualRuleKey").setName("Manual Rule"));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(resource);
    when(resourceDao.getRootProjectByComponentKey(anyString())).thenReturn(project);

    Issue result = issueService.createManualIssue("org.sonar.Sample", RuleKey.of("manual", "manualRuleKey"), null, "", null, null, userSession);
    assertThat(result).isNotNull();
    assertThat(result.message()).isEqualTo("Manual Rule");
    assertThat(result.creationDate()).isNotNull();
    assertThat(result.updateDate()).isNotNull();

    verify(issueStorage).save(any(DefaultIssue.class));
    verify(authorizationDao).isAuthorizedComponentKey(anyString(), anyInt(), eq(UserRole.USER));
  }

  @Test
  public void fail_create_manual_issue_if_not_having_required_role() {
    RuleKey ruleKey = RuleKey.of("manual", "manualRuleKey");
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(resource);
    when(resourceDao.getRootProjectByComponentKey(anyString())).thenReturn(project);
    when(ruleFinder.findByKey(ruleKey)).thenReturn(Rule.create("manual", "manualRuleKey"));
    when(authorizationDao.isAuthorizedComponentKey(anyString(), eq(10), anyString())).thenReturn(false);

    try {
      issueService.createManualIssue("org.sonar.Sample", ruleKey, null, null, null, null, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("User does not have the required role");
    }
  }

  @Test
  public void fail_create_manual_issue_if_not_manual_rule() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(resource);
    when(resourceDao.getRootProjectByComponentKey(anyString())).thenReturn(project);

    RuleKey ruleKey = RuleKey.of("squid", "s100");
    try {
      issueService.createManualIssue("org.sonar.Sample", ruleKey, null, null, null, null, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Issues can be created only on rules marked as 'manual': squid:s100");
    }
    verifyZeroInteractions(issueStorage);
  }

  @Test
  public void fail_create_manual_issue_if_rule_not_found() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(resource);
    when(resourceDao.getRootProjectByComponentKey(anyString())).thenReturn(project);

    RuleKey ruleKey = RuleKey.of("manual", "manualRuleKey");
    when(ruleFinder.findByKey(ruleKey)).thenReturn(null);
    try {
      issueService.createManualIssue("org.sonar.Sample", RuleKey.of("manual", "manualRuleKey"), null, null, null, null, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown rule: manual:manualRuleKey");
    }
    verifyZeroInteractions(issueStorage);
  }

  @Test
  public void fail_create_manual_issue_if_component_not_found() {
    RuleKey ruleKey = RuleKey.of("manual", "manualRuleKey");
    when(ruleFinder.findByKey(ruleKey)).thenReturn(Rule.create("manual", "manualRuleKey"));
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);
    try {
      issueService.createManualIssue("org.sonar.Sample", RuleKey.of("manual", "manualRuleKey"), null, null, null, null, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown component: org.sonar.Sample");
    }
    verifyZeroInteractions(issueStorage);
  }

  @Test
  public void find_rules_by_component() throws Exception {
    DbSession session = mock(DbSession.class);
    String componentKey = "org.sonar.Sample";

    Date date = new Date();
    when(issueDao.findRulesByComponent(componentKey, date, session)).thenReturn(newArrayList(
        RuleDto.createFor(RuleKey.of("repo", "rule")).setName("Rule name"),
        RuleDto.createFor(RuleKey.of("repo", "rule")).setName("Rule name")
    ));

    RulesAggregation result = issueService.findRulesByComponent(componentKey, date, session);
    assertThat(result.rules()).hasSize(1);
  }

  @Test
  public void find_rules_by_severity() throws Exception {
    DbSession session = mock(DbSession.class);
    String componentKey = "org.sonar.Sample";

    Date date = new Date();
    when(issueDao.findSeveritiesByComponent(componentKey, date, session)).thenReturn(newArrayList("MAJOR", "MAJOR", "INFO"));

    Multiset<String> result = issueService.findSeveritiesByComponent(componentKey, date, session);
    assertThat(result.count("MAJOR")).isEqualTo(2);
    assertThat(result.count("INFO")).isEqualTo(1);
    assertThat(result.count("UNKNOWN")).isEqualTo(0);
  }
}
