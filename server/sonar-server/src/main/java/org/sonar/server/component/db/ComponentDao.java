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
package org.sonar.server.component.db;

import org.sonar.api.ServerComponent;
import org.sonar.api.utils.System2;
import org.sonar.core.component.AuthorizedComponentDto;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.db.ComponentMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;

import java.util.List;

/**
 * @since 4.3
 */
public class ComponentDao extends BaseDao<ComponentMapper, ComponentDto, String> implements ServerComponent, DaoComponent {

  public ComponentDao(System2 system) {
    super(ComponentMapper.class, system);
  }

  public ComponentDto getById(Long id, DbSession session) {
    ComponentDto componentDto = getNullableById(id, session);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Project with id '%s' not found", id));
    }
    return componentDto;
  }

  @CheckForNull
  public ComponentDto getNullableById(Long id, DbSession session) {
    return mapper(session).selectById(id);
  }

  public boolean existsById(Long id, DbSession session) {
    return mapper(session).countById(id) > 0;
  }

  /**
   * Return null only if the component does not exists.
   * If the component if a root project, it will return itself.
   */
  @CheckForNull
  public ComponentDto getNullableRootProjectByKey(String componentKey, DbSession session) {
    return mapper(session).selectRootProjectByKey(componentKey);
  }

  public ComponentDto getRootProjectByKey(String componentKey, DbSession session) {
    ComponentDto componentDto = getNullableRootProjectByKey(componentKey, session);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Root project for project '%s' not found", componentKey));
    }
    return componentDto;
  }

  @CheckForNull
  public ComponentDto getParentModuleByKey(String componentKey, DbSession session) {
    return mapper(session).selectParentModuleByKey(componentKey);
  }

  public List<ComponentDto> findModulesByProject(String projectKey, DbSession session) {
    return mapper(session).findModulesByProject(projectKey);
  }

  @CheckForNull
  public AuthorizedComponentDto getNullableAuthorizedComponentById(Long id, DbSession session) {
    return mapper(session).selectAuthorizedComponentById(id);
  }

  public AuthorizedComponentDto getAuthorizedComponentById(Long id, DbSession session) {
    AuthorizedComponentDto componentDto = getNullableAuthorizedComponentById(id, session);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Project with id '%s' not found", id));
    }
    return componentDto;
  }

  @CheckForNull
  public AuthorizedComponentDto getNullableAuthorizedComponentByKey(String key, DbSession session) {
    return mapper(session).selectAuthorizedComponentByKey(key);
  }

  public AuthorizedComponentDto getAuthorizedComponentByKey(String key, DbSession session) {
    AuthorizedComponentDto componentDto = getNullableAuthorizedComponentByKey(key, session);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Project with key '%s' not found", key));
    }
    return componentDto;
  }

  @Override
  @CheckForNull
  protected ComponentDto doGetNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  @Override
  protected ComponentDto doInsert(DbSession session, ComponentDto item) {
    mapper(session).insert(item);
    return item;
  }
}
