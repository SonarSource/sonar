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
package org.sonar.core.component.db;

import org.apache.ibatis.annotations.Param;
import org.sonar.core.component.AuthorizedComponentDto;
import org.sonar.core.component.ComponentDto;

import javax.annotation.CheckForNull;

import java.util.List;

/**
 * @since 4.3
 */
public interface ComponentMapper {

  @CheckForNull
  ComponentDto selectByKey(String key);

  @CheckForNull
  ComponentDto selectById(long id);

  @CheckForNull
  ComponentDto selectRootProjectByKey(String key);

  @CheckForNull
  ComponentDto selectParentModuleByKey(String key);

  /**
   * Return direct modules from a project/module
   */
  List<ComponentDto> findModulesByProject(@Param("projectKey") String projectKey);

  long countById(long id);

  @CheckForNull
  AuthorizedComponentDto selectAuthorizedComponentById(long id);

  AuthorizedComponentDto selectAuthorizedComponentByKey(String key);

  void insert(ComponentDto rule);
}
