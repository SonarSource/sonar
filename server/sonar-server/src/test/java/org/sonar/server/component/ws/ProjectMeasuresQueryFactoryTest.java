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
package org.sonar.server.component.ws;

import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.component.ws.FilterParser.Criterion;
import org.sonar.server.measure.index.ProjectMeasuresQuery;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.measure.index.ProjectMeasuresQuery.Operator;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.newProjectMeasuresQuery;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.Level.OK;

public class ProjectMeasuresQueryFactoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Test
  public void create_query() throws Exception {
    List<Criterion> criteria = asList(
      Criterion.builder().setKey("ncloc").setOperator(">").setValue("10").build(),
      Criterion.builder().setKey("coverage").setOperator("<=").setValue("80").build());

    ProjectMeasuresQuery underTest = newProjectMeasuresQuery(criteria, emptySet());

    assertThat(underTest.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(
        tuple("ncloc", Operator.GT, 10d),
        tuple("coverage", Operator.LTE, 80d));
  }

  @Test
  public void create_query_having_lesser_than_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator("<").setValue("10").build()),
      emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.LT, 10d));
  }

  @Test
  public void create_query_having_lesser_than_or_equals_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator("<=").setValue("10").build()),
      emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.LTE, 10d));
  }

  @Test
  public void create_query_having_greater_than_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(">").setValue("10").build()),
      emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.GT, 10d));
  }

  @Test
  public void create_query_having_greater_than_or_equals_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(">=").setValue("10").build()),
      emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.GTE, 10d));
  }

  @Test
  public void create_query_having_equal_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator("=").setValue("10").build()),
      emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.EQ, 10d));
  }

  @Test
  public void create_query_on_quality_gate() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("alert_status").setOperator("=").setValue("OK").build()),
      emptySet());

    assertThat(query.getQualityGateStatus().get().name()).isEqualTo(OK.name());
  }

  @Test
  public void do_not_filter_on_projectUuids_if_criteria_non_empty_and_projectUuid_is_null() {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator("=").setValue("10").build()),
      null);

    assertThat(query.getProjectUuids()).isEmpty();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_empty_and_criteria_non_empty() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(">").setValue("10").build()),
      emptySet());

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_non_empty_and_criteria_non_empty() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(">").setValue("10").build()),
      Collections.singleton("foo"));

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_empty_and_criteria_is_empty() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(emptyList(), emptySet());

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void filter_on_projectUuids_if_projectUuid_is_non_empty_and_criteria_empty() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(emptyList(), Collections.singleton("foo"));

    assertThat(query.getProjectUuids()).isPresent();
  }

  @Test
  public void fail_to_create_query_on_quality_gate_when_operator_is_not_equal() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("alert_status").setOperator(">").setValue("OK").build()), emptySet());
  }

  @Test
  public void convert_metric_to_lower_case() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery(asList(
      Criterion.builder().setKey("NCLOC").setOperator(">").setValue("10").build(),
      Criterion.builder().setKey("coVERage").setOperator("<=").setValue("80").build()),
      emptySet());

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(
        tuple("ncloc", Operator.GT, 10d),
        tuple("coverage", Operator.LTE, 80d));
  }

  @Test
  public void accept_empty_query() throws Exception {
    ProjectMeasuresQuery result = newProjectMeasuresQuery(emptyList(), emptySet());

    assertThat(result.getMetricCriteria()).isEmpty();
  }

  @Test
  public void fail_when_not_double() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value 'ten' is not a number");

    newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(">").setValue("ten").build()),
      emptySet());
  }

  @Test
  public void fail_when_no_operator() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Operator cannot be null for 'ncloc'");

    newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(null).setValue("ten").build()),
      emptySet());
  }

  @Test
  public void fail_when_no_value() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value cannot be null for 'ncloc'");

    newProjectMeasuresQuery(singletonList(Criterion.builder().setKey("ncloc").setOperator(">").setValue(null).build()),
      emptySet());
  }
}
