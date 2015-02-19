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
package org.sonar.batch.bootstrapper;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingConfigurationTest {

  @Test
  public void testSetVerbose() {
    assertThat(LoggingConfiguration.create(null).setVerbose(true)
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_VERBOSE);

    assertThat(LoggingConfiguration.create(null).setVerbose(false)
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_DEFAULT);

    assertThat(LoggingConfiguration.create(null).setRootLevel("ERROR")
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo("ERROR");
  }

  @Test
  public void shouldNotBeVerboseByDefault() {
    assertThat(LoggingConfiguration.create(null)
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_DEFAULT);
  }

  @Test
  public void testSetVerboseProperty() {
    Map<String, String> properties = Maps.newHashMap();
    assertThat(LoggingConfiguration.create(null).setProperties(properties)
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_DEFAULT);

    properties.put("sonar.verbose", "true");
    assertThat(LoggingConfiguration.create(null).setProperties(properties)
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_VERBOSE);

    properties.put("sonar.verbose", "false");
    assertThat(LoggingConfiguration.create(null).setProperties(properties)
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL)).isEqualTo(LoggingConfiguration.LEVEL_ROOT_DEFAULT);
  }

  @Test
  public void testDefaultFormat() {
    assertThat(LoggingConfiguration.create(null)
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_DEFAULT);
  }

  @Test
  public void testMavenFormat() {
    assertThat(LoggingConfiguration.create(new EnvironmentInformation("maven", "1.0"))
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_MAVEN);
  }

  @Test
  public void testSetFormat() {
    assertThat(LoggingConfiguration.create(null).setFormat("%d %level")
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo("%d %level");
  }

  @Test
  public void shouldNotSetBlankFormat() {
    assertThat(LoggingConfiguration.create(null).setFormat(null)
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_DEFAULT);

    assertThat(LoggingConfiguration.create(null).setFormat("")
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_DEFAULT);

    assertThat(LoggingConfiguration.create(null).setFormat("   ")
        .getSubstitutionVariable(LoggingConfiguration.PROPERTY_FORMAT)).isEqualTo(LoggingConfiguration.FORMAT_DEFAULT);
  }
}
