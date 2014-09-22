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
package org.sonar.plugins.cpd;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.cpd.decorators.DuplicationDensityDecorator;
import org.sonar.plugins.cpd.decorators.SumDuplicationsDecorator;
import org.sonar.plugins.cpd.index.IndexFactory;

import java.util.List;

public final class CpdPlugin extends SonarPlugin {

  public List getExtensions() {
    return ImmutableList.of(
      PropertyDefinition.builder(CoreProperties.CPD_CROSS_PROJECT)
        .defaultValue(CoreProperties.CPD_CROSS_RPOJECT_DEFAULT_VALUE + "")
        .name("Cross project duplication detection")
        .description("By default, SonarQube detects duplications at sub-project level. This means that a block "
          + "duplicated on two sub-projects of the same project won't be reported. Setting this parameter to \"true\" "
          + "allows to detect duplicates across sub-projects and more generally across projects. Note that activating "
          + "this property will slightly increase each SonarQube analysis time.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS)
        .type(PropertyType.BOOLEAN)
        .build(),
      PropertyDefinition.builder(CoreProperties.CPD_SKIP_PROPERTY)
        .defaultValue("false")
        .name("Skip")
        .description("Disable detection of duplications")
        .hidden()
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS)
        .type(PropertyType.BOOLEAN)
        .build(),
      PropertyDefinition.builder(CoreProperties.CPD_EXCLUSIONS)
        .defaultValue("")
        .name("Duplication Exclusions")
        .description("Patterns used to exclude some source files from the duplication detection mechanism. " +
          "See below to know how to use wildcards to specify this property.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS_EXCLUSIONS)
        .multiValues(true)
        .build(),

      CpdSensor.class,
      CpdMappings.class,
      SumDuplicationsDecorator.class,
      DuplicationDensityDecorator.class,
      IndexFactory.class,
      JavaCpdEngine.class,
      DefaultCpdEngine.class);
  }

}
