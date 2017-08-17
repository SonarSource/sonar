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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.scan.BranchConfiguration.BranchType;

public class BranchConfigurationProviderTest {
  private BranchConfigurationProvider provider = new BranchConfigurationProvider();
  private GlobalConfiguration globalConfiguration;
  private BranchConfigurationLoader loader;
  private BranchConfiguration config;

  @Before
  public void setUp() {
    globalConfiguration = mock(GlobalConfiguration.class);
    loader = mock(BranchConfigurationLoader.class);
    config = mock(BranchConfiguration.class);
  }

  @Test
  public void should_cache_config() {
    BranchConfiguration configuration = provider.provide(null, () -> "project", globalConfiguration);
    assertThat(provider.provide(null, () -> "project", globalConfiguration)).isSameAs(configuration);
  }

  @Test
  public void should_use_loader() {
    when(loader.load("key", globalConfiguration)).thenReturn(config);
    BranchConfiguration branchConfig = provider.provide(loader, () -> "key", globalConfiguration);

    assertThat(branchConfig).isSameAs(config);
  }

  @Test
  public void should_return_default_if_no_loader() {
    BranchConfiguration configuration = provider.provide(null, () -> "project", globalConfiguration);
    assertThat(configuration.branchTarget()).isNull();
    assertThat(configuration.branchType()).isEqualTo(BranchType.LONG);
  }
}
