/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.computation.ws;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Protobuf;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.computation.log.LogFileRef;
import org.sonar.server.computation.taskprocessor.CeTaskProcessor;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsCe;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_STATUS;

public class ActivityActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ComponentDbTester componentDb = new ComponentDbTester(dbTester);

  CeLogging ceLogging = mock(CeLogging.class);
  TaskFormatter formatter = new TaskFormatter(dbTester.getDbClient(), ceLogging, System2.INSTANCE);
  ActivityAction underTest = new ActivityAction(userSession, dbTester.getDbClient(), formatter, new CeTaskProcessor[]{mock(CeTaskProcessor.class)});
  WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() {
    when(ceLogging.getFile(any(LogFileRef.class))).thenReturn(Optional.<File>absent());
  }

  @Test
  public void get_all_past_activity() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);

    TestResponse wsResponse = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    // verify the protobuf response
    WsCe.ActivityResponse activityResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.ActivityResponse.parser());
    assertThat(activityResponse.getTasksCount()).isEqualTo(2);

    // chronological order, from newest to oldest
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T2");
    assertThat(activityResponse.getTasks(0).getStatus()).isEqualTo(WsCe.TaskStatus.FAILED);
    assertThat(activityResponse.getTasks(0).getComponentId()).isEqualTo("PROJECT_2");
    assertThat(activityResponse.getTasks(0).getAnalysisId()).isEqualTo("123456");
    assertThat(activityResponse.getTasks(0).getExecutionTimeMs()).isEqualTo(500L);
    assertThat(activityResponse.getTasks(0).getLogs()).isFalse();
    assertThat(activityResponse.getTasks(1).getId()).isEqualTo("T1");
    assertThat(activityResponse.getTasks(1).getStatus()).isEqualTo(WsCe.TaskStatus.SUCCESS);
    assertThat(activityResponse.getTasks(1).getComponentId()).isEqualTo("PROJECT_1");
    assertThat(activityResponse.getTasks(1).getLogs()).isFalse();
  }

  @Test
  public void filter_by_status() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertQueue("T3", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);

    TestResponse wsResponse = ws.newRequest()
      .setParam("status", "FAILED,IN_PROGRESS")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    WsCe.ActivityResponse activityResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.ActivityResponse.parser());
    assertThat(activityResponse.getTasksCount()).isEqualTo(2);
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T3");
    assertThat(activityResponse.getTasks(1).getId()).isEqualTo("T2");
  }

  @Test
  public void filter_on_current_activities() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    // T2 is the current activity (the most recent one)
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_1", CeActivityDto.Status.FAILED);
    insertQueue("T3", "PROJECT_1", CeQueueDto.Status.PENDING);

    TestResponse wsResponse = ws.newRequest()
      .setParam("onlyCurrents", "true")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    WsCe.ActivityResponse activityResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.ActivityResponse.parser());
    assertThat(activityResponse.getTasksCount()).isEqualTo(1);
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T2");
  }

  @Test
  public void paginate_results() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertQueue("T3", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);

    assertPage(1, 1, 3, asList("T3"));
    assertPage(2, 1, 3, asList("T2"));
    assertPage(1, 10, 3, asList("T3", "T2", "T1"));
    assertPage(2, 10, 3, Collections.<String>emptyList());
  }

  private void assertPage(int pageIndex, int pageSize, int expectedTotal, List<String> expectedOrderedTaskIds) {
    TestResponse wsResponse = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(Param.PAGE, Integer.toString(pageIndex))
      .setParam(Param.PAGE_SIZE, Integer.toString(pageSize))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING")
      .execute();

    WsCe.ActivityResponse activityResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.ActivityResponse.parser());
    assertThat(activityResponse.getPaging().getPageIndex()).isEqualTo(pageIndex);
    assertThat(activityResponse.getPaging().getPageSize()).isEqualTo(pageSize);
    assertThat(activityResponse.getPaging().getTotal()).isEqualTo(expectedTotal);

    assertThat(activityResponse.getTasksCount()).isEqualTo(expectedOrderedTaskIds.size());
    for (int i = 0; i < expectedOrderedTaskIds.size(); i++) {
      String expectedTaskId = expectedOrderedTaskIds.get(i);
      assertThat(activityResponse.getTasks(i).getId()).isEqualTo(expectedTaskId);
    }
  }

  @Test
  public void project_administrator_can_access_his_project_activity() {
    // no need to be a system admin
    userSession.addComponentUuidPermission(UserRole.ADMIN, "PROJECT_1", "PROJECT_1");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);

    TestResponse wsResponse = ws.newRequest()
      .setParam("componentId", "PROJECT_1")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    WsCe.ActivityResponse activityResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.ActivityResponse.parser());
    assertThat(activityResponse.getTasksCount()).isEqualTo(1);
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T1");
    assertThat(activityResponse.getTasks(0).getStatus()).isEqualTo(WsCe.TaskStatus.SUCCESS);
    assertThat(activityResponse.getTasks(0).getComponentId()).isEqualTo("PROJECT_1");
  }

  @Test
  public void search_activity_by_component_name() throws IOException {
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("apache struts").setUuid("P1"));
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("apache zookeeper").setUuid("P2"));
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("eclipse").setUuid("P3"));
    dbTester.commit();
    componentDb.indexProjects();
    userSession.setGlobalPermissions(UserRole.ADMIN);
    insertActivity("T1", "P1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "P2", CeActivityDto.Status.SUCCESS);
    insertActivity("T3", "P3", CeActivityDto.Status.SUCCESS);

    TestResponse wsResponse = ws.newRequest()
      .setParam("componentQuery", "apac")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    WsCe.ActivityResponse activityResponse = WsCe.ActivityResponse.parseFrom(wsResponse.getInputStream());
    assertThat(activityResponse.getTasksList()).extracting("id").containsOnly("T1", "T2");
  }

  @Test
  public void fail_if_both_filters_on_component_id_and_name() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("componentId and componentQuery must not be set at the same time");

    ws.newRequest()
      .setParam("componentId", "ID1")
      .setParam("componentQuery", "apache")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();
  }

  @Test
  public void fail_if_page_size_greater_than_1000() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'ps' parameter must be less than 1000");

    ws.newRequest()
      .setParam(Param.PAGE_SIZE, "1001")
      .execute();
  }

  @Test
  public void support_json_response() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    TestResponse wsResponse = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .execute();

    JsonAssert.assertJson(wsResponse.getInput()).isSimilarTo("{\"tasks\":[]}");
  }

  private CeQueueDto insertQueue(String taskUuid, String componentUuid, CeQueueDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setUuid(taskUuid);
    queueDto.setStatus(status);
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), queueDto);
    dbTester.commit();
    return queueDto;
  }

  private CeActivityDto insertActivity(String taskUuid, String componentUuid, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setUuid(taskUuid);
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setSnapshotId(123_456L);
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(), activityDto);
    dbTester.commit();
    return activityDto;
  }
}
