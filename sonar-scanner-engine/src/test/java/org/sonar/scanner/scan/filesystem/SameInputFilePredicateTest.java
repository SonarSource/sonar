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
package org.sonar.scanner.scan.filesystem;

import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFilePredicates;

import static org.assertj.core.api.Assertions.assertThat;

public class SameInputFilePredicateTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  FilePredicates predicates;

  @Before
  public void before() throws IOException {
    predicates = new DefaultFilePredicates(temp.newFolder().toPath());
  }

  @Test
  public void testHasExplicitFilterOnStatus() {
    assertThat(SameInputFilePredicate.hasExplicitFilterOnStatus(predicates.all())).isFalse();
    assertThat(SameInputFilePredicate.hasExplicitFilterOnStatus(predicates.hasStatus(InputFile.Status.ADDED))).isTrue();
    assertThat(SameInputFilePredicate.hasExplicitFilterOnStatus(predicates.and(predicates.all(), predicates.hasStatus(InputFile.Status.ADDED)))).isTrue();
  }
}
