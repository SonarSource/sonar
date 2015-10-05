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
package org.sonar.server.computation.scm;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.source.SourceService;

import static com.google.common.base.Preconditions.checkNotNull;

public class ScmInfoRepositoryImpl implements ScmInfoRepository {

  private static final Logger LOGGER = Loggers.get(ScmInfoRepositoryImpl.class);

  private final BatchReportReader batchReportReader;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private final SourceService sourceService;

  private final Map<Component, ScmInfo> scmInfoCache = new HashMap<>();

  public ScmInfoRepositoryImpl(BatchReportReader batchReportReader, AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient, SourceService sourceService) {
    this.batchReportReader = batchReportReader;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
    this.sourceService = sourceService;
  }

  @Override
  public Optional<ScmInfo> getScmInfo(Component component) {
    checkNotNull(component, "Component cannot be bull");
    initializeScmInfoForComponent(component);
    return Optional.fromNullable(scmInfoCache.get(component));
  }

  private void initializeScmInfoForComponent(Component component) {
    if (scmInfoCache.containsKey(component)) {
      return;
    }
    Optional<ScmInfo> scmInfoOptional = getScmInfoForComponent(component);
    scmInfoCache.put(component, scmInfoOptional.isPresent() ? scmInfoOptional.get() : null);
  }

  private Optional<ScmInfo> getScmInfoForComponent(Component component) {
    BatchReport.Changesets changesets = batchReportReader.readChangesets(component.getReportAttributes().getRef());
    if (changesets == null) {
      return getScmInfoFromDb(component);
    }
    return getScmInfoFromReport(component, changesets);
  }

  private Optional<ScmInfo> getScmInfoFromDb(Component component) {
    if (analysisMetadataHolder.isFirstAnalysis()) {
      return Optional.absent();
    }
    LOGGER.trace("Reading SCM info from db for file '{}'", component);

    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<Iterable<DbFileSources.Line>> linesOpt = sourceService.getLines(dbSession, component.getUuid(), 1, Integer.MAX_VALUE);
      if (linesOpt.isPresent()) {
        return DbScmInfo.create(component, linesOpt.get());
      }
      return Optional.absent();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static Optional<ScmInfo> getScmInfoFromReport(Component component, BatchReport.Changesets changesets) {
    LOGGER.trace("Reading SCM info from report for file '{}'", component);
    return Optional.<ScmInfo>of(new ReportScmInfo(changesets));
  }
}
