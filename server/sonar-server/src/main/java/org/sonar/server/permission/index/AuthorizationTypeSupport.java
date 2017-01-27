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
package org.sonar.server.permission.index;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.Set;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.server.es.NewIndex;
import org.sonar.server.user.UserSession;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@ServerSide
@ComputeEngineSide
public class AuthorizationTypeSupport {

  public static final String TYPE_AUTHORIZATION = "authorization";
  public static final String FIELD_GROUP_IDS = "groupIds";
  public static final String FIELD_GROUP_NAMES = "groupNames";
  public static final String FIELD_USER_LOGINS = "users";
  public static final String FIELD_UPDATED_AT = "updatedAt";

  /**
   * When true, then anybody can access to the project. In that case
   * it's useless to store granted groups and users. The related
   * fields are empty.
   */
  public static final String FIELD_ALLOW_ANYONE = "allowAnyone";

  private final UserSession userSession;

  public AuthorizationTypeSupport(UserSession userSession) {
    this.userSession = userSession;
  }

  /**
   * Creates a type that requires to verify that user has the read permission
   * when searching for documents.
   * It relies on a parent type named "authorization" that is automatically
   * populated by {@link org.sonar.server.permission.index.PermissionIndexer}.
   *
   * Both types {@code typeName} and "authorization" are created. Documents
   * must be created with _parent and _routing having the parent uuid as values.
   *
   * @see NewIndex#createTypeRequiringProjectAuthorization(String)
   */
  public static NewIndex.NewIndexType createTypeRequiringProjectAuthorization(NewIndex newIndex, String typeName) {
    NewIndex.NewIndexType type = newIndex.createType(typeName);
    type.setAttribute("_parent", ImmutableMap.of("type", TYPE_AUTHORIZATION));
    type.setAttribute("_routing", ImmutableMap.of("required", "true"));

    NewIndex.NewIndexType authType = newIndex.createType(TYPE_AUTHORIZATION);
    authType.setAttribute("_routing", ImmutableMap.of("required", "true"));
    authType.createDateTimeField(FIELD_UPDATED_AT);
    authType.createLongField(FIELD_GROUP_IDS);
    authType.stringFieldBuilder(FIELD_GROUP_NAMES).disableNorms().build();
    authType.stringFieldBuilder(FIELD_USER_LOGINS).disableNorms().build();
    authType.createBooleanField(FIELD_ALLOW_ANYONE);
    authType.setEnableSource(false);
    return type;
  }

  /**
   * Build a filter to restrict query to the documents on which
   * user has read access.
   */
  public QueryBuilder createQueryFilter() {
    Integer userLogin = userSession.getUserId();
    Set<String> userGroupNames = userSession.getUserGroups();
    BoolQueryBuilder filter = boolQuery();

    // anyone
    filter.should(QueryBuilders.termQuery(FIELD_ALLOW_ANYONE, true));

    // users
    Optional.ofNullable(userLogin)
      .map(Integer::longValue)
      .ifPresent(userId -> filter.should(termQuery(FIELD_USER_LOGINS, userId)));

    // groups
    userGroupNames.forEach(
      group -> filter.should(termQuery(FIELD_GROUP_NAMES, group)));

    return QueryBuilders.hasParentQuery(TYPE_AUTHORIZATION,
      QueryBuilders.boolQuery().filter(filter));
  }
}
