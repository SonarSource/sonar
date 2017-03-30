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
package org.sonar.server.computation.queue;

import java.io.IOException;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.server.computation.taskprocessor.CeProcessingScheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CeQueueInitializerTest {

  private Server server = mock(Server.class);
  private CeProcessingScheduler scheduler = mock(CeProcessingScheduler.class);
  private CeQueueInitializer underTest = new CeQueueInitializer(scheduler);

  @Test
  public void clean_queue_then_start_scheduler_of_workers() throws IOException {
    underTest.onServerStart(server);

    verify(scheduler).startScheduling();
  }

  @Test
  public void onServerStart_has_no_effect_if_called_twice_to_support_medium_test_doing_startup_tasks_multiple_times() {

    underTest.onServerStart(server);

    reset(scheduler);

    underTest.onServerStart(server);

    verifyZeroInteractions(scheduler);

  }
}
