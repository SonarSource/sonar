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
package org.sonar.api.batch.sensor.issue;

import com.google.common.annotations.Beta;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.rule.RuleKey;

import javax.annotation.CheckForNull;

/**
 * Issue reported by an {@link Sensor}
 *
 * @since 4.4
 */
@Beta
public interface Issue {

  /**
   * The {@link InputPath} this issue belongs to. Returns null if issue is global to the project.
   */
  @CheckForNull
  InputPath inputPath();

  /**
   * The {@link RuleKey} of this issue.
   */
  RuleKey ruleKey();

  /**
   * Message of the issue.
   */
  @CheckForNull
  String message();

  /**
   * Line of the issue. Null for global issues and issues on directories. Can also be null
   * for files (issue global to the file).
   */
  @CheckForNull
  Integer line();

  /**
   * Effort to fix the issue. Used by technical debt model.
   */
  @CheckForNull
  Double effortToFix();

  /**
   * See constants in {@link org.sonar.api.rule.Severity}.
   * Can be null before issue is saved. Means to use severity configured in quality profile.
   */
  @CheckForNull
  String severity();

}
