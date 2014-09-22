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
package org.sonar.server.rule;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.sonar.core.rule.RuleDto;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleTagHelperTest {

  @Test
  public void applyTags() throws Exception {
    RuleDto rule = new RuleDto().setTags(Sets.newHashSet("performance"));
    boolean changed = RuleTagHelper.applyTags(rule, Sets.newHashSet("java8", "security"));
    assertThat(rule.getTags()).containsOnly("java8", "security");
    assertThat(changed).isTrue();
  }

  @Test
  public void applyTags_remove_all_existing_tags() throws Exception {
    RuleDto rule = new RuleDto().setTags(Sets.newHashSet("performance"));
    boolean changed = RuleTagHelper.applyTags(rule, Collections.<String>emptySet());
    assertThat(rule.getTags()).isEmpty();
    assertThat(changed).isTrue();
  }

  @Test
  public void applyTags_no_changes() throws Exception {
    RuleDto rule = new RuleDto().setTags(Sets.newHashSet("performance"));
    boolean changed = RuleTagHelper.applyTags(rule, Sets.newHashSet("performance"));
    assertThat(rule.getTags()).containsOnly("performance");
    assertThat(changed).isFalse();
  }

  @Test
  public void applyTags_validate_format() throws Exception {
    RuleDto rule = new RuleDto();
    boolean changed = RuleTagHelper.applyTags(rule, Sets.newHashSet("java8", "security"));
    assertThat(rule.getTags()).containsOnly("java8", "security");
    assertThat(changed).isTrue();

    try {
      RuleTagHelper.applyTags(rule, Sets.newHashSet("Java Eight"));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).startsWith("Tag 'Java Eight' is invalid");
    }
  }

  @Test
  public void applyTags_do_not_duplicate_system_tags() throws Exception {
    RuleDto rule = new RuleDto()
      .setTags(Sets.newHashSet("performance"))
      .setSystemTags(Sets.newHashSet("security"));

    boolean changed = RuleTagHelper.applyTags(rule, Sets.newHashSet("java8", "security"));

    assertThat(changed).isTrue();
    assertThat(rule.getTags()).containsOnly("java8");
    assertThat(rule.getSystemTags()).containsOnly("security");
  }
}
