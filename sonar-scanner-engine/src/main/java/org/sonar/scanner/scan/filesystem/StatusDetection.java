/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.scan.filesystem;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ServerSideProjectData;

class StatusDetection {

  private final ServerSideProjectData serverSideProjectData;

  StatusDetection(ServerSideProjectData serverSideProjectData) {
    this.serverSideProjectData = serverSideProjectData;
  }

  InputFile.Status status(String projectKey, String relativePath, String hash) {
    FileData fileDataPerPath = serverSideProjectData.fileData(projectKey, relativePath);
    if (fileDataPerPath == null) {
      return InputFile.Status.ADDED;
    }
    String previousHash = fileDataPerPath.hash();
    if (StringUtils.equals(hash, previousHash)) {
      return InputFile.Status.SAME;
    }
    if (StringUtils.isEmpty(previousHash)) {
      return InputFile.Status.ADDED;
    }
    return InputFile.Status.CHANGED;
  }
}
