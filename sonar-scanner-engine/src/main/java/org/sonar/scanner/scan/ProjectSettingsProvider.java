/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.scanner.scan;

import java.util.LinkedHashMap;
import java.util.Map;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.bootstrap.GlobalMode;
import org.sonar.scanner.repository.settings.SettingsLoader;

public class ProjectSettingsProvider extends ProviderAdapter {

  private ProjectSettings projectSettings;

  public ProjectSettings provide(ProjectReactor reactor, GlobalConfiguration globalSettings, SettingsLoader settingsLoader, GlobalMode mode) {
    if (projectSettings == null) {

      Map<String, String> settings = new LinkedHashMap<>();
      settings.putAll(globalSettings.getProperties());
      settings.putAll(settingsLoader.load(reactor.getRoot().getKeyWithBranch()));
      settings.putAll(reactor.getRoot().properties());

      projectSettings = new ProjectSettings(globalSettings.getDefinitions(), globalSettings.getEncryption(), mode, settings);
    }
    return projectSettings;
  }
}
