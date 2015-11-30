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
package org.sonar.batch.report;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.scan.ImmutableProjectReactor;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.client.WsClient;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportPublisherTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DefaultAnalysisMode mode = mock(DefaultAnalysisMode.class);
  Settings settings = new Settings();
  WsClient wsClient = mock(WsClient.class, Mockito.RETURNS_DEEP_STUBS);
  ImmutableProjectReactor reactor = mock(ImmutableProjectReactor.class);
  ProjectDefinition root;
  AnalysisContextReportPublisher contextPublisher = mock(AnalysisContextReportPublisher.class);

  @Before
  public void setUp() {
    root = ProjectDefinition.create().setKey("struts").setWorkDir(temp.getRoot());
    when(reactor.getRoot()).thenReturn(root);
    when(wsClient.wsConnector().baseUrl()).thenReturn("https://localhost");
  }

  @Test
  public void log_and_dump_information_about_report_uploading() throws IOException {
    ReportPublisher underTest = new ReportPublisher(settings, wsClient, contextPublisher, reactor, mode, mock(TempFolder.class), new ReportPublisherStep[0]);

    underTest.logSuccess("TASK-123");

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can browse https://localhost/dashboard/index/struts")
      .contains("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report")
      .contains("More about the report processing at https://localhost/api/ce/task?id=TASK-123");

    File detailsFile = new File(temp.getRoot(), "analysis-details.json");
    JsonAssert.assertJson(readFileToString(detailsFile)).isSimilarTo("{" +
      "\"projectKey\": \"struts\"," +
      "\"dashboardUrl\": \"https://localhost/dashboard/index/struts\"," +
      "\"ceTaskId\": \"TASK-123\"," +
      "\"ceTaskUrl\": \"https://localhost/api/ce/task?id=TASK-123\"" +
      "}"
      );
  }

  @Test
  public void log_public_url_if_defined() throws IOException {
    settings.setProperty(CoreProperties.SERVER_BASE_URL, "https://publicserver/sonarqube");
    ReportPublisher underTest = new ReportPublisher(settings, wsClient, contextPublisher, reactor, mode, mock(TempFolder.class), new ReportPublisherStep[0]);

    underTest.logSuccess("TASK-123");

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL, you can browse https://publicserver/sonarqube/dashboard/index/struts")
      .contains("More about the report processing at https://publicserver/sonarqube/api/ce/task?id=TASK-123");

    File detailsFile = new File(temp.getRoot(), "analysis-details.json");
    JsonAssert.assertJson(readFileToString(detailsFile)).isSimilarTo("{" +
      "\"projectKey\": \"struts\"," +
      "\"dashboardUrl\": \"https://publicserver/sonarqube/dashboard/index/struts\"," +
      "\"ceTaskId\": \"TASK-123\"," +
      "\"ceTaskUrl\": \"https://publicserver/sonarqube/api/ce/task?id=TASK-123\"" +
      "}"
    );
  }

  @Test
  public void log_but_not_dump_information_when_report_is_not_uploaded() {
    ReportPublisher underTest = new ReportPublisher(settings, wsClient, contextPublisher, reactor, mode, mock(TempFolder.class), new ReportPublisherStep[0]);

    underTest.logSuccess(/* report not uploaded, no server task */null);

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("ANALYSIS SUCCESSFUL")
      .doesNotContain("dashboard/index");

    File detailsFile = new File(temp.getRoot(), "analysis-details.json");
    assertThat(detailsFile).doesNotExist();
  }

  @Test
  public void should_not_delete_report_if_property_is_set() throws IOException {
    settings.setProperty("sonar.batch.keepReport", true);
    Path reportDir = temp.getRoot().toPath().resolve("batch-report");
    Files.createDirectory(reportDir);
    ReportPublisher underTest = new ReportPublisher(settings, wsClient, contextPublisher, reactor, mode, mock(TempFolder.class), new ReportPublisherStep[0]);

    underTest.start();
    underTest.stop();
    assertThat(reportDir).isDirectory();
  }

  @Test
  public void should_delete_report_by_default() throws IOException {
    Path reportDir = temp.getRoot().toPath().resolve("batch-report");
    Files.createDirectory(reportDir);
    ReportPublisher job = new ReportPublisher(settings, wsClient, contextPublisher, reactor, mode, mock(TempFolder.class), new ReportPublisherStep[0]);

    job.start();
    job.stop();
    assertThat(reportDir).doesNotExist();
  }

}
