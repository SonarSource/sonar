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
package org.sonarqube.tests.qualityGate;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.qualitygates.CreateConditionRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class OrganizationQualityGateTest {

  @ClassRule
  public static Orchestrator orchestrator = OrganizationQualityGateSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  @Ignore("To be reactivated when SONAR-10134 is fixed")
  public void always_display_current_quality_gate_in_effect() throws Exception {
    Organization organization = tester.organizations().generate();
    Project project = tester.projects().provision(organization);
    Qualitygates.CreateResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    tester.wsClient().qualitygates().createCondition(new CreateConditionRequest()
      .setGateId(String.valueOf(qualityGate.getId()))
      .setMetric("new_coverage")
      .setOp("LT")
      .setWarning("90")
      .setError("80")
      .setPeriod("1"));
    tester.settings().setProjectSetting(project.getKey(), "sonar.leak.period", "previous_version");
    String password = "password1";
    Users.CreateWsResponse.User user = tester.users().generateAdministrator(organization, u -> u.setPassword(password));

    WsResponse response = tester.wsClient().wsConnector().call(new GetRequest("api/navigation/component").setParam("componentKey", project.getKey()));
    Map currentQualityGate = (Map) ItUtils.jsonToMap(response.content()).get("qualityGate");
    assertThat((long) (double) (Double) currentQualityGate.get("key")).isEqualTo(qualityGate.getId());

    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"))
        .setProperty("sonar.organization", organization.getKey())
        .setProjectKey(project.getKey())
        .setProjectName(project.getName())
        .setProperty("sonar.login", user.getLogin())
        .setProperty("sonar.password", password)
        .setDebugLogs(true));

    WsResponse response2 = tester.wsClient().wsConnector().call(new GetRequest("api/navigation/component").setParam("componentKey", project.getKey()));
    Map currentQualityGate2 = (Map) ItUtils.jsonToMap(response2.content()).get("qualityGate");
    assertThat((long) (double) (Double) currentQualityGate2.get("key")).isEqualTo(qualityGate.getId());

    Qualitygates.CreateResponse qualityGate2 = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate2, project);
    tester.wsClient().qualitygates().createCondition(new CreateConditionRequest()
      .setGateId(String.valueOf(qualityGate2.getId()))
      .setMetric("new_coverage")
      .setOp("LT")
      .setWarning("90")
      .setError("80")
      .setPeriod("1"));

    WsResponse response3 = tester.wsClient().wsConnector().call(new GetRequest("api/navigation/component").setParam("componentKey", project.getKey()));
    Map currentQualityGate3 = (Map) ItUtils.jsonToMap(response3.content()).get("qualityGate");
    assertThat((long) (double) (Double) currentQualityGate3.get("key")).isEqualTo(qualityGate2.getId());
  }
}
