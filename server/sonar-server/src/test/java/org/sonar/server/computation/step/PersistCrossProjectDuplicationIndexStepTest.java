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

import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.ReportComponent;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class PersistCrossProjectDuplicationIndexStepTest {

  static final int FILE_REF = 2;
  static final Component FILE = ReportComponent.builder(Component.Type.FILE, FILE_REF).build();
  static final long FILE_SNAPSHOT_ID = 11L;

  static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 1)
    .addChildren(FILE)
    .build();
  static final long PROJECT_SNAPSHOT_ID = 10L;

  static final BatchReport.CpdTextBlock CPD_TEXT_BLOCK = BatchReport.CpdTextBlock.newBuilder()
    .setHash("a8998353e96320ec")
    .setStartLine(30)
    .setEndLine(45)
    .build();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(PROJECT);

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  DbIdsRepositoryImpl dbIdsRepository = new DbIdsRepositoryImpl();

  DbClient dbClient = dbTester.getDbClient();

  ComputationStep underTest = new PersistCrossProjectDuplicationIndexStep(dbClient, dbIdsRepository, treeRootHolder, reportReader, analysisMetadataHolder);

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void persist_cpd_text_blocks() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(true)
      .setBranch(null);

    dbIdsRepository.setSnapshotId(PROJECT, PROJECT_SNAPSHOT_ID);
    dbIdsRepository.setSnapshotId(FILE, FILE_SNAPSHOT_ID);
    reportReader.putDuplicationBlocks(FILE_REF, singletonList(CPD_TEXT_BLOCK));

    underTest.execute();

    Map<String, Object> dto = dbTester.selectFirst("select hash as \"hash\", start_line as \"startLine\", end_line as \"endLine\", index_in_file as \"indexInFile\", " +
      "snapshot_id as \"snapshotId\", project_snapshot_id as \"projectSnapshotId\" from duplications_index");
    assertThat(dto.get("hash")).isEqualTo(CPD_TEXT_BLOCK.getHash());
    assertThat(dto.get("startLine")).isEqualTo(30L);
    assertThat(dto.get("endLine")).isEqualTo(45L);
    assertThat(dto.get("indexInFile")).isEqualTo(0L);
    assertThat(dto.get("snapshotId")).isEqualTo(FILE_SNAPSHOT_ID);
    assertThat(dto.get("projectSnapshotId")).isEqualTo(PROJECT_SNAPSHOT_ID);
  }

  @Test
  public void nothing_to_do_when_branch_is_used() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(true)
      .setBranch("origin/master");

    dbIdsRepository.setSnapshotId(PROJECT, PROJECT_SNAPSHOT_ID);
    dbIdsRepository.setSnapshotId(FILE, FILE_SNAPSHOT_ID);
    reportReader.putDuplicationBlocks(FILE_REF, singletonList(CPD_TEXT_BLOCK));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("duplications_index")).isEqualTo(0);
  }

  @Test
  public void nothing_to_do_when_cross_project_duplication_is_disabled() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(false)
      .setBranch(null);

    dbIdsRepository.setSnapshotId(PROJECT, PROJECT_SNAPSHOT_ID);
    dbIdsRepository.setSnapshotId(FILE, FILE_SNAPSHOT_ID);
    reportReader.putDuplicationBlocks(FILE_REF, singletonList(CPD_TEXT_BLOCK));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("duplications_index")).isEqualTo(0);
  }
}
