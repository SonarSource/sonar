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
package org.sonar.batch.repository;

import com.google.common.base.Function;
import com.google.common.io.InputSupplier;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.protocol.input.BatchInput.PreviousIssue;

import java.io.IOException;
import java.io.InputStream;

public class DefaultPreviousIssuesLoader implements PreviousIssuesLoader {

  private final ServerClient serverClient;

  public DefaultPreviousIssuesLoader(ServerClient serverClient) {
    this.serverClient = serverClient;
  }

  @Override
  public void load(ProjectReactor reactor, Function<PreviousIssue, Void> consumer) {
    InputSupplier<InputStream> request = serverClient.doRequest("/batch/issues?key=" + ServerClient.encodeForUrl(reactor.getRoot().getKeyWithBranch()), "GET", null);
    try (InputStream is = request.getInput()) {
      PreviousIssue previousIssue = PreviousIssue.parseDelimitedFrom(is);
      while (previousIssue != null) {
        consumer.apply(previousIssue);
        previousIssue = PreviousIssue.parseDelimitedFrom(is);
      }
    } catch (HttpDownloader.HttpException e) {
      throw serverClient.handleHttpException(e);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get previous issues", e);
    }
  }

}
