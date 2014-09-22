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

package org.sonar.server.component.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SnapshotDaoTest extends AbstractDaoTestCase {

  DbSession session;

  SnapshotDao dao;

  System2 system2;

  @Before
  public void createDao() throws Exception {
    session = getMyBatis().openSession(false);
    system2 = mock(System2.class);
    dao = new SnapshotDao(system2);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void get_by_key() {
    setupData("shared");

    SnapshotDto result = dao.getNullableByKey(session, 3L);
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(3L);
    assertThat(result.getResourceId()).isEqualTo(3L);
    assertThat(result.getRootProjectId()).isEqualTo(1L);
    assertThat(result.getParentId()).isEqualTo(2L);
    assertThat(result.getRootId()).isEqualTo(1L);
    assertThat(result.getStatus()).isEqualTo("P");
    assertThat(result.getLast()).isTrue();
    assertThat(result.getPurgeStatus()).isEqualTo(1);
    assertThat(result.getDepth()).isEqualTo(1);
    assertThat(result.getScope()).isEqualTo("DIR");
    assertThat(result.getQualifier()).isEqualTo("PAC");
    assertThat(result.getVersion()).isEqualTo("2.1-SNAPSHOT");
    assertThat(result.getPath()).isEqualTo("1.2.");

    assertThat(result.getPeriodMode(1)).isEqualTo("days1");
    assertThat(result.getPeriodModeParameter(1)).isEqualTo("30");
    assertThat(result.getPeriodDate(1)).isEqualTo(DateUtils.parseDate("2011-09-24"));
    assertThat(result.getPeriodMode(2)).isEqualTo("days2");
    assertThat(result.getPeriodModeParameter(2)).isEqualTo("31");
    assertThat(result.getPeriodDate(2)).isEqualTo(DateUtils.parseDate("2011-09-25"));
    assertThat(result.getPeriodMode(3)).isEqualTo("days3");
    assertThat(result.getPeriodModeParameter(3)).isEqualTo("32");
    assertThat(result.getPeriodDate(3)).isEqualTo(DateUtils.parseDate("2011-09-26"));
    assertThat(result.getPeriodMode(4)).isEqualTo("days4");
    assertThat(result.getPeriodModeParameter(4)).isEqualTo("33");
    assertThat(result.getPeriodDate(4)).isEqualTo(DateUtils.parseDate("2011-09-27"));
    assertThat(result.getPeriodMode(5)).isEqualTo("days5");
    assertThat(result.getPeriodModeParameter(5)).isEqualTo("34");
    assertThat(result.getPeriodDate(5)).isEqualTo(DateUtils.parseDate("2011-09-28"));

    assertThat(result.getCreatedAt()).isEqualTo(DateUtils.parseDate("2008-12-02"));
    assertThat(result.getBuildDate()).isEqualTo(DateUtils.parseDate("2011-09-29"));

    assertThat(dao.getNullableByKey(session, 999L)).isNull();
  }

  @Test
  public void insert() {
    setupData("empty");

    when(system2.now()).thenReturn(DateUtils.parseDate("2014-06-18").getTime());

    SnapshotDto dto = new SnapshotDto()
      .setResourceId(3L)
      .setRootProjectId(1L)
      .setParentId(2L)
      .setRootId(1L)
      .setStatus("P")
      .setLast(true)
      .setPurgeStatus(1)
      .setDepth(1)
      .setScope("DIR")
      .setQualifier("PAC")
      .setVersion("2.1-SNAPSHOT")
      .setPath("1.2.")
      .setPeriodMode(1, "days1")
      .setPeriodMode(2, "days2")
      .setPeriodMode(3, "days3")
      .setPeriodMode(4, "days4")
      .setPeriodMode(5, "days5")
      .setPeriodParam(1, "30")
      .setPeriodParam(2, "31")
      .setPeriodParam(3, "32")
      .setPeriodParam(4, "33")
      .setPeriodParam(5, "34")
      .setPeriodDate(1, DateUtils.parseDate("2011-09-24"))
      .setPeriodDate(2, DateUtils.parseDate("2011-09-25"))
      .setPeriodDate(3, DateUtils.parseDate("2011-09-26"))
      .setPeriodDate(4, DateUtils.parseDate("2011-09-27"))
      .setPeriodDate(5, DateUtils.parseDate("2011-09-28"))
      .setBuildDate(DateUtils.parseDate("2011-09-29"));

    dao.insert(session, dto);
    session.commit();

    assertThat(dto.getId()).isNotNull();
    checkTables("insert", "snapshots");
  }

}
