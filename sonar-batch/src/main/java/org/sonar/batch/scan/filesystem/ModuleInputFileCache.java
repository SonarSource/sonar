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
package org.sonar.batch.scan.filesystem;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.RelativePathPredicate;

public class ModuleInputFileCache extends DefaultFileSystem.Cache implements BatchComponent {

  private final String moduleKey;
  private final InputPathCache projectCache;

  public ModuleInputFileCache(ProjectDefinition projectDef, InputPathCache projectCache) {
    this.moduleKey = projectDef.getKeyWithBranch();
    this.projectCache = projectCache;
  }

  @Override
  protected Iterable<InputFile> inputFiles() {
    return projectCache.filesByModule(moduleKey);
  }

  @Override
  protected InputFile inputFile(RelativePathPredicate predicate) {
    return projectCache.getFile(moduleKey, predicate.path());
  }

  @Override
  protected InputDir inputDir(String relativePath) {
    return projectCache.getDir(moduleKey, relativePath);
  }

  @Override
  protected void doAdd(InputFile inputFile) {
    projectCache.put(moduleKey, inputFile);
  }

  @Override
  protected void doAdd(InputDir inputDir) {
    projectCache.put(moduleKey, inputDir);
  }
}
