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
package org.sonar.api.batch.sensor.internal;

import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import static org.fest.assertions.Assertions.assertThat;

public class DefaultSensorDescriptorTest {

  @Test
  public void describe() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    descriptor
      .name("Foo")
      .dependsOn(CoreMetrics.NCLOC)
      .provides(CoreMetrics.BLOCKER_VIOLATIONS)
      .workOnLanguages("java", "php")
      .workOnFileTypes(InputFile.Type.MAIN);

    assertThat(descriptor.name()).isEqualTo("Foo");
    assertThat(descriptor.dependsOn()).containsOnly(CoreMetrics.NCLOC);
    assertThat(descriptor.provides()).containsOnly(CoreMetrics.BLOCKER_VIOLATIONS);
    assertThat(descriptor.languages()).containsOnly("java", "php");
    assertThat(descriptor.types()).containsOnly(InputFile.Type.MAIN);
  }

}
