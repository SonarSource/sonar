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

package org.sonar.server.projectanalysis.ws;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarqube.ws.ProjectAnalyses.CreateEventResponse;
import org.sonarqube.ws.ProjectAnalyses.Event;
import org.sonarqube.ws.client.projectanalysis.CreateEventRequest;
import org.sonarqube.ws.client.projectanalysis.CreateEventRequest.Category;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.projectanalysis.CreateEventRequest.Category.OTHER;
import static org.sonarqube.ws.client.projectanalysis.CreateEventRequest.Category.VERSION;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_ANALYSIS;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_CATEGORY;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_NAME;

public class CreateEventAction implements ProjectAnalysesWsAction {
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final System2 system;

  public CreateEventAction(DbClient dbClient, UuidFactory uuidFactory, System2 system) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.system = system;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("create_event")
      .setDescription("Add an event to a project analysis")
      .setSince("6.3")
      .setPost(true)
      .setResponseExample(getClass().getResource("create_event-example.json"))
      .setHandler(this);

    action.createParam(PARAM_ANALYSIS)
      .setDescription("Analysis key")
      .setExampleValue(Uuids.UUID_EXAMPLE_01)
      .setRequired(true);

    action.createParam(PARAM_CATEGORY)
      .setDescription("Category")
      .setRequired(true)
      .setPossibleValues(EnumSet.allOf(Category.class));

    action.createParam(PARAM_NAME)
      .setDescription("Name")
      .setExampleValue("5.6")
      .setRequired(true);

    action.createParam(PARAM_DESCRIPTION)
      .setDescription("Description")
      .setExampleValue("Version released");
  }

  @Override
  public void handle(Request httpRequest, Response httpResponse) throws Exception {
    CreateEventRequest request = toAddEventRequest(httpRequest);
    CreateEventResponse response = doHandle(request);

    writeProtobuf(response, httpRequest, httpResponse);
  }

  private CreateEventResponse doHandle(CreateEventRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SnapshotDto analysis = getAnalysis(dbSession, request);
      checkExistingDbEvents(dbSession, request, analysis);
      EventDto dbEvent = insertDbEvent(dbSession, request, analysis);
      return toCreateEventResponse(dbEvent);
    }
  }

  private EventDto insertDbEvent(DbSession dbSession, CreateEventRequest request, SnapshotDto analysis) {
    EventDto dbEvent = dbClient.eventDao().insert(dbSession, toDbEvent(request, analysis));
    if (VERSION.equals(request.getCategory())) {
      dbClient.snapshotDao().updateVersion(dbSession, analysis.getUuid(), request.getName());
    }
    dbSession.commit();
    return dbEvent;
  }

  private SnapshotDto getAnalysis(DbSession dbSession, CreateEventRequest request) {
    SnapshotDto analysis = dbClient.snapshotDao().selectByUuid(dbSession, request.getAnalysis())
      .orElseThrow(() -> new NotFoundException(format(Locale.ENGLISH, "Analysis '%s' is not found", request.getAnalysis())));
    ComponentDto project = dbClient.componentDao().selectByUuid(dbSession, analysis.getComponentUuid()).orNull();
    checkState(project != null, "Project of analysis '%s' is not found", analysis.getUuid());
    checkArgument(Qualifiers.PROJECT.equals(project.qualifier()) && Scopes.PROJECT.equals(project.scope()),
      "An event must be created on a project");
    return analysis;
  }

  private static CreateEventRequest toAddEventRequest(Request request) {
    return CreateEventRequest.builder()
      .setAnalysis(request.mandatoryParam(PARAM_ANALYSIS))
      .setName(request.mandatoryParam(PARAM_NAME))
      .setCategory(request.mandatoryParamAsEnum(PARAM_CATEGORY, Category.class))
      .setDescription(request.param(PARAM_DESCRIPTION))
      .build();
  }

  private EventDto toDbEvent(CreateEventRequest request, SnapshotDto analysis) {
    return new EventDto()
      .setUuid(uuidFactory.create())
      .setAnalysisUuid(analysis.getUuid())
      .setComponentUuid(analysis.getComponentUuid())
      .setCategory(request.getCategory().name())
      .setName(request.getName())
      .setDescription(request.getDescription())
      .setCreatedAt(system.now())
      .setDate(analysis.getCreatedAt());
  }

  private static CreateEventResponse toCreateEventResponse(EventDto dbEvent) {
    Event.Builder wsEvent = Event.newBuilder()
      .setKey(dbEvent.getUuid())
      .setCategory(dbEvent.getCategory())
      .setAnalysis(dbEvent.getAnalysisUuid())
      .setName(dbEvent.getName());
    setNullable(dbEvent.getDescription(), wsEvent::setDescription);

    return CreateEventResponse.newBuilder().setEvent(wsEvent).build();
  }

  private void checkExistingDbEvents(DbSession dbSession, CreateEventRequest request, SnapshotDto analysis) {
    List<EventDto> dbEvents = dbClient.eventDao().selectByAnalysisUuid(dbSession, analysis.getUuid());
    Predicate<EventDto> similarEventExisting = filterSimilarEvents(request);
    dbEvents.stream()
      .filter(similarEventExisting)
      .findAny()
      .ifPresent(throwException(request));
  }

  private static Predicate<EventDto> filterSimilarEvents(CreateEventRequest request) {
    switch (request.getCategory()) {
      case VERSION:
        return dbEvent -> VERSION.name().equals(dbEvent.getCategory());
      case OTHER:
        return dbEvent -> OTHER.name().equals(dbEvent.getCategory()) && request.getName().equals(dbEvent.getName());
      default:
        throw new IllegalStateException("Event category not handled: " + request.getCategory());
    }
  }

  private static Consumer<EventDto> throwException(CreateEventRequest request) {
    switch (request.getCategory()) {
      case VERSION:
        return dbEvent -> {
          throw new IllegalArgumentException(format(ENGLISH, "A version event already exists on analysis '%s'", request.getAnalysis()));
        };
      case OTHER:
        return dbEvent -> {
          throw new IllegalArgumentException(format(ENGLISH, "An 'other' event with the same name already exists on analysis '%s'", request.getAnalysis()));
        };
      default:
        throw new IllegalStateException("Event category not handled: " + request.getCategory());
    }
  }
}
