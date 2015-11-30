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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;
import com.squareup.okhttp.HttpUrl;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.ImmutableProjectReactor;
import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.ce.SubmitWsRequest;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;

@BatchSide
public class ReportPublisher implements Startable {

  private static final Logger LOG = Loggers.get(ReportPublisher.class);

  public static final String KEEP_REPORT_PROP_KEY = "sonar.batch.keepReport";
  public static final String VERBOSE_KEY = "sonar.verbose";
  public static final String METADATA_DUMP_FILENAME = "analysis-details.json";

  private final Settings settings;
  private final WsClient wsClient;
  private final AnalysisContextReportPublisher contextPublisher;
  private final ImmutableProjectReactor projectReactor;
  private final DefaultAnalysisMode analysisMode;
  private final TempFolder temp;
  private final ReportPublisherStep[] publishers;

  private File reportDir;
  private BatchReportWriter writer;

  public ReportPublisher(Settings settings, WsClient wsClient, AnalysisContextReportPublisher contextPublisher,
    ImmutableProjectReactor projectReactor, DefaultAnalysisMode analysisMode, TempFolder temp, ReportPublisherStep[] publishers) {
    this.settings = settings;
    this.wsClient = wsClient;
    this.contextPublisher = contextPublisher;
    this.projectReactor = projectReactor;
    this.analysisMode = analysisMode;
    this.temp = temp;
    this.publishers = publishers;
  }

  @Override
  public void start() {
    reportDir = new File(projectReactor.getRoot().getWorkDir(), "batch-report");
    writer = new BatchReportWriter(reportDir);
    contextPublisher.init(writer);
  }

  @Override
  public void stop() {
    if (!settings.getBoolean(KEEP_REPORT_PROP_KEY) && !settings.getBoolean(VERBOSE_KEY)) {
      FileUtils.deleteQuietly(reportDir);
    } else {
      LOG.info("Analysis report generated in " + reportDir);
    }
  }

  public File getReportDir() {
    return reportDir;
  }

  public BatchReportWriter getWriter() {
    return writer;
  }

  public void execute() {
    // If this is a issues mode analysis then we should not upload reports
    String taskId = null;
    if (!analysisMode.isIssues()) {
      File report = generateReportFile();
      if (!analysisMode.isMediumTest()) {
        taskId = upload(report);
      }
    }
    logSuccess(taskId);
  }

  private File generateReportFile() {
    try {
      long startTime = System.currentTimeMillis();
      for (ReportPublisherStep publisher : publishers) {
        publisher.publish(writer);
      }
      long stopTime = System.currentTimeMillis();
      LOG.info("Analysis report generated in " + (stopTime - startTime) + "ms, dir size=" + FileUtils.byteCountToDisplaySize(FileUtils.sizeOfDirectory(reportDir)));

      startTime = System.currentTimeMillis();
      File reportZip = temp.newFile("batch-report", ".zip");
      ZipUtils.zipDir(reportDir, reportZip);
      stopTime = System.currentTimeMillis();
      LOG.info("Analysis reports compressed in " + (stopTime - startTime) + "ms, zip size=" + FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(reportZip)));
      return reportZip;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to prepare analysis report", e);
    }
  }

  /**
   * Uploads the report file to server and returns the generated task id
   */
  @VisibleForTesting
  String upload(File report) {
    LOG.debug("Upload report");
    long startTime = System.currentTimeMillis();
    ProjectDefinition projectDefinition = projectReactor.getRoot();
    SubmitWsRequest submitRequest = new SubmitWsRequest();
    submitRequest.setProjectKey(projectDefinition.getKey());
    submitRequest.setProjectName(projectDefinition.getName());
    submitRequest.setProjectBranch(projectDefinition.getBranch());
    submitRequest.setReport(report);
    WsCe.SubmitResponse submitResponse = wsClient.computeEngine().submit(submitRequest);
    long stopTime = System.currentTimeMillis();
    LOG.info("Analysis report uploaded in " + (stopTime - startTime) + "ms");
    return submitResponse.getTaskId();
  }

  @VisibleForTesting
  void logSuccess(@Nullable String taskId) {
    if (taskId == null) {
      LOG.info("ANALYSIS SUCCESSFUL");
    } else {
      Map<String, String> metadata = new LinkedHashMap<>();
      String effectiveKey = projectReactor.getRoot().getKeyWithBranch();
      metadata.put("projectKey", effectiveKey);

      URL dashboardUrl = HttpUrl.parse(publicUrl()).newBuilder()
        .addPathSegment("dashboard").addPathSegment("index").addPathSegment(effectiveKey)
        .build()
        .url();
      metadata.put("dashboardUrl", dashboardUrl.toExternalForm());

      URL taskUrl = HttpUrl.parse(publicUrl()).newBuilder()
        .addPathSegment("api").addPathSegment("ce").addPathSegment("task")
        .addQueryParameter("id", taskId)
        .build()
        .url();
      metadata.put("ceTaskId", taskId);
      metadata.put("ceTaskUrl", taskUrl.toExternalForm());

      LOG.info("ANALYSIS SUCCESSFUL, you can browse {}", dashboardUrl);
      LOG.info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report");
      LOG.info("More about the report processing at {}", taskUrl);

      dumpMetadata(metadata);
    }
  }

  private void dumpMetadata(Map<String, String> metadata) {
    File file = new File(projectReactor.getRoot().getWorkDir(), METADATA_DUMP_FILENAME);
    try (Writer output = Files.newWriter(file, StandardCharsets.UTF_8)) {
      JsonWriter json = JsonWriter.of(output);
      json.beginObject();
      for (Map.Entry<String, String> entry : metadata.entrySet()) {
        json.prop(entry.getKey(), entry.getValue());
      }
      json.endObject();

      LOG.debug("Report metadata written to {}", file);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to dump " + file, e);
    }
  }

  /**
   * The public URL is optionally configured on server. If not, then the regular URL is returned.
   * See https://jira.sonarsource.com/browse/SONAR-4239
   */
  private String publicUrl() {
    return defaultIfBlank(settings.getString(CoreProperties.SERVER_BASE_URL), wsClient.wsConnector().baseUrl());
  }
}
