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
package org.sonar.batch.protocol.output;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.FileStream;
import org.sonar.batch.protocol.ProtobufUtil;
import org.sonar.batch.protocol.ReportStream;
import org.sonar.batch.protocol.output.BatchReport.Range;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class BatchReportWriterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void create_dir_if_does_not_exist() throws Exception {
    File dir = temp.newFolder();
    FileUtils.deleteQuietly(dir);

    new BatchReportWriter(dir);

    assertThat(dir).isDirectory().exists();
  }

  @Test
  public void write_metadata() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Metadata.Builder metadata = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1);
    writer.writeMetadata(metadata.build());

    BatchReport.Metadata read = ProtobufUtil.readFile(writer.getFileStructure().metadataFile(), BatchReport.Metadata.PARSER);
    assertThat(read.getAnalysisDate()).isEqualTo(15000000L);
    assertThat(read.getProjectKey()).isEqualTo("PROJECT_A");
    assertThat(read.getRootComponentRef()).isEqualTo(1);
  }

  @Test
  public void write_component() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    // no data yet
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isFalse();

    // write data
    BatchReport.Component.Builder component = BatchReport.Component.newBuilder()
      .setRef(1)
      .setLanguage("java")
      .setPath("src/Foo.java")
      .setUuid("UUID_A")
      .setType(Constants.ComponentType.FILE)
      .setIsTest(false)
      .addChildRef(5)
      .addChildRef(42);
    writer.writeComponent(component.build());

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.COMPONENT, 1);
    assertThat(file).exists().isFile();
    BatchReport.Component read = ProtobufUtil.readFile(file, BatchReport.Component.PARSER);
    assertThat(read.getRef()).isEqualTo(1);
    assertThat(read.getChildRefList()).containsOnly(5, 42);
    assertThat(read.hasName()).isFalse();
    assertThat(read.getIsTest()).isFalse();
    assertThat(read.getUuid()).isEqualTo("UUID_A");
  }

  @Test
  public void write_issues() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    // no data yet
    assertThat(writer.hasComponentData(FileStructure.Domain.ISSUES, 1)).isFalse();

    // write data
    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setUuid("ISSUE_A")
      .setLine(50)
      .setMsg("the message")
      .build();

    writer.writeComponentIssues(1, Arrays.asList(issue));

    assertThat(writer.hasComponentData(FileStructure.Domain.ISSUES, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.ISSUES, 1);
    assertThat(file).exists().isFile();
    BatchReport.Issues read = ProtobufUtil.readFile(file, BatchReport.Issues.PARSER);
    assertThat(read.getComponentRef()).isEqualTo(1);
    assertThat(read.hasComponentUuid()).isFalse();
    assertThat(read.getIssueCount()).isEqualTo(1);
  }

  @Test
  public void write_issues_of_deleted_component() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    // no data yet
    assertThat(writer.hasComponentData(FileStructure.Domain.ISSUES_ON_DELETED, 1)).isFalse();

    // write data
    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setUuid("ISSUE_A")
      .setLine(50)
      .setMsg("the message")
      .build();

    writer.writeDeletedComponentIssues(1, "componentUuid", Arrays.asList(issue));

    assertThat(writer.hasComponentData(FileStructure.Domain.ISSUES_ON_DELETED, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.ISSUES_ON_DELETED, 1);
    assertThat(file).exists().isFile();
    BatchReport.Issues read = ProtobufUtil.readFile(file, BatchReport.Issues.PARSER);
    assertThat(read.getComponentRef()).isEqualTo(1);
    assertThat(read.getComponentUuid()).isEqualTo("componentUuid");
    assertThat(read.getIssueCount()).isEqualTo(1);
  }

  @Test
  public void write_measures() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    assertThat(writer.hasComponentData(FileStructure.Domain.MEASURES, 1)).isFalse();

    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setStringValue("text-value")
      .setDoubleValue(2.5d)
      .setValueType(Constants.MeasureValueType.DOUBLE)
      .setDescription("description")
      .build();

    writer.writeComponentMeasures(1, Arrays.asList(measure));

    assertThat(writer.hasComponentData(FileStructure.Domain.MEASURES, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.MEASURES, 1);
    assertThat(file).exists().isFile();
    BatchReport.Measures measures = ProtobufUtil.readFile(file, BatchReport.Measures.PARSER);
    assertThat(measures.getComponentRef()).isEqualTo(1);
    assertThat(measures.getMeasureCount()).isEqualTo(1);
    assertThat(measures.getMeasure(0).getStringValue()).isEqualTo("text-value");
    assertThat(measures.getMeasure(0).getDoubleValue()).isEqualTo(2.5d);
    assertThat(measures.getMeasure(0).getValueType()).isEqualTo(Constants.MeasureValueType.DOUBLE);
    assertThat(measures.getMeasure(0).getDescription()).isEqualTo("description");
  }

  @Test
  public void write_scm() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    assertThat(writer.hasComponentData(FileStructure.Domain.SCM, 1)).isFalse();

    BatchReport.Scm scm = BatchReport.Scm.newBuilder()
      .setComponentRef(1)
      .addChangesetIndexByLine(0)
      .addChangeset(BatchReport.Scm.Changeset.newBuilder()
        .setRevision("123-456-789")
        .setAuthor("author")
        .setDate(123_456_789L))
      .build();

    writer.writeComponentScm(scm);

    assertThat(writer.hasComponentData(FileStructure.Domain.SCM, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.SCM, 1);
    assertThat(file).exists().isFile();
    BatchReport.Scm read = ProtobufUtil.readFile(file, BatchReport.Scm.PARSER);
    assertThat(read.getComponentRef()).isEqualTo(1);
    assertThat(read.getChangesetCount()).isEqualTo(1);
    assertThat(read.getChangesetList()).hasSize(1);
    assertThat(read.getChangeset(0).getDate()).isEqualTo(123_456_789L);
  }

  @Test
  public void write_duplications() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    assertThat(writer.hasComponentData(FileStructure.Domain.DUPLICATIONS, 1)).isFalse();

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileKey("COMPONENT_A")
        .setOtherFileRef(2)
        .setRange(Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(1, Arrays.asList(duplication));

    assertThat(writer.hasComponentData(FileStructure.Domain.DUPLICATIONS, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.DUPLICATIONS, 1);
    assertThat(file).exists().isFile();
    BatchReport.Duplications duplications = ProtobufUtil.readFile(file, BatchReport.Duplications.PARSER);
    assertThat(duplications.getComponentRef()).isEqualTo(1);
    assertThat(duplications.getDuplicationList()).hasSize(1);
    assertThat(duplications.getDuplication(0).getOriginPosition()).isNotNull();
    assertThat(duplications.getDuplication(0).getDuplicateList()).hasSize(1);
  }

  @Test
  public void write_symbols() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    // no data yet
    assertThat(writer.hasComponentData(FileStructure.Domain.SYMBOLS, 1)).isFalse();

    // write data
    BatchReport.Symbols.Symbol symbol = BatchReport.Symbols.Symbol.newBuilder()
      .setDeclaration(BatchReport.Range.newBuilder()
        .setStartLine(1)
        .setStartOffset(3)
        .setEndLine(1)
        .setEndOffset(5)
        .build())
      .addReference(BatchReport.Range.newBuilder()
        .setStartLine(10)
        .setStartOffset(15)
        .setEndLine(11)
        .setEndOffset(2)
        .build())
      .build();

    writer.writeComponentSymbols(1, Arrays.asList(symbol));

    assertThat(writer.hasComponentData(FileStructure.Domain.SYMBOLS, 1)).isTrue();

    File file = writer.getFileStructure().fileFor(FileStructure.Domain.SYMBOLS, 1);
    assertThat(file).exists().isFile();
    BatchReport.Symbols read = ProtobufUtil.readFile(file, BatchReport.Symbols.PARSER);
    assertThat(read.getFileRef()).isEqualTo(1);
    assertThat(read.getSymbolList()).hasSize(1);
    assertThat(read.getSymbol(0).getDeclaration().getStartLine()).isEqualTo(1);
    assertThat(read.getSymbol(0).getReference(0).getStartLine()).isEqualTo(10);
  }

  @Test
  public void write_syntax_highlighting() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    assertThat(writer.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTING, 1)).isFalse();

    BatchReport.SyntaxHighlighting.HighlightingRule highlightingRule = BatchReport.SyntaxHighlighting.HighlightingRule.newBuilder()
      .setRange(BatchReport.Range.newBuilder()
        .setStartLine(1)
        .setEndLine(1)
        .build())
      .setType(Constants.HighlightingType.ANNOTATION)
      .build();
    writer.writeComponentSyntaxHighlighting(1, Arrays.asList(highlightingRule));

    assertThat(writer.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTING, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTING, 1);
    assertThat(file).exists().isFile();
    BatchReport.SyntaxHighlighting syntaxHighlighting = ProtobufUtil.readFile(file, BatchReport.SyntaxHighlighting.PARSER);
    assertThat(syntaxHighlighting.getFileRef()).isEqualTo(1);
    assertThat(syntaxHighlighting.getHighlightingRuleList()).hasSize(1);
    assertThat(syntaxHighlighting.getHighlightingRule(0).getType()).isEqualTo(Constants.HighlightingType.ANNOTATION);
  }

  @Test
  public void write_coverage() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    // no data yet
    assertThat(writer.hasComponentData(FileStructure.Domain.COVERAGE, 1)).isFalse();

    writer.writeFileCoverage(1, Arrays.asList(
      BatchReport.Coverage.newBuilder()
        .setLine(1)
        .setConditions(1)
        .setUtHits(true)
        .setItHits(false)
        .setUtCoveredConditions(1)
        .setItCoveredConditions(1)
        .setOverallCoveredConditions(1)
        .build(),
      BatchReport.Coverage.newBuilder()
        .setLine(2)
        .setConditions(5)
        .setUtHits(false)
        .setItHits(false)
        .setUtCoveredConditions(4)
        .setItCoveredConditions(5)
        .setOverallCoveredConditions(5)
        .build()
    ));

    assertThat(writer.hasComponentData(FileStructure.Domain.COVERAGE, 1)).isTrue();

    ReportStream coverageReportStream = null;
    try {
      coverageReportStream = new BatchReportReader(dir).readFileCoverage(1);
      List<BatchReport.Coverage> coverageList = newArrayList(coverageReportStream);
      assertThat(coverageList).hasSize(2);

      BatchReport.Coverage coverage = coverageList.get(0);
      assertThat(coverage.getLine()).isEqualTo(1);
      assertThat(coverage.getConditions()).isEqualTo(1);
      assertThat(coverage.getUtHits()).isTrue();
      assertThat(coverage.getItHits()).isFalse();
      assertThat(coverage.getUtCoveredConditions()).isEqualTo(1);
      assertThat(coverage.getItCoveredConditions()).isEqualTo(1);
      assertThat(coverage.getOverallCoveredConditions()).isEqualTo(1);

      coverage = coverageList.get(1);
      assertThat(coverage.getLine()).isEqualTo(2);
      assertThat(coverage.getConditions()).isEqualTo(5);
      assertThat(coverage.getUtHits()).isFalse();
      assertThat(coverage.getItHits()).isFalse();
      assertThat(coverage.getUtCoveredConditions()).isEqualTo(4);
      assertThat(coverage.getItCoveredConditions()).isEqualTo(5);
      assertThat(coverage.getOverallCoveredConditions()).isEqualTo(5);
    } finally {
     IOUtils.closeQuietly(coverageReportStream);
    }
  }

  @Test
  public void read_source_lines() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.SOURCE, 1);
    assertThat(file.exists());
    FileUtils.writeLines(file, Lists.newArrayList("line1", "line2"));

    FileStream fileStream = null;
    try {
      fileStream = new BatchReportReader(dir).readSourceLines(1);
      assertThat(fileStream).hasSize(2);
    } finally {
      fileStream.close();
    }
  }

}
