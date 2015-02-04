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

package org.sonar.server.computation;

import org.sonar.batch.protocol.output.BatchOutputReader;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;

import java.util.Date;

public class ComputationContext {

  private final AnalysisReportDto reportDto;
  private final ComponentDto project;
  private final BatchOutputReader reportReader;

  /**
   * Cache of analysis date as it can be accessed several times
   */
  private Date analysisDate = null;

  public ComputationContext(AnalysisReportDto reportDto, ComponentDto project, BatchOutputReader reportReader) {
    this.reportDto = reportDto;
    this.project = project;
    this.reportReader = reportReader;
  }

  public AnalysisReportDto getReportDto() {
    return reportDto;
  }

  public ComponentDto getProject() {
    return project;
  }

  public BatchOutputReader getReportReader() {
    return reportReader;
  }

  public Date getAnalysisDate() {
    if (analysisDate == null) {
      analysisDate = new Date(reportReader.readMetadata().getAnalysisDate());
    }
    return analysisDate;
  }
}
