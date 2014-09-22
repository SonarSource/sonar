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
package org.sonar.server.platform.ws;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.Platform;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RestartHandlerTest {

  System2 system = mock(System2.class);

  @Test
  public void restart_if_dev_mode() throws Exception {
    Platform platform = mock(Platform.class);
    Settings settings = new Settings();
    settings.setProperty("sonar.web.dev", true);
    when(system.isOsWindows()).thenReturn(false);

    RestartHandler restartHandler = new RestartHandler(settings, platform, system);
    SystemWs ws = new SystemWs(restartHandler);

    WsTester tester = new WsTester(ws);
    tester.newPostRequest("api/system", "restart").execute();

    verify(platform).restart();
  }

  @Test
  public void fail_if_production_mode() throws Exception {
    Platform platform = mock(Platform.class);
    Settings settings = new Settings();
    RestartHandler restartHandler = new RestartHandler(settings, platform, system);
    SystemWs ws = new SystemWs(restartHandler);

    WsTester tester = new WsTester(ws);
    try {
      tester.newPostRequest("api/system", "restart").execute();
      fail();
    } catch (ForbiddenException e) {
      verifyZeroInteractions(platform);
    }
  }

  @Test
  public void fail_if_windows_java_6() throws Exception {
    Platform platform = mock(Platform.class);
    Settings settings = new Settings();
    settings.setProperty("sonar.web.dev", true);
    when(system.isOsWindows()).thenReturn(true);
    when(system.isJavaAtLeast17()).thenReturn(false);

    RestartHandler restartHandler = new RestartHandler(settings, platform, system);
    SystemWs ws = new SystemWs(restartHandler);

    WsTester tester = new WsTester(ws);
    try {
      tester.newPostRequest("api/system", "restart").execute();
      fail();
    } catch (ForbiddenException e) {
      verifyZeroInteractions(platform);
    }
  }
}
