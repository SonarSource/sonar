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
package org.sonar.batch.index;

import com.google.common.collect.Maps;
import org.sonar.api.BatchComponent;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Library;

import java.util.Map;
import java.util.Set;

/**
 * Does not contains snapshots of {@link Library} as effectiveKey can be the same than a project.
 */
public class SnapshotCache implements BatchComponent {
  // snapshots by component key
  private final Map<String, Snapshot> snapshots = Maps.newHashMap();

  public Snapshot get(String componentKey) {
    return snapshots.get(componentKey);
  }

  public SnapshotCache put(String componentKey, Snapshot snapshot) {
    snapshots.put(componentKey, snapshot);
    return this;
  }

  public Set<Map.Entry<String, Snapshot>> snapshots() {
    return snapshots.entrySet();
  }
}
