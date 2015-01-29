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
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.FilePathWithHashDto;
import org.sonar.core.component.UuidWithProjectUuidDto;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;

/**
 * @since 4.3
 */
public interface ComponentMapper {

  /**
   * Warning, projectId is always null
   */
  @CheckForNull
  ComponentDto selectByKey(String key);

  /**
   * Warning, projectId is always null
   */
  @CheckForNull
  ComponentDto selectById(long id);

  /**
   * Warning, projectId is always null
   */
  @CheckForNull
  ComponentDto selectByUuid(String uuid);

  /**
   * Return direct modules from a project/module
   */
  List<ComponentDto> findModulesByProject(@Param("projectKey") String projectKey);

  /**
   * Return sub project of component keys
   */
  List<ComponentDto> findSubProjectsByComponentUuids(@Param("uuids") Collection<String> uuids);

  List<ComponentDto> findByKeys(@Param("keys") Collection<String> keys);

  List<ComponentDto> findByUuids(@Param("uuids") Collection<String> uuids);

  List<String> selectExistingUuids(@Param("uuids") Collection<String> uuids);

  /**
   * Return all project (PRJ/TRK) uuids
   */
  List<String> findProjectUuids();

  /**
   * Return all modules tree (child, grand child, etc. including itself) from a module uuid
   */
  List<ComponentDto> selectModulesTree(@Param("moduleUuid") String moduleUuid, @Param(value = "scope") String scope);

  /**
   * Return all files children from a module uuid
   */
  List<FilePathWithHashDto> selectModuleFilesTree(@Param("moduleUuid") String moduleUuid, @Param(value = "scope") String scope);

  /**
   * Return uuids and project uuids from list of qualifiers
   *
   * It's using a join on snapshots in order to use he indexed columns snapshots.qualifier
   */
  List<UuidWithProjectUuidDto> selectUuidsForQualifiers(@Param("qualifiers") String... qualifiers);

  /**
   * Return technical projects from a view or a sub-view
   */
  List<String> selectProjectsFromView(@Param("viewUuidLikeQuery") String viewUuidLikeQuery, @Param("projectViewUuid") String projectViewUuid,
    @Param("subViewQualifier") String subViewQualifier);

  long countById(long id);

  void insert(ComponentDto rule);

  void deleteByKey(String key);
}
