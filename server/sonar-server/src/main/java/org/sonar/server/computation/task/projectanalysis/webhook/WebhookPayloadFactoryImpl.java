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
package org.sonar.server.computation.task.projectanalysis.webhook;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.text.JsonWriter;

import static org.sonar.core.config.WebhookProperties.ANALYSIS_PROPERTY_PREFIX;

@ComputeEngineSide
public class WebhookPayloadFactoryImpl implements WebhookPayloadFactory {

  private final Server server;

  public WebhookPayloadFactoryImpl(Server server) {
    this.server = server;
  }

  @Override
  public WebhookPayload create(PostProjectAnalysisTask.ProjectAnalysis analysis) {
    Writer string = new StringWriter();
    try (JsonWriter writer = JsonWriter.of(string)) {
      writer.beginObject();
      writeServer(writer);
      writeTask(writer, analysis.getCeTask());
      Optional<Date> analysisDate = analysis.getAnalysisDate();
      analysisDate.ifPresent(date -> writer.propDateTime("analysedAt", date));
      writeProject(analysis, writer, analysis.getProject());
      writeQualityGate(writer, analysis.getQualityGate());
      writeAnalysisProperties(writer, analysis.getScannerContext());
      writer.endObject().close();
      return new WebhookPayload(analysis.getProject().getKey(), string.toString());
    }
  }

  private void writeServer(JsonWriter writer) {
    writer.prop("serverUrl", server.getPublicRootUrl());
  }

  private static void writeAnalysisProperties(JsonWriter writer, ScannerContext scannerContext) {
    writer.name("properties");
    writer.beginObject();
    scannerContext.getProperties().entrySet()
      .stream()
      .filter(prop -> prop.getKey().startsWith(ANALYSIS_PROPERTY_PREFIX))
      .forEach(prop -> writer.prop(prop.getKey(), prop.getValue()));
    writer.endObject();
  }

  private static void writeTask(JsonWriter writer, CeTask ceTask) {
    writer.prop("taskId", ceTask.getId());
    writer.prop("status", ceTask.getStatus().toString());
  }

  private static void writeProject(PostProjectAnalysisTask.ProjectAnalysis analysis, JsonWriter writer, Project project) {
    writer.name("project");
    writer.beginObject();
    writer.prop("key", project.getKey());
    writer.prop("name", analysis.getProject().getName());
    writer.endObject();
  }

  private static void writeQualityGate(JsonWriter writer, @Nullable QualityGate gate) {
    if (gate != null) {
      writer.name("qualityGate");
      writer.beginObject();
      writer.prop("name", gate.getName());
      writer.prop("status", gate.getStatus().toString());
      writer.name("conditions").beginArray();
      for (QualityGate.Condition condition : gate.getConditions()) {
        writer.beginObject();
        writer.prop("metric", condition.getMetricKey());
        writer.prop("operator", condition.getOperator().name());
        if (condition.getStatus() != QualityGate.EvaluationStatus.NO_VALUE) {
          writer.prop("value", condition.getValue());
        }
        writer.prop("status", condition.getStatus().name());
        writer.prop("onLeakPeriod", condition.isOnLeakPeriod());
        writer.prop("errorThreshold", condition.getErrorThreshold());
        writer.prop("warningThreshold", condition.getWarningThreshold());
        writer.endObject();
      }
      writer.endArray();
      writer.endObject();
    }
  }
}
