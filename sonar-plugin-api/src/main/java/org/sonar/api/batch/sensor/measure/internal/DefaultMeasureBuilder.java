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
package org.sonar.api.batch.sensor.measure.internal;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.measure.MeasureBuilder;

import java.io.Serializable;

public class DefaultMeasureBuilder<G extends Serializable> implements MeasureBuilder<G> {

  boolean onProject = false;
  InputFile file;
  Metric<G> metric;
  G value;

  @Override
  public DefaultMeasureBuilder<G> onFile(InputFile inputFile) {
    Preconditions.checkState(!this.onProject, "onProject already called");
    Preconditions.checkState(this.file == null, "onFile already called");
    Preconditions.checkNotNull(inputFile, "InputFile should be non null");
    this.file = inputFile;
    return this;
  }

  @Override
  public DefaultMeasureBuilder<G> onProject() {
    Preconditions.checkState(!this.onProject, "onProject already called");
    Preconditions.checkState(this.file == null, "onFile already called");
    this.onProject = true;
    return this;
  }

  @Override
  public DefaultMeasureBuilder<G> forMetric(Metric<G> metric) {
    Preconditions.checkState(metric != null, "Metric already defined");
    Preconditions.checkNotNull(metric, "metric should be non null");
    this.metric = metric;
    return this;
  }

  @Override
  public DefaultMeasureBuilder<G> withValue(G value) {
    Preconditions.checkState(this.value == null, "Measure value already defined");
    Preconditions.checkNotNull(value, "Measure value can't be null");
    this.value = value;
    return this;
  }

  @Override
  public DefaultMeasure<G> build() {
    DefaultMeasure<G> result = new DefaultMeasure<G>(this);
    reset();
    return result;
  }

  private void reset() {
    onProject = false;
    file = null;
    metric = null;
    value = null;
  }
}
