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
package org.sonar.process.systeminfo;

import java.lang.management.MemoryMXBean;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessStateProviderTest {

  public static final String PROCESS_NAME = "the process name";

  @Test
  public void toMegaBytes() {
    assertThat(ProcessStateProvider.toMegaBytes(-1)).isNull();
    assertThat(ProcessStateProvider.toMegaBytes(0L)).isEqualTo(0L);
    assertThat(ProcessStateProvider.toMegaBytes(500L)).isEqualTo(0L);
    assertThat(ProcessStateProvider.toMegaBytes(500_000L)).isEqualTo(0L);
    assertThat(ProcessStateProvider.toMegaBytes(500_000_000L)).isEqualTo(476L);
  }

  @Test
  public void toSystemInfoSection() {
    ProcessStateProvider underTest = new ProcessStateProvider(PROCESS_NAME);
    ProtobufSystemInfo.Section section = underTest.toSystemInfoSection();

    assertThat(section.getName()).isEqualTo(PROCESS_NAME);
    assertThat(section.getAttributesCount()).isEqualTo(9);
    assertThat(section.getAttributes(0).getKey()).isEqualTo("Heap Committed (MB)");
    assertThat(section.getAttributes(0).getLongValue()).isGreaterThan(1L);
    assertThat(section.getAttributes(8).getKey()).isEqualTo("Thread Count");
    assertThat(section.getAttributes(8).getLongValue()).isGreaterThan(1L);
  }

  @Test
  public void should_hide_attributes_without_values() {
    MemoryMXBean memoryBean = mock(MemoryMXBean.class, Mockito.RETURNS_DEEP_STUBS);
    when(memoryBean.getHeapMemoryUsage().getCommitted()).thenReturn(-1L);

    ProcessStateProvider underTest = new ProcessStateProvider(PROCESS_NAME);
    ProtobufSystemInfo.Section section = underTest.toSystemInfoSection(memoryBean);

    assertThat(section.getAttributesList()).extracting("key").doesNotContain("Heap Committed (MB)");
  }
}
