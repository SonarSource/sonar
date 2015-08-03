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
package org.sonar.batch.issue.tracking;

import com.google.common.base.Function;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.scan.ImmutableProjectReactor;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.core.component.ComponentKeys;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@BatchSide
public class ServerIssueRepository {

  private static final Logger LOG = Loggers.get(ServerIssueRepository.class);

  private final Caches caches;
  private Cache<ServerIssue> issuesCache;
  private final ServerIssuesLoader previousIssuesLoader;
  private final ImmutableProjectReactor reactor;
  private final BatchComponentCache resourceCache;
  private final AnalysisMode analysisMode;
  private final InputPathCache inputPathCache;

  public ServerIssueRepository(Caches caches, ServerIssuesLoader previousIssuesLoader, ImmutableProjectReactor reactor, BatchComponentCache resourceCache,
    AnalysisMode analysisMode, InputPathCache inputPathCache) {
    this.caches = caches;
    this.previousIssuesLoader = previousIssuesLoader;
    this.reactor = reactor;
    this.resourceCache = resourceCache;
    this.analysisMode = analysisMode;
    this.inputPathCache = inputPathCache;
  }

  public void load() {
    if (analysisMode.isIncremental()) {
      return;
    }
    Profiler profiler = Profiler.create(LOG).startInfo("Load server issues");
    this.issuesCache = caches.createCache("previousIssues");
    caches.registerValueCoder(ServerIssue.class, new ServerIssueValueCoder());
    boolean fromCache = previousIssuesLoader.load(reactor.getRoot().getKeyWithBranch(), new SaveIssueConsumer(), false);
    stopDebug(profiler, "Load server issues", fromCache);
  }

  public Iterable<ServerIssue> byComponent(BatchComponent component) {
    if (analysisMode.isIncremental()) {
      if (!component.isFile()) {
        throw new UnsupportedOperationException("Incremental mode should only get issues on files");
      }
      InputFile inputFile = (InputFile) component.inputComponent();
      if (inputFile.status() == Status.ADDED) {
        return Collections.emptyList();
      }
      Profiler profiler = Profiler.create(LOG).startInfo("Load server issues for " + component.resource().getPath());
      ServerIssueConsumer consumer = new ServerIssueConsumer();
      boolean fromCache = previousIssuesLoader.load(component.key(), consumer, true);
      stopDebug(profiler, "Load server issues for " + component.resource().getPath(), fromCache);
      return consumer.issueList;
    } else {
      return issuesCache.values(component.batchId());
    }
  }

  private void stopDebug(Profiler profiler, String msg, boolean fromCache) {
    if (fromCache) {
      profiler.stopDebug(msg + " (done from cache)");
    } else {
      profiler.stopDebug(msg + " (done)");
    }
  }

  private class SaveIssueConsumer implements Function<ServerIssue, Void> {

    @Override
    public Void apply(@Nullable ServerIssue issue) {
      if (issue == null) {
        return null;
      }
      String componentKey = ComponentKeys.createEffectiveKey(issue.getModuleKey(), issue.hasPath() ? issue.getPath() : null);
      BatchComponent r = resourceCache.get(componentKey);
      if (r == null) {
        // Deleted resource
        issuesCache.put(0, issue.getKey(), issue);
      } else {
        issuesCache.put(r.batchId(), issue.getKey(), issue);
      }
      return null;
    }
  }

  private static class ServerIssueConsumer implements Function<ServerIssue, Void> {
    List<ServerIssue> issueList = new LinkedList<>();

    @Override
    public Void apply(@Nullable ServerIssue issue) {
      if (issue == null) {
        return null;
      }
      issueList.add(issue);
      return null;
    }
  }

  public Iterable<ServerIssue> issuesOnMissingComponents() {
    if (analysisMode.isIncremental()) {
      throw new UnsupportedOperationException("Only issues of analyzed components are loaded in incremental mode");
    }
    return issuesCache.values(0);
  }
}
