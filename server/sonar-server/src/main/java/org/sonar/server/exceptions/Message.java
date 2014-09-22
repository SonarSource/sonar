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
package org.sonar.server.exceptions;

import com.google.common.base.Objects;
import org.apache.commons.lang.StringUtils;

public class Message {

  private final String key;
  private final Object[] params;

  private Message(String key, Object[] params) {
    this.key = key;
    this.params = params;
  }

  public String getKey() {
    return key;
  }

  public Object[] getParams() {
    return params;
  }

  public static Message of(String l10nKey, Object... l10nParams) {
    return new Message(StringUtils.defaultString(l10nKey), l10nParams);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("key", key)
      .add("params", params)
      .toString();
  }
}
