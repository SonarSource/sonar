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
package org.sonar.api.resources;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DirectoryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createFromIoFileShouldComputeCorrectKey() throws IOException {
    java.io.File baseDir = temp.newFolder();
    Project project = mock(Project.class);
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    when(fileSystem.getBasedir()).thenReturn(baseDir);
    Resource dir = Directory.fromIOFile(new java.io.File(baseDir, "src/foo/bar/"), project);
    assertThat(dir.getKey(), is("src/foo/bar"));
  }

  @Test
  public void shouldStartBySlashAndNotEndBySlash() {
    Resource dir = Directory.create("src/foo/bar/", "      /foo/bar/  ");
    assertThat(dir.getKey(), is("src/foo/bar"));
    assertThat(dir.getDeprecatedKey(), is("foo/bar"));
    assertThat(dir.getName(), is("src/foo/bar"));
  }

  @Test
  public void shouldNotStartOrEndBySlashDeprecatedConstructor() {
    Resource dir = new Directory("      /foo/bar/  ");
    assertThat(dir.getDeprecatedKey(), is("foo/bar"));
  }

  @Test
  public void rootDirectoryDeprecatedConstructor() {
    assertThat(new Directory(null).getDeprecatedKey(), is(Directory.ROOT));
    assertThat(new Directory("").getDeprecatedKey(), is(Directory.ROOT));
    assertThat(new Directory("   ").getDeprecatedKey(), is(Directory.ROOT));
  }

  @Test
  public void backSlashesShouldBeReplacedBySlashes() {
    Resource dir = new Directory("  foo\\bar\\     ");
    assertThat(dir.getDeprecatedKey(), is("foo/bar"));
  }

  @Test
  public void directoryHasNoParents() {
    Resource dir = new Directory("foo/bar");
    assertThat(dir.getParent(), nullValue());
  }

  @Test
  public void shouldHaveOnlyOneLevelOfDirectory() {
    assertThat(new Directory("one/two/third").getParent(), nullValue());
    assertThat(new Directory("one").getParent(), nullValue());
  }

  @Test
  public void parseDirectoryKey() {
    assertThat(Directory.parseKey("/foo/bar"), is("foo/bar"));
  }

  @Test
  public void matchExclusionPatterns() {
    Directory directory = Directory.create("src/one/two/third", "one/two/third");
    assertThat(directory.matchFilePattern("one/two/*.java"), is(false));
    assertThat(directory.matchFilePattern("false"), is(false));
    assertThat(directory.matchFilePattern("two/one/**"), is(false));
    assertThat(directory.matchFilePattern("other*/**"), is(false));

    assertThat(directory.matchFilePattern("src/one*/**"), is(true));
    assertThat(directory.matchFilePattern("src/one/t?o/**"), is(true));
    assertThat(directory.matchFilePattern("**/*"), is(true));
    assertThat(directory.matchFilePattern("**"), is(true));
    assertThat(directory.matchFilePattern("src/one/two/*"), is(true));
    assertThat(directory.matchFilePattern("/src/one/two/*"), is(true));
    assertThat(directory.matchFilePattern("src/one/**"), is(true));
  }
}
