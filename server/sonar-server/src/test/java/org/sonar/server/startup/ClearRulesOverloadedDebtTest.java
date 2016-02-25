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
package org.sonar.server.startup;

import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.loadedtemplate.LoadedTemplateDto.ONE_SHOT_TASK_TYPE;

public class ClearRulesOverloadedDebtTest {

  static final int SUB_CHARACTERISTIC_ID = 1;

  private static final RuleKey RULE_KEY_1 = RuleTesting.XOO_X1;
  private static final RuleKey RULE_KEY_2 = RuleTesting.XOO_X2;
  private static final RuleKey RULE_KEY_3 = RuleTesting.XOO_X3;

  @Rule
  public DbTester tester = DbTester.create(System2.INSTANCE);

  @Rule
  public LogTester logTester = new LogTester();

  DbClient dbClient = tester.getDbClient();
  DbSession dbSession = tester.getSession();
  RuleDao ruleDao = new RuleDao();

  ClearRulesOverloadedDebt underTest = new ClearRulesOverloadedDebt(dbClient);

  @Test
  public void remove_overridden_debt() throws Exception {
    // Characteristic and remediation function is overridden
    insertRuleDto(RULE_KEY_1, SUB_CHARACTERISTIC_ID, "LINEAR", null, "1d");
    // Only characteristic is overridden
    insertRuleDto(RULE_KEY_2, SUB_CHARACTERISTIC_ID, null, null, null);
    // Only remediation function is overridden
    insertRuleDto(RULE_KEY_3, null, "CONSTANT_ISSUE", "5min", null);

    underTest.start();

    verifyRuleHasNotOverriddenDebt(RULE_KEY_1);
    verifyRuleHasNotOverriddenDebt(RULE_KEY_2);
    verifyRuleHasNotOverriddenDebt(RULE_KEY_3);
    verifyTaskRegistered();
    verifyLog();
  }

  @Test
  public void not_update_rule_debt_not_overridden() throws Exception {
    RuleDto rule = insertRuleDto(RULE_KEY_1, null, null, null, null);
    Date updateAt = rule.getUpdatedAt();

    underTest.start();

    RuleDto reloaded = ruleDao.selectOrFailByKey(dbSession, RULE_KEY_1);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(updateAt);
    verifyRuleHasNotOverriddenDebt(RULE_KEY_1);

    verifyTaskRegistered();
    verifyEmptyLog();
  }

  @Test
  public void not_update_rule_debt_when_sqale_is_installed() throws Exception {
    insertSqaleProperty();
    RuleDto rule = insertRuleDto(RULE_KEY_1, SUB_CHARACTERISTIC_ID, "LINEAR", null, "1d");
    Date updateAt = rule.getUpdatedAt();

    underTest.start();

    RuleDto reloaded = ruleDao.selectOrFailByKey(dbSession, RULE_KEY_1);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(updateAt);

    verifyTaskRegistered();
    verifyEmptyLog();
  }

  @Test
  public void not_execute_task_when_already_executed() throws Exception {
    insertRuleDto(RULE_KEY_1, SUB_CHARACTERISTIC_ID, "LINEAR", null, "1d");
    underTest.start();
    verifyLog();
    verifyTaskRegistered();

    logTester.clear();
    underTest.start();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    verifyEmptyLog();
  }

  private void verifyRuleHasNotOverriddenDebt(RuleKey ruleKey) {
    // Refresh session
    dbSession.commit(true);

    RuleDto ruleDto = ruleDao.selectOrFailByKey(dbSession, ruleKey);
    assertThat(ruleDto.getSubCharacteristicId()).isNull();
    assertThat(ruleDto.getRemediationFunction()).isNull();
    assertThat(ruleDto.getRemediationCoefficient()).isNull();
    assertThat(ruleDto.getRemediationOffset()).isNull();
  }

  private RuleDto insertRuleDto(RuleKey ruleKey, @Nullable Integer subCharId, @Nullable String function, @Nullable String coeff, @Nullable String offset) {
    RuleDto ruleDto = RuleTesting.newDto(ruleKey).setSubCharacteristicId(subCharId).setRemediationFunction(function).setRemediationOffset(offset).setRemediationCoefficient(coeff);
    ruleDao.insert(dbSession,
      ruleDto
      );
    dbSession.commit();
    return ruleDto;
  }

  private void insertSqaleProperty() {
    dbClient.propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.sqale.licenseHash.secured").setValue("ABCD"));
    dbSession.commit();
  }

  private void verifyTaskRegistered() {
    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(ONE_SHOT_TASK_TYPE, "ClearRulesOverloadedDebt")).isEqualTo(1);
  }

  private void verifyLog() {
    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "The SQALE model has been cleaned to remove any redundant data left over from previous migrations.",
      "=> As a result, the technical debt of existing issues in your projects may change slightly when those projects are reanalyzed.");
  }

  private void verifyEmptyLog() {
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }
}
