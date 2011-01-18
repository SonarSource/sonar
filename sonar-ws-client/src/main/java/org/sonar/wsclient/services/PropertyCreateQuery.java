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
 * @since 2.5
 */
public class PropertyCreateQuery extends CreateQuery<Property> {

  private String key;
  private String value;

  public PropertyCreateQuery() {
  }

  public PropertyCreateQuery(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public PropertyCreateQuery(Property property) {
    this.key = property.getKey();
    this.value = property.getValue();
  }

  public String getKey() {
    return key;
  }

  public PropertyCreateQuery setKey(String key) {
    this.key = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public PropertyCreateQuery setValue(String value) {
    this.value = value;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder sb = new StringBuilder();
    sb.append(PropertyQuery.BASE_URL);
    sb.append('?');
    appendUrlParameter(sb, "key", key);
    return sb.toString();
  }
  
  /**
   * Property value is transmitted through request body as content may
   * exceed URL size allowed by the server.
   */
  @Override
  public String getBody() {
	return value;
  }

  @Override
  public Class<Property> getModelClass() {
    return Property.class;
  }
}
