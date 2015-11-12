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

package org.sonar.server.computation.step;

import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.duplication.DuplicationUnitDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;

import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Persist cross project duplications text blocks into DUPLICATIONS_INDEX table
 */
public class PersistCrossProjectDuplicationIndexStep implements ComputationStep {

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbIdsRepository dbIdsRepository;

  public PersistCrossProjectDuplicationIndexStep(DbClient dbClient, DbIdsRepository dbIdsRepository, TreeRootHolder treeRootHolder, BatchReportReader reportReader,
    AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbIdsRepository = dbIdsRepository;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(true);
    try {
      if (analysisMetadataHolder.isCrossProjectDuplicationEnabled() && analysisMetadataHolder.getBranch() == null) {
        Component project = treeRootHolder.getRoot();
        long projectSnapshotId = dbIdsRepository.getSnapshotId(project);
        new DepthTraversalTypeAwareCrawler(new DuplicationVisitor(session, projectSnapshotId)).visit(project);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private class DuplicationVisitor extends TypeAwareVisitorAdapter {

    private final DbSession session;
    private final long projectSnapshotId;

    private DuplicationVisitor(DbSession session, long projectSnapshotId) {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
      this.session = session;
      this.projectSnapshotId = projectSnapshotId;
    }

    @Override
    public void visitFile(Component file) {
      visitComponent(file);
    }

    private void visitComponent(Component component) {
      int indexInFile = 0;
      try (CloseableIterator<BatchReport.CpdTextBlock> blocks = reportReader.readCpdTextBlocks(component.getReportAttributes().getRef())) {
        while (blocks.hasNext()) {
          BatchReport.CpdTextBlock block = blocks.next();
          dbClient.duplicationDao().insert(session, new DuplicationUnitDto()
            .setHash(block.getHash())
            .setStartLine(block.getStartLine())
            .setEndLine(block.getEndLine())
            .setIndexInFile(indexInFile++)
            .setSnapshotId(dbIdsRepository.getSnapshotId(component))
            .setProjectSnapshotId(projectSnapshotId)
            );
        }
      }
    }
  }

  @Override
  public String getDescription() {
    return "Persist cross project duplications index";
  }

}
