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
package org.sonar.server.user.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsUsers.CurrentWsResponse;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;

public class CurrentActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private WsActionTester ws;
  private Settings settings = mock(Settings.class);

  @Before
  public void before() {
    ws = new WsActionTester(new CurrentAction(userSessionRule, dbClient, defaultOrganizationProvider, settings));
  }

  @Test
  public void json_example() {
    userSessionRule.logIn("obiwan.kenobi").setName("Obiwan Kenobi");

    // permissions on default organization
    userSessionRule
      .addPermission(SCAN, db.getDefaultOrganization())
      .addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization());

    // permissions on other organizations are ignored
    userSessionRule.addPermission(ADMINISTER, db.organizations().insert());

    UserDto obiwan = db.users().insertUser(
      newUserDto("obiwan.kenobi", "Obiwan Kenobi", "obiwan.kenobi@starwars.com")
        .setLocal(true)
        .setExternalIdentity("obiwan.kenobi")
        .setExternalIdentityProvider("sonarqube")
        .setScmAccounts(newArrayList("obiwan:github", "obiwan:bitbucket")));
    GroupDto jedi = db.users().insertGroup(newGroupDto().setName("Jedi"));
    GroupDto rebel = db.users().insertGroup(newGroupDto().setName("Rebel"));
    db.users().insertGroup(newGroupDto().setName("Sith"));
    dbClient.userGroupDao().insert(db.getSession(), new UserGroupDto()
      .setUserId(obiwan.getId())
      .setGroupId(jedi.getId()));
    dbClient.userGroupDao().insert(db.getSession(), new UserGroupDto()
      .setUserId(obiwan.getId())
      .setGroupId(rebel.getId()));
    db.users().setOnboarded(obiwan, true);
    db.commit();

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("current-example.json"));
  }

  @Test
  public void anonymous() {
    userSessionRule.anonymous();

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("CurrentActionTest/anonymous.json"));
  }

  @Test
  public void should_return_showOnboardingTutorial_false_for_anonymous() {
    userSessionRule.anonymous();

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);
    assertThat(response.getShowOnboardingTutorial()).isFalse();
  }

  @Test
  public void should_return_showOnboardingTutorial_true_for_not_yet_onboarded_user() {
    String login = randomAlphanumeric(10);
    userSessionRule.logIn(login);
    UserDto user = db.users().insertUser(u -> u.setLogin(login));
    db.users().setOnboarded(user, false);

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);
    assertThat(response.getShowOnboardingTutorial()).isTrue();
  }

  @Test
  public void should_return_showOnboardingTutorial_false_for_onboarded_user() {
    String login = randomAlphanumeric(10);
    userSessionRule.logIn(login);
    UserDto user = db.users().insertUser(u -> u.setLogin(login));
    db.users().setOnboarded(user, true);

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);
    assertThat(response.getShowOnboardingTutorial()).isFalse();
  }

  @Test
  public void should_return_showOnboardingTutorial_false_for_not_yet_onboarded_user_if_onboarding_is_globally_skipped() {
    doReturn(true).when(settings).getBoolean(eq(CoreProperties.SKIP_ONBOARDING_TUTORIAL));

    String login = randomAlphanumeric(10);
    userSessionRule.logIn(login);
    UserDto user = db.users().insertUser(u -> u.setLogin(login));
    db.users().setOnboarded(user, false);

    CurrentWsResponse response = ws.newRequest().executeProtobuf(CurrentWsResponse.class);
    assertThat(response.getShowOnboardingTutorial()).isFalse();
  }
}
