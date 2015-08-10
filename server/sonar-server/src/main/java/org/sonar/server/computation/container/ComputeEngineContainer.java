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
package org.sonar.server.computation.container;

import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.computation.ReportQueue.Item;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.step.ComputationStep;

/**
 * The Compute Engine container. Created for a specific parent {@link ComponentContainer} and a specific {@link Item}.
 */
public interface ComputeEngineContainer {
  Item getItem();

  ComponentContainer getParent();

  /**
   * Process the current {@link Item}
   */
  void process();

  /**
   * Clean's up resources after process has been called and has returned.
   */
  void cleanup();

  /**
   */
  <T extends ComputationStep> T getStep(Class<T> type);

  <T extends ComponentVisitor> T getComponentVisitor(Class<T> type);


}
