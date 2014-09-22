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
package org.sonar.plugins.core.timemachine;

import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TendencyDecoratorTest {

  @Test
  public void initQuery() throws ParseException {
    Project project = mock(Project.class);
    when(project.getAnalysisDate()).thenReturn(date("2009-12-25"));

    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.findAll()).thenReturn(Arrays.<Metric>asList(CoreMetrics.LINES, CoreMetrics.COVERAGE, CoreMetrics.COVERAGE_LINE_HITS_DATA));

    TendencyDecorator decorator = new TendencyDecorator(null, metricFinder);

    TimeMachineQuery query = decorator.initQuery(project);
    assertThat(query.getMetrics().size(), is(2));
    assertThat(query.getMetrics(), JUnitMatchers.<Metric>hasItems(CoreMetrics.LINES, CoreMetrics.COVERAGE));
    assertThat(query.getFrom(), is(date("2009-11-25")));
    assertThat(query.isToCurrentAnalysis(), is(true));
  }

  @Test
  public void includeCurrentMeasures() throws ParseException {
    TendencyAnalyser analyser = mock(TendencyAnalyser.class);
    TimeMachineQuery query = new TimeMachineQuery(null).setMetrics(CoreMetrics.LINES, CoreMetrics.COVERAGE);
    TimeMachine timeMachine = mock(TimeMachine.class);

    when(timeMachine.getMeasuresFields(query)).thenReturn(Arrays.<Object[]>asList(
      new Object[] {date("2009-12-01"), CoreMetrics.LINES, 1200.0},
      new Object[] {date("2009-12-01"), CoreMetrics.COVERAGE, 80.5},
      new Object[] {date("2009-12-02"), CoreMetrics.LINES, 1300.0},
      new Object[] {date("2009-12-02"), CoreMetrics.COVERAGE, 79.6},
      new Object[] {date("2009-12-15"), CoreMetrics.LINES, 1150.0}
    ));

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.LINES)).thenReturn(new Measure(CoreMetrics.LINES, 1400.0));
    when(context.getMeasure(CoreMetrics.COVERAGE)).thenReturn(new Measure(CoreMetrics.LINES, 90.0));

    TendencyDecorator decorator = new TendencyDecorator(timeMachine, query, analyser);
    decorator.decorate(new Directory("org/foo"), context);

    verify(analyser).analyseLevel(Arrays.asList(1200.0, 1300.0, 1150.0, 1400.0));
    verify(analyser).analyseLevel(Arrays.asList(80.5, 79.6, 90.0));
  }

  @Test
  public void noTendencyIfNoCurrentMeasures() throws ParseException {
    TendencyAnalyser analyser = mock(TendencyAnalyser.class);
    TimeMachineQuery query = new TimeMachineQuery(null).setMetrics(CoreMetrics.LINES, CoreMetrics.COVERAGE);
    TimeMachine timeMachine = mock(TimeMachine.class);

    when(timeMachine.getMeasuresFields(query)).thenReturn(Arrays.<Object[]>asList(
      new Object[] {date("2009-12-01"), CoreMetrics.LINES, 1200.0},
      new Object[] {date("2009-12-02"), CoreMetrics.LINES, 1300.0}
    ));

    DecoratorContext context = mock(DecoratorContext.class);
    TendencyDecorator decorator = new TendencyDecorator(timeMachine, query, analyser);
    decorator.decorate(new Directory("org/foo"), context);

    verify(analyser, never()).analyseLevel(anyList());
  }

  private Date date(String date) throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd").parse(date);
  }
}
