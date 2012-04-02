/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

/**
 * Barriers are used to define the order of execution of Decorators. Decorators must be annotated with the following :
 *
 * <ul>
 *   <li>{@code @DependsUpon(BARRIER)} in order to be executed after BARRIER</li>
 *   <li>{@code @DependedUpon(BARRIER)} in order to be executed before BARRIER</li>
 * </ul>
 *
 * @since 2.3
 */
public interface DecoratorBarriers {

  String START_VIOLATIONS_GENERATION = "START_VIOLATIONS_GENERATION";

  /**
   * This barrier is used by a decorator in order to :
   * <ul>
   * <li>be executed after all the decorators which generate violations :
   * {@code @DependsUpon(value=DecoratorBarriers.END_OF_VIOLATIONS_GENERATION}</li>
   * <li>declare that it generates violations : {@code @DependedUpon(value=DecoratorBarriers.END_OF_VIOLATIONS_GENERATION}</li>
   * </ul>
   */
  String END_OF_VIOLATIONS_GENERATION = "END_OF_VIOLATIONS_GENERATION";

  /**
   * Extensions which call the method {@code Violation#setSwitchedOff} must be executed before this barrier
   * ({@code @DependedUpon(value=DecoratorBarriers.START_VIOLATION_TRACKING})
   *
   * This barrier is after {@code END_OF_VIOLATIONS_GENERATION}
   *
   * @since 2.8
   */
  String START_VIOLATION_TRACKING = "START_VIOLATION_TRACKING";

  /**
   * This barrier is after {@code END_OF_VIOLATIONS_GENERATION} and {@code START_VIOLATION_TRACKING}.
   * Decorators executed after this barrier ({@code @DependsUpon(value=DecoratorBarriers.END_OF_VIOLATION_TRACKING})
   * can benefit from all the features of violation tracking :
   * <ul>
   *   <li>{@code Violation#getCreatedAt()}</li>
   *   <li>{@code Violation#isSwitchedOff()}, usually to know if a violation has been flagged as false-positives in UI</li>
   * </ul>
   *
   * @since 2.8
   */
  String END_OF_VIOLATION_TRACKING = "END_OF_VIOLATION_TRACKING";

  /**
   * @since 2.13
   */
  String START_VIOLATION_PERSISTENCE = "START_VIOLATION_PERSISTENCE";

  /**
   * @since 2.13
   */
  String END_OF_VIOLATION_PERSISTENCE = "END_OF_VIOLATION_PERSISTENCE";

  /**
   * Any kinds of time machine data are calculated before this barrier. Decorators executed after this barrier can use
   * Measure#getVariationValue() and Measure#getTendency() methods.
   *
   * @since 2.5
   */
  String END_OF_TIME_MACHINE = "END_OF_TIME_MACHINE";
  
  /**
   * Any kinds of alerts are calculated before this barrier. The global alert status will be computed after this barrier.
   * Use {@code @DependedUpon(value=DecoratorBarriers.END_OF_ALERTS_GENERATION)} if your decorator produces alerts.
   * @since 2.13
   */
  String END_OF_ALERTS_GENERATION = "END_OF_ALERTS_GENERATION";
}
