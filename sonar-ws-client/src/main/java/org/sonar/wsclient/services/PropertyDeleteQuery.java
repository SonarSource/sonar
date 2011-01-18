/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.services;

/**
 * @since 2.2
 */
public class PropertyDeleteQuery extends DeleteQuery<Property> {

  private String key;
  private String resourceKeyOrId;

  public PropertyDeleteQuery(String key) {
    this.key = key;
  }

  public PropertyDeleteQuery(String key, String resourceKeyOrId) {
    this.key = key;
    this.resourceKeyOrId = resourceKeyOrId;
  }

  public PropertyDeleteQuery(Property property) {
    this.key = property.getKey();
  }

  public String getKey() {
    return key;
  }

  public PropertyDeleteQuery setKey(String key) {
    this.key = key;
    return this;
  }

  public String getResourceKeyOrId() {
    return resourceKeyOrId;
  }
  
  public PropertyDeleteQuery setResourceKeyOrId(String resourceKeyOrId) {
    this.resourceKeyOrId = resourceKeyOrId;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder();
    url.append(PropertyQuery.BASE_URL);
    url.append("/").append(key);
    url.append('?');
    appendUrlParameter(url, "resource", resourceKeyOrId);
    return url.toString();
  }
}
