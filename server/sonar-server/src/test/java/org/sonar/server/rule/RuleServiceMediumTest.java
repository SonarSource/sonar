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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.rule.index.RuleIndex2;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex2 index = tester.get(RuleIndex2.class);
  RuleService service = tester.get(RuleService.class);
  DbSession dbSession;
  RuleIndexer ruleIndexer;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
    ruleIndexer = tester.get(RuleIndexer.class);
    ruleIndexer.setEnabled(true);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void list_tags() {
    // insert db
    RuleKey key1 = RuleKey.of("javascript", "S001");
    RuleKey key2 = RuleKey.of("java", "S001");
    dao.insert(dbSession,
      RuleTesting.newDto(key1).setTags(Sets.newHashSet("tag1")).setSystemTags(Sets.newHashSet("sys1", "sys2")));
    dao.insert(dbSession,
      RuleTesting.newDto(key2).setTags(Sets.newHashSet("tag2")).setSystemTags(Collections.<String>emptySet()));
    dbSession.commit();
    ruleIndexer.index();

    // all tags, including system
    Set<String> tags = service.listTags();
    assertThat(tags).containsOnly("tag1", "tag2", "sys1", "sys2");

    // verify in es
    tags = index.terms(RuleIndexDefinition.FIELD_RULE_ALL_TAGS);
    assertThat(tags).containsOnly("tag1", "tag2", "sys1", "sys2");
  }

  @Test
  public void update_rule() {
    userSessionRule.login("me").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    RuleKey key = RuleKey.of("java", "S001");

    dao.insert(dbSession, RuleTesting.newDto(key));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForCustomRule(key);
    update.setMarkdownNote("my *note*");
    service.update(update);

    dbSession.clearCache();

    RuleDto rule = dao.selectOrFailByKey(dbSession, key);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserLogin()).isEqualTo("me");
  }

  @Test(expected = UnauthorizedException.class)
  public void do_not_update_if_not_granted() {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    RuleKey key = RuleKey.of("java", "S001");

    dao.insert(dbSession, RuleTesting.newDto(key)
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc")));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(key).setMarkdownNote("my *note*");
    service.update(update);
  }

  @Test
  public void create_rule() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    // Create template rule
    RuleKey templateRuleKey = RuleKey.of("java", "S001");
    dao.insert(dbSession, RuleTesting.newTemplateRule(templateRuleKey));
    dbSession.commit();

    // Create custom rule
    NewRule newRule = NewRule.createForCustomRule("MY_CUSTOM", templateRuleKey)
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));
    RuleKey customRuleKey = service.create(newRule);

    dbSession.clearCache();

    RuleDto rule = dao.selectOrFailByKey(dbSession, customRuleKey);
    assertThat(rule).isNotNull();
  }

  @Test(expected = UnauthorizedException.class)
  public void do_not_create_if_not_granted() {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    service.create(NewRule.createForCustomRule("MY_CUSTOM", RuleKey.of("java", "S001")));
  }

  @Test
  public void delete_rule() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    // Create template rule
    RuleKey templateRuleKey = RuleKey.of("java", "S001");
    dao.insert(dbSession, RuleTesting.newTemplateRule(templateRuleKey));
    dbSession.commit();

    // Create custom rule
    NewRule newRule = NewRule.createForCustomRule("MY_CUSTOM", templateRuleKey)
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "a.*"));
    RuleKey customRuleKey = service.create(newRule);

    // Delete custom rule
    service.delete(customRuleKey);
  }

  @Test(expected = UnauthorizedException.class)
  public void do_not_delete_if_not_granted() {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    service.delete(RuleKey.of("java", "S001"));
  }
}
