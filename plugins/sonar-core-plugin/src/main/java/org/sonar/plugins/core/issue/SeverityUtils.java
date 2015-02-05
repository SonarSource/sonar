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
package org.sonar.plugins.core.issue;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.Severity;

final class SeverityUtils {
  private SeverityUtils() {
    // only static methods
  }

  static Metric severityToIssueMetric(String severity) {
    switch (severity) {
      case Severity.BLOCKER:
        return CoreMetrics.BLOCKER_VIOLATIONS;
      case Severity.CRITICAL:
        return CoreMetrics.CRITICAL_VIOLATIONS;
      case Severity.MAJOR:
        return CoreMetrics.MAJOR_VIOLATIONS;
      case Severity.MINOR:
        return CoreMetrics.MINOR_VIOLATIONS;
      case Severity.INFO:
        return CoreMetrics.INFO_VIOLATIONS;
      default:
        throw new IllegalArgumentException("Unsupported severity: " + severity);
    }
  }

  static Metric severityToNewMetricIssue(String severity) {
    switch (severity) {
      case Severity.BLOCKER:
        return CoreMetrics.NEW_BLOCKER_VIOLATIONS;
      case Severity.CRITICAL:
        return CoreMetrics.NEW_CRITICAL_VIOLATIONS;
      case Severity.MAJOR:
        return CoreMetrics.NEW_MAJOR_VIOLATIONS;
      case Severity.MINOR:
        return CoreMetrics.NEW_MINOR_VIOLATIONS;
      case Severity.INFO:
        return CoreMetrics.NEW_INFO_VIOLATIONS;
      default:
        throw new IllegalArgumentException("Unsupported severity: " + severity);
    }
  }
}
