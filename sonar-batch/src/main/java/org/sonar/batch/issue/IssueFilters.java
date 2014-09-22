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
package org.sonar.batch.issue;

import org.sonar.api.BatchComponent;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rules.Violation;
import org.sonar.batch.ViolationFilters;

import javax.annotation.Nullable;

public class IssueFilters implements BatchComponent {

  private final ViolationFilters deprecatedFilters;
  private final DeprecatedViolations deprecatedViolations;
  private final org.sonar.api.issue.IssueFilter[] exclusionFilters;
  private final IssueFilter[] filters;

  public IssueFilters(@Nullable ViolationFilters deprecatedFilters, @Nullable DeprecatedViolations deprecatedViolations, org.sonar.api.issue.IssueFilter[] exclusionFilters,
    IssueFilter[] filters) {
    this.deprecatedFilters = deprecatedFilters;
    this.deprecatedViolations = deprecatedViolations;
    this.exclusionFilters = exclusionFilters;
    this.filters = filters;
  }

  public IssueFilters(@Nullable ViolationFilters deprecatedFilters, @Nullable DeprecatedViolations deprecatedViolations, IssueFilter[] filters) {
    this(deprecatedFilters, deprecatedViolations, new org.sonar.api.issue.IssueFilter[0], filters);
  }

  public IssueFilters(@Nullable ViolationFilters deprecatedFilters, @Nullable DeprecatedViolations deprecatedViolations, org.sonar.api.issue.IssueFilter[] exclusionFilters) {
    this(deprecatedFilters, deprecatedViolations, exclusionFilters, new IssueFilter[0]);
  }

  public IssueFilters(@Nullable ViolationFilters deprecatedFilters, @Nullable DeprecatedViolations deprecatedViolations) {
    this(deprecatedFilters, deprecatedViolations, new org.sonar.api.issue.IssueFilter[0]);
  }

  /**
   * Used by scan2
   */
  public IssueFilters(org.sonar.api.issue.IssueFilter[] exclusionFilters, IssueFilter[] filters) {
    this(null, null, exclusionFilters, filters);
  }

  public IssueFilters(org.sonar.api.issue.IssueFilter[] exclusionFilters) {
    this(null, null, exclusionFilters, new IssueFilter[0]);
  }

  public IssueFilters(IssueFilter[] filters) {
    this(null, null, new org.sonar.api.issue.IssueFilter[0], filters);
  }

  public IssueFilters() {
    this(null, null, new org.sonar.api.issue.IssueFilter[0], new IssueFilter[0]);
  }

  public boolean accept(DefaultIssue issue, @Nullable Violation violation) {
    if (new DefaultIssueFilterChain(filters).accept(issue)) {
      // Apply deprecated rules only if filter chain accepts the current issue
      for (org.sonar.api.issue.IssueFilter filter : exclusionFilters) {
        if (!filter.accept(issue)) {
          return false;
        }
      }
      if (deprecatedFilters != null && !deprecatedFilters.isEmpty() && deprecatedViolations != null) {
        Violation v = violation != null ? violation : deprecatedViolations.toViolation(issue);
        return !deprecatedFilters.isIgnored(v);
      }
      return true;
    } else {
      return false;
    }
  }
}
