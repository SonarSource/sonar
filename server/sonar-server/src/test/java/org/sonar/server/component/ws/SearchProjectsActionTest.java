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

package org.sonar.server.component.ws;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.ws.KeyExamples;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.client.component.SearchProjectsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchProjectsActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbClient dbClient = db.getDbClient();

  WsActionTester ws = new WsActionTester(new SearchProjectsAction(dbClient));

  SearchProjectsRequest.Builder request = SearchProjectsRequest.builder();

  @Test
  public void json_example() {
    componentDb.insertComponent(newProjectDto()
      .setUuid(Uuids.UUID_EXAMPLE_01)
      .setKey(KeyExamples.KEY_PROJECT_EXAMPLE_001)
      .setName("My Project 1"));
    componentDb.insertComponent(newProjectDto()
      .setUuid(Uuids.UUID_EXAMPLE_02)
      .setKey(KeyExamples.KEY_PROJECT_EXAMPLE_002)
      .setName("My Project 2"));
    componentDb.insertComponent(newProjectDto()
      .setUuid(Uuids.UUID_EXAMPLE_03)
      .setKey(KeyExamples.KEY_PROJECT_EXAMPLE_003)
      .setName("My Project 3"));

    String result = ws.newRequest().execute().getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void order_by_name() {
    componentDb.insertComponents(newProjectDto().setName("Maven"), newProjectDto().setName("Apache"), newProjectDto().setName("Guava"));

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsList()).extracting(Component::getName)
      .containsExactly("Apache", "Guava", "Maven");
  }

  @Test
  public void paginate_result() {
    IntStream.rangeClosed(1, 9).forEach(i -> componentDb.insertComponent(newProjectDto().setName("PROJECT-" + i)));

    SearchProjectsWsResponse result = call(request.setPage(2).setPageSize(3));

    assertThat(result.getPaging().getPageIndex()).isEqualTo(2);
    assertThat(result.getPaging().getPageSize()).isEqualTo(3);
    assertThat(result.getPaging().getTotal()).isEqualTo(9);
    assertThat(result.getComponentsCount()).isEqualTo(3);
    assertThat(result.getComponentsList())
      .extracting(Component::getName)
      .containsExactly("PROJECT-4", "PROJECT-5", "PROJECT-6");
  }

  @Test
  public void return_only_projects() {
    ComponentDto project = newProjectDto().setName("SonarQube");
    ComponentDto directory = newDirectory(project, "path");
    componentDb.insertComponents(project, newModuleDto(project), newView(), newDeveloper("Sonar Developer"), directory, newFileDto(project, directory));

    SearchProjectsWsResponse result = call(request);

    assertThat(result.getComponentsCount()).isEqualTo(1);
    assertThat(result.getComponents(0).getName()).isEqualTo("SonarQube");
  }

  @Test
  public void fail_if_page_size_greater_than_500() {
    expectedException.expect(IllegalArgumentException.class);

    call(request.setPageSize(501));
  }

  private SearchProjectsWsResponse call(SearchProjectsRequest.Builder requestBuilder) {
    SearchProjectsRequest wsRequest = requestBuilder.build();
    TestRequest httpRequest = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF);

    httpRequest.setParam(Param.PAGE, String.valueOf(wsRequest.getPage()));
    httpRequest.setParam(Param.PAGE_SIZE, String.valueOf(wsRequest.getPageSize()));

    try {
      return SearchProjectsWsResponse.parseFrom(httpRequest.execute().getInputStream());
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.key()).isEqualTo("search_projects");
    assertThat(def.since()).isEqualTo("6.2");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
  }
}
