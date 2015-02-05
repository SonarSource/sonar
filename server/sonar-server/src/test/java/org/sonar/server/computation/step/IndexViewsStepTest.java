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

package org.sonar.server.computation.step;

import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.view.index.ViewIndexer;

import static org.mockito.Mockito.*;

public class IndexViewsStepTest {

  ComputationContext context = mock(ComputationContext.class);
  ViewIndexer indexer = mock(ViewIndexer.class);
  IndexViewsStep sut = new IndexViewsStep(indexer);

  @Test
  public void index_views() throws Exception {
    when(context.getProject()).thenReturn(ComponentTesting.newProjectDto("ABCD").setQualifier(Qualifiers.VIEW));

    sut.execute(context);

    verify(indexer).index("ABCD");
  }

}
