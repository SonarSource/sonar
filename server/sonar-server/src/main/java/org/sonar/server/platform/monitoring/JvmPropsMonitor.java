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
package org.sonar.server.platform.monitoring;

import com.google.common.base.Optional;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class JvmPropsMonitor implements Monitor {
  @Override
  public String name() {
    return "JvmProperties";
  }

  @Override
  public Optional<Map<String, Object>> attributes() {
    Map<String, Object> sortedProps = new TreeMap<>();
    for (Map.Entry<Object, Object> systemProp : System.getProperties().entrySet()) {
      sortedProps.put(Objects.toString(systemProp.getKey()), Objects.toString(systemProp.getValue()));
    }
    return Optional.of(sortedProps);
  }
}
