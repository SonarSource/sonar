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
package org.sonar.batch.mediumtest;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.dependency.internal.DefaultDependency;
import org.sonar.api.batch.sensor.duplication.Duplication;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.highlighting.internal.SyntaxHighlightingRule;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.Measure;
import org.sonar.api.source.Symbol;
import org.sonar.batch.dependency.DependencyCache;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.highlighting.SyntaxHighlightingData;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.scan.ProjectScanContainer;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.batch.symbol.SymbolData;
import org.sonar.core.source.SnapshotDataTypes;

import javax.annotation.CheckForNull;

import java.io.Serializable;
import java.util.*;

public class TaskResult implements org.sonar.batch.mediumtest.ScanTaskObserver {

  private static final Logger LOG = LoggerFactory.getLogger(TaskResult.class);

  private List<Issue> issues = new ArrayList<>();
  private List<org.sonar.api.batch.sensor.measure.Measure> measures = new ArrayList<>();
  private Map<String, List<Duplication>> duplications = new HashMap<>();
  private Map<String, InputFile> inputFiles = new HashMap<>();
  private Map<String, InputDir> inputDirs = new HashMap<>();
  private Map<InputFile, SyntaxHighlightingData> highlightingPerFile = new HashMap<>();
  private Map<InputFile, SymbolData> symbolTablePerFile = new HashMap<>();
  private Map<String, Map<String, Integer>> dependencies = new HashMap<>();

  @Override
  public void scanTaskCompleted(ProjectScanContainer container) {
    LOG.info("Store analysis results in memory for later assertions in medium test");
    for (DefaultIssue issue : container.getComponentByType(IssueCache.class).all()) {
      issues.add(issue);
    }

    storeFs(container);

    storeMeasures(container);

    storeComponentData(container);
    storeDuplication(container);
    // storeTestCases(container);
    // storeCoveragePerTest(container);
    storeDependencies(container);

  }

  private void storeMeasures(ProjectScanContainer container) {
    InputPathCache inputFileCache = container.getComponentByType(InputPathCache.class);
    for (Entry<Measure> measureEntry : container.getComponentByType(MeasureCache.class).entries()) {
      String componentKey = measureEntry.key()[0].toString();
      InputFile file = inputFileCache.getFile(StringUtils.substringBeforeLast(componentKey, ":"), StringUtils.substringAfterLast(componentKey, ":"));
      Measure oldMeasure = measureEntry.value();
      DefaultMeasure<Serializable> newMeasure = new DefaultMeasure<>().forMetric(oldMeasure.getMetric());
      if (file != null) {
        newMeasure.onFile(file);
      } else {
        newMeasure.onProject();
      }
      newMeasure.withValue(oldMeasure.value());
      measures.add(newMeasure);
    }
  }

  private void storeDuplication(ProjectScanContainer container) {
    DuplicationCache duplicationCache = container.getComponentByType(DuplicationCache.class);
    for (String effectiveKey : duplicationCache.componentKeys()) {
      duplications.put(effectiveKey, Lists.<Duplication>newArrayList(duplicationCache.byComponent(effectiveKey)));
    }
  }

  private void storeComponentData(ProjectScanContainer container) {
    ComponentDataCache componentDataCache = container.getComponentByType(ComponentDataCache.class);
    for (InputFile file : inputFiles.values()) {
      SyntaxHighlightingData highlighting = componentDataCache.getData(((DefaultInputFile) file).key(), SnapshotDataTypes.SYNTAX_HIGHLIGHTING);
      if (highlighting != null) {
        highlightingPerFile.put(file, highlighting);
      }
      SymbolData symbolTable = componentDataCache.getData(((DefaultInputFile) file).key(), SnapshotDataTypes.SYMBOL_HIGHLIGHTING);
      if (symbolTable != null) {
        symbolTablePerFile.put(file, symbolTable);
      }
    }
  }

  private void storeFs(ProjectScanContainer container) {
    InputPathCache inputFileCache = container.getComponentByType(InputPathCache.class);
    for (InputFile inputPath : inputFileCache.allFiles()) {
      inputFiles.put(inputPath.relativePath(), inputPath);
    }
    for (InputDir inputPath : inputFileCache.allDirs()) {
      inputDirs.put(inputPath.relativePath(), inputPath);
    }
  }

  private void storeDependencies(ProjectScanContainer container) {
    DependencyCache dependencyCache = container.getComponentByType(DependencyCache.class);
    for (Entry<DefaultDependency> entry : dependencyCache.entries()) {
      String fromKey = entry.key()[1].toString();
      String toKey = entry.key()[2].toString();
      if (!dependencies.containsKey(fromKey)) {
        dependencies.put(fromKey, new HashMap<String, Integer>());
      }
      dependencies.get(fromKey).put(toKey, entry.value().weight());
    }
  }

  public List<Issue> issues() {
    return issues;
  }

  public List<org.sonar.api.batch.sensor.measure.Measure> measures() {
    return measures;
  }

  public Collection<InputFile> inputFiles() {
    return inputFiles.values();
  }

  @CheckForNull
  public InputFile inputFile(String relativePath) {
    return inputFiles.get(relativePath);
  }

  public Collection<InputDir> inputDirs() {
    return inputDirs.values();
  }

  @CheckForNull
  public InputDir inputDir(String relativePath) {
    return inputDirs.get(relativePath);
  }

  public List<Duplication> duplicationsFor(InputFile inputFile) {
    return duplications.get(((DefaultInputFile) inputFile).key());
  }

  /**
   * Get highlighting types at a given position in an inputfile
   * @param charIndex 0-based offset in file
   */
  public List<TypeOfText> highlightingTypeFor(InputFile file, int charIndex) {
    SyntaxHighlightingData syntaxHighlightingData = highlightingPerFile.get(file);
    if (syntaxHighlightingData == null) {
      return Collections.emptyList();
    }
    List<TypeOfText> result = new ArrayList<TypeOfText>();
    for (SyntaxHighlightingRule sortedRule : syntaxHighlightingData.syntaxHighlightingRuleSet()) {
      if (sortedRule.getStartPosition() <= charIndex && sortedRule.getEndPosition() > charIndex) {
        result.add(sortedRule.getTextType());
      }
    }
    return result;
  }

  /**
   * Get list of all positions of a symbol in an inputfile
   * @param symbolStartOffset 0-based start offset for the symbol in file
   * @param symbolEndOffset 0-based end offset for the symbol in file
   */
  @CheckForNull
  public Set<Integer> symbolReferencesFor(InputFile file, int symbolStartOffset, int symbolEndOffset) {
    SymbolData data = symbolTablePerFile.get(file);
    if (data == null) {
      return null;
    }
    for (Symbol symbol : data.referencesBySymbol().keySet()) {
      if (symbol.getDeclarationStartOffset() == symbolStartOffset && symbol.getDeclarationEndOffset() == symbolEndOffset) {
        return data.referencesBySymbol().get(symbol);
      }
    }
    return null;
  }

  /**
   * @return null if no dependency else return dependency weight.
   */
  @CheckForNull
  public Integer dependencyWeight(InputFile from, InputFile to) {
    String fromKey = ((DefaultInputFile) from).key();
    String toKey = ((DefaultInputFile) to).key();
    return dependencies.containsKey(fromKey) ? dependencies.get(fromKey).get(toKey) : null;
  }
}
