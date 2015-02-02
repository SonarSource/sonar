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
package org.sonar.server.issue.ws;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.persistence.DbSession;
import org.sonar.markdown.Markdown;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueQueryService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.search.ws.SearchRequestHandler;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class SearchAction extends SearchRequestHandler<IssueQuery, Issue> {


  public static final String SEARCH_ACTION = "search";

  private static final String ACTIONS_EXTRA_FIELD = "actions";
  private static final String TRANSITIONS_EXTRA_FIELD = "transitions";
  private static final String ASSIGNEE_NAME_EXTRA_FIELD = "assigneeName";
  private static final String REPORTER_NAME_EXTRA_FIELD = "reporterName";
  private static final String ACTION_PLAN_NAME_EXTRA_FIELD = "actionPlanName";

  private static final String EXTRA_FIELDS_PARAM = "extra_fields";

  private static final String INTERNAL_PARAMETER_DISCLAIMER = "This parameter is mostly used by the Issues page, please prefer usage of the componentKeys parameter. ";

  private final IssueChangeDao issueChangeDao;
  private final IssueService service;
  private final IssueActionsWriter actionsWriter;

  private final IssueQueryService issueQueryService;
  private final RuleService ruleService;
  private final DbClient dbClient;
  private final ActionPlanService actionPlanService;
  private final UserFinder userFinder;
  private final I18n i18n;
  private final Durations durations;
  private final Languages languages;

  public SearchAction(DbClient dbClient, IssueChangeDao issueChangeDao, IssueService service, IssueActionsWriter actionsWriter, IssueQueryService issueQueryService,
    RuleService ruleService, ActionPlanService actionPlanService, UserFinder userFinder, I18n i18n, Durations durations, Languages languages) {
    super(SEARCH_ACTION);
    this.dbClient = dbClient;
    this.issueChangeDao = issueChangeDao;
    this.service = service;
    this.actionsWriter = actionsWriter;
    this.issueQueryService = issueQueryService;
    this.ruleService = ruleService;
    this.actionPlanService = actionPlanService;
    this.userFinder = userFinder;
    this.i18n = i18n;
    this.durations = durations;
    this.languages = languages;
  }

  @Override
  protected void doDefinition(WebService.NewAction action) {
    action.setDescription("Get a list of issues. If the number of issues is greater than 10,000, only the first 10,000 ones are returned by the web service. " +
      "Requires Browse permission on project(s)")
      .setSince("3.6")
      .setResponseExample(Resources.getResource(this.getClass(), "example-search.json"));

    addComponentRelatedParams(action);
    action.createParam(IssueFilterParameters.ISSUES)
      .setDescription("Comma-separated list of issue keys")
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam(IssueFilterParameters.SEVERITIES)
      .setDescription("Comma-separated list of severities")
      .setExampleValue(Severity.BLOCKER + "," + Severity.CRITICAL)
      .setPossibleValues(Severity.ALL);
    action.createParam(IssueFilterParameters.STATUSES)
      .setDescription("Comma-separated list of statuses")
      .setExampleValue(Issue.STATUS_OPEN + "," + Issue.STATUS_REOPENED)
      .setPossibleValues(Issue.STATUSES);
    action.createParam(IssueFilterParameters.RESOLUTIONS)
      .setDescription("Comma-separated list of resolutions")
      .setExampleValue(Issue.RESOLUTION_FIXED + "," + Issue.RESOLUTION_REMOVED)
      .setPossibleValues(Issue.RESOLUTIONS);
    action.createParam(IssueFilterParameters.RESOLVED)
      .setDescription("To match resolved or unresolved issues")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.RULES)
      .setDescription("Comma-separated list of coding rule keys. Format is <repository>:<rule>")
      .setExampleValue("squid:AvoidCycles");
    action.createParam(IssueFilterParameters.TAGS)
      .setDescription("Comma-separated list of tags.")
      .setExampleValue("security,convention");
    action.createParam(IssueFilterParameters.HIDE_RULES)
      .setDescription("To not return rules")
      .setDefaultValue(false)
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.ACTION_PLANS)
      .setDescription("Comma-separated list of action plan keys (not names)")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513");
    action.createParam(IssueFilterParameters.PLANNED)
      .setDescription("To retrieve issues associated to an action plan or not")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.REPORTERS)
      .setDescription("Comma-separated list of reporter logins")
      .setExampleValue("admin");
    action.createParam(IssueFilterParameters.ASSIGNEES)
      .setDescription("Comma-separated list of assignee logins")
      .setExampleValue("admin,usera");
    action.createParam(IssueFilterParameters.ASSIGNED)
      .setDescription("To retrieve assigned or unassigned issues")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.LANGUAGES)
      .setDescription("Comma-separated list of languages. Available since 4.4")
      .setExampleValue("java,js");
    action.createParam(EXTRA_FIELDS_PARAM)
      .setDescription("Add some extra fields on each issue. Available since 4.4")
      .setPossibleValues(ACTIONS_EXTRA_FIELD, TRANSITIONS_EXTRA_FIELD, ASSIGNEE_NAME_EXTRA_FIELD, REPORTER_NAME_EXTRA_FIELD, ACTION_PLAN_NAME_EXTRA_FIELD);
    action.createParam(IssueFilterParameters.CREATED_AT)
      .setDescription("To retrieve issues created at a given date. Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.CREATED_AFTER)
      .setDescription("To retrieve issues created after the given date (inclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(IssueFilterParameters.CREATED_BEFORE)
      .setDescription("To retrieve issues created before the given date (exclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(SearchRequestHandler.PARAM_SORT)
      .setDescription("Sort field")
      .setDeprecatedKey(IssueFilterParameters.SORT)
      .setPossibleValues(IssueQuery.SORTS);
    action.createParam(SearchRequestHandler.PARAM_ASCENDING)
      .setDeprecatedKey(IssueFilterParameters.ASC)
      .setDescription("Ascending sort")
      .setBooleanPossibleValues();
    action.createParam(IssueFilterParameters.IGNORE_PAGING)
      .setDescription("Return the full list of issues, regardless of paging. For internal use only")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
    action.createParam("format")
      .setDescription("Only json format is available. This parameter is kept only for backward compatibility and shouldn't be used anymore");
  }

  private void addComponentRelatedParams(WebService.NewAction action) {

    action.createParam(IssueFilterParameters.ON_COMPONENT_ONLY)
      .setDescription("Return only issues at a component's level, not on its descendants (modules, directories, files, etc). " +
        "This parameter is only considered when componentKeys or componentUuids is set. " +
        "Using the deprecated componentRoots or componentRootUuids parameters will set this parameter to false. " +
        "Using the deprecated components parameter will set this parameter to true.")
      .setBooleanPossibleValues()
      .setDefaultValue("false");

    action.createParam(IssueFilterParameters.COMPONENT_KEYS)
      .setDescription("To retrieve issues associated to a specific list of components sub-components (comma-separated list of component keys). " +
        "A component can be a view, developer, project, module, directory or file. " +
        "If this parameter is set, componentUuids must not be set.")
      .setExampleValue("org.apache.struts:struts:org.apache.struts.Action");
    action.createParam(IssueFilterParameters.COMPONENTS)
      .setDescription("Deprecated since 5.1. If used, will have the same meaning as componentKeys AND onComponentOnly=true.");
    action.createParam(IssueFilterParameters.COMPONENT_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of components their sub-components (comma-separated list of component UUIDs). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "A component can be a project, module, directory or file. " +
        "If this parameter is set, componentKeys must not be set.")
      .setExampleValue("584a89f2-8037-4f7b-b82c-8b45d2d63fb2");

    action.createParam(IssueFilterParameters.COMPONENT_ROOTS)
      .setDescription("Deprecated since 5.1. If used, will have the same meaning as componentKeys AND onComponentOnly=false.");
    action.createParam(IssueFilterParameters.COMPONENT_ROOT_UUIDS)
      .setDescription("Deprecated since 5.1. If used, will have the same meaning as componentUuids AND onComponentOnly=false.");

    action.createParam(IssueFilterParameters.PROJECTS)
      .setDescription("Deprecated since 5.1. See projectKeys");
    action.createParam(IssueFilterParameters.PROJECT_KEYS)
      .setDescription("To retrieve issues associated to a specific list of projects (comma-separated list of project keys). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "If this parameter is set, projectUuids must not be set.")
      .setDeprecatedKey(IssueFilterParameters.PROJECTS)
      .setExampleValue("org.apache.struts:struts:org.apache.struts.Action");
    action.createParam(IssueFilterParameters.PROJECT_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of projects (comma-separated list of project UUIDs). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "Views are not supported. If this parameter is set, projectKeys must not be set.")
      .setExampleValue("7d8749e8-3070-4903-9188-bdd82933bb92");

    action.createParam(IssueFilterParameters.MODULE_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of modules (comma-separated list of module UUIDs). " +
        INTERNAL_PARAMETER_DISCLAIMER +
        "Views are not supported. If this parameter is set, moduleKeys must not be set.")
      .setExampleValue("7d8749e8-3070-4903-9188-bdd82933bb92");

    action.createParam(IssueFilterParameters.DIRECTORIES)
      .setDescription("Since 5.1. To retrieve issues associated to a specific list of directories (comma-separated list of directory paths). " +
        "This parameter is only meaningful when a module is selected. " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setExampleValue("src/main/java/org/sonar/server/");

    action.createParam(IssueFilterParameters.FILE_UUIDS)
      .setDescription("To retrieve issues associated to a specific list of files (comma-separated list of file UUIDs). " +
        INTERNAL_PARAMETER_DISCLAIMER)
      .setExampleValue("bdd82933-3070-4903-9188-7d8749e8bb92");
  }

  @Override
  protected IssueQuery doQuery(Request request) {
    return issueQueryService.createFromRequest(request);
  }

  @Override
  protected Result<Issue> doSearch(IssueQuery query, QueryContext context) {
    Collection<String> components = query.componentUuids();
    if (components != null && components.size() == 1 && BooleanUtils.isTrue(query.ignorePaging())) {
      context.setShowFullResult(true);
    }
    return service.search(query, context);
  }

  @Override
  @CheckForNull
  protected Collection<String> possibleFields() {
    return Collections.emptyList();
  }

  @Override
  @CheckForNull
  protected Collection<String> possibleFacets() {
    return Arrays.asList(new String[]{
      IssueFilterParameters.SEVERITIES,
      IssueFilterParameters.STATUSES,
      IssueFilterParameters.RESOLUTIONS,
      IssueFilterParameters.ACTION_PLANS,
      IssueFilterParameters.PROJECT_UUIDS,
      IssueFilterParameters.RULES,
      IssueFilterParameters.ASSIGNEES,
      IssueFilterParameters.REPORTERS,
      IssueFilterParameters.MODULE_UUIDS,
      IssueFilterParameters.FILE_UUIDS,
      IssueFilterParameters.DIRECTORIES,
      IssueFilterParameters.LANGUAGES,
      IssueFilterParameters.TAGS,
    });
  }

  @Override
  protected void doContextResponse(Request request, QueryContext context, Result<Issue> result, JsonWriter json) {
    List<String> issueKeys = newArrayList();
    Set<RuleKey> ruleKeys = newHashSet();
    Set<String> projectUuids = newHashSet();
    Set<String> componentUuids = newHashSet();
    Set<String> actionPlanKeys = newHashSet();
    List<String> userLogins = newArrayList();
    Map<String, User> usersByLogin = newHashMap();
    Map<String, ComponentDto> componentsByUuid = newHashMap();
    Multimap<String, DefaultIssueComment> commentsByIssues = ArrayListMultimap.create();
    Collection<ComponentDto> componentDtos = newHashSet();
    List<ComponentDto> projectDtos = newArrayList();
    Map<String, ComponentDto> projectsByComponentUuid = newHashMap();

    for (Issue issue : result.getHits()) {
      IssueDoc issueDoc = (IssueDoc) issue;
      issueKeys.add(issue.key());
      ruleKeys.add(issue.ruleKey());
      projectUuids.add(issueDoc.projectUuid());
      componentUuids.add(issueDoc.componentUuid());
      actionPlanKeys.add(issue.actionPlanKey());
      if (issue.reporter() != null) {
        userLogins.add(issue.reporter());
      }
      if (issue.assignee() != null) {
        userLogins.add(issue.assignee());
      }
    }

    collectRuleKeys(request, result, ruleKeys);

    collectFacetsData(request, result, projectUuids, componentUuids, userLogins, actionPlanKeys);

    UserSession userSession = UserSession.get();
    if (userSession.isLoggedIn()) {
      userLogins.add(userSession.login());
    }

    DbSession session = dbClient.openSession(false);
    try {
      List<DefaultIssueComment> comments = issueChangeDao.selectCommentsByIssues(session, issueKeys);
      for (DefaultIssueComment issueComment : comments) {
        userLogins.add(issueComment.userLogin());
        commentsByIssues.put(issueComment.issueKey(), issueComment);
      }
      usersByLogin = getUsersByLogin(userLogins);

      List<ComponentDto> fileDtos = dbClient.componentDao().getByUuids(session, componentUuids);
      List<ComponentDto> subProjectDtos = dbClient.componentDao().findSubProjectsByComponentUuids(session, componentUuids);
      componentDtos.addAll(fileDtos);
      componentDtos.addAll(subProjectDtos);
      for (ComponentDto component : componentDtos) {
        projectUuids.add(component.projectUuid());
      }

      projectDtos = dbClient.componentDao().getByUuids(session, projectUuids);
      componentDtos.addAll(projectDtos);
      for (ComponentDto componentDto : componentDtos) {
        componentsByUuid.put(componentDto.uuid(), componentDto);
      }
      projectsByComponentUuid = getProjectsByComponentUuid(componentDtos, projectDtos);

      writeProjects(json, projectDtos);
      writeComponents(json, componentDtos, projectsByComponentUuid);
    } finally {
      session.close();
    }

    Map<String, ActionPlan> actionPlanByKeys = getActionPlanByKeys(actionPlanKeys);

    writeIssues(result, commentsByIssues, usersByLogin, actionPlanByKeys, componentsByUuid, projectsByComponentUuid,
      request.paramAsStrings(EXTRA_FIELDS_PARAM), json);
    writeRules(json, !request.mandatoryParamAsBoolean(IssueFilterParameters.HIDE_RULES) ? ruleService.getByKeys(ruleKeys) : Collections.<Rule>emptyList());
    writeUsers(json, usersByLogin);
    writeActionPlans(json, actionPlanByKeys.values());
    writeLanguages(json);

    // TODO remove legacy paging. Handled by the SearchRequestHandler
    writeLegacyPaging(context, json, result);
  }

  private void collectRuleKeys(Request request, Result<Issue> result, Set<RuleKey> ruleKeys) {
    Collection<FacetValue> facetRules = result.getFacetValues(IssueFilterParameters.RULES);
    if (facetRules != null) {
      for (FacetValue rule: facetRules) {
        ruleKeys.add(RuleKey.parse(rule.getKey()));
      }
    }
    List<String> rulesFromRequest = request.paramAsStrings(IssueFilterParameters.RULES);
    if (rulesFromRequest != null ) {
      for (String ruleKey: rulesFromRequest) {
        ruleKeys.add(RuleKey.parse(ruleKey));
      }
    }
  }

  @Override
  protected void writeFacets(Request request, QueryContext context, Result<?> results, JsonWriter json) {
    addMandatoryFacetValues(results, IssueFilterParameters.SEVERITIES, Severity.ALL);
    addMandatoryFacetValues(results, IssueFilterParameters.STATUSES, Issue.STATUSES);
    List<String> resolutions = Lists.newArrayList("");
    resolutions.addAll(Issue.RESOLUTIONS);
    addMandatoryFacetValues(results, IssueFilterParameters.RESOLUTIONS, resolutions);
    addMandatoryFacetValues(results, IssueFilterParameters.PROJECT_UUIDS, request.paramAsStrings(IssueFilterParameters.PROJECT_UUIDS));

    List<String> assignees = Lists.newArrayList("");
    List<String> assigneesFromRequest = request.paramAsStrings(IssueFilterParameters.ASSIGNEES);
    if (assigneesFromRequest != null) {
      assignees.addAll(assigneesFromRequest);
    }
    UserSession userSession = UserSession.get();
    if (userSession.isLoggedIn()) {
      assignees.add(userSession.login());
    }
    addMandatoryFacetValues(results, IssueFilterParameters.ASSIGNEES, assignees);
    addMandatoryFacetValues(results, IssueFilterParameters.REPORTERS, request.paramAsStrings(IssueFilterParameters.REPORTERS));
    addMandatoryFacetValues(results, IssueFilterParameters.RULES, request.paramAsStrings(IssueFilterParameters.RULES));
    addMandatoryFacetValues(results, IssueFilterParameters.LANGUAGES, request.paramAsStrings(IssueFilterParameters.LANGUAGES));
    addMandatoryFacetValues(results, IssueFilterParameters.TAGS, request.paramAsStrings(IssueFilterParameters.TAGS));
    List<String> actionPlans = Lists.newArrayList("");
    List<String> actionPlansFromRequest = request.paramAsStrings(IssueFilterParameters.ACTION_PLANS);
    if (actionPlansFromRequest != null) {
      actionPlans.addAll(actionPlansFromRequest);
    }
    addMandatoryFacetValues(results, IssueFilterParameters.ACTION_PLANS, actionPlans);
    addMandatoryFacetValues(results, IssueFilterParameters.COMPONENT_UUIDS, request.paramAsStrings(IssueFilterParameters.COMPONENT_UUIDS));

    super.writeFacets(request, context, results, json);
  }

  private void collectFacetsData(Request request, Result<Issue> result, Set<String> projectUuids, Set<String> componentUuids, List<String> userLogins, Set<String> actionPlanKeys) {
    collectFacetKeys(result, IssueFilterParameters.PROJECT_UUIDS, projectUuids);
    collectParameterValues(request, IssueFilterParameters.PROJECT_UUIDS, projectUuids);

    collectFacetKeys(result, IssueFilterParameters.COMPONENT_UUIDS, componentUuids);
    collectParameterValues(request, IssueFilterParameters.COMPONENT_UUIDS, componentUuids);
    collectFacetKeys(result, IssueFilterParameters.FILE_UUIDS, componentUuids);
    collectParameterValues(request, IssueFilterParameters.FILE_UUIDS, componentUuids);

    collectFacetKeys(result, IssueFilterParameters.MODULE_UUIDS, componentUuids);
    collectParameterValues(request, IssueFilterParameters.MODULE_UUIDS, componentUuids);
    collectParameterValues(request, IssueFilterParameters.COMPONENT_ROOT_UUIDS, componentUuids);

    collectFacetKeys(result, IssueFilterParameters.ASSIGNEES, userLogins);
    collectParameterValues(request, IssueFilterParameters.ASSIGNEES, userLogins);
    collectFacetKeys(result, IssueFilterParameters.REPORTERS, userLogins);
    collectParameterValues(request, IssueFilterParameters.REPORTERS, userLogins);
    collectFacetKeys(result, IssueFilterParameters.ACTION_PLANS, actionPlanKeys);
    collectParameterValues(request, IssueFilterParameters.ACTION_PLANS, actionPlanKeys);
  }

  private void collectFacetKeys(Result<Issue> result, String facetName, Collection<String> facetKeys) {
    Collection<FacetValue> facetValues = result.getFacetValues(facetName);
    if (facetValues != null) {
      for (FacetValue project : facetValues) {
        facetKeys.add(project.getKey());
      }
    }
  }

  private void collectParameterValues(Request request, String facetName, Collection<String> facetKeys) {
    Collection<String> paramValues = request.paramAsStrings(facetName);
    if (paramValues != null) {
      facetKeys.addAll(paramValues);
    }
  }

  private void writeLegacyPaging(QueryContext context, JsonWriter json, Result<?> result) {
    // TODO remove with stas on HTML side
    json.prop("maxResultsReached", false);

    long pages = context.getLimit();
    if (pages > 0) {
      pages = result.getTotal() / context.getLimit();
      if (result.getTotal() % context.getLimit() > 0) {
        pages++;
      }
    }

    json.name("paging").beginObject()
      .prop("pageIndex", context.getPage())
      .prop("pageSize", context.getLimit())
      .prop("total", result.getTotal())
      // TODO Remove as part of Front-end rework on Issue Domain
      .prop("fTotal", i18n.formatInteger(UserSession.get().locale(), (int) result.getTotal()))
      .prop("pages", pages)
      .endObject();
  }

  // TODO change to use the RuleMapper
  private void writeRules(JsonWriter json, Collection<Rule> rules) {
    json.name("rules").beginArray();
    for (Rule rule : rules) {
      json.beginObject()
        .prop("key", rule.key().toString())
        .prop("name", rule.name())
        .prop("lang", rule.language())
        .prop("desc", rule.htmlDescription())
        .prop("status", rule.status().toString());
      Language lang = languages.get(rule.language());
      json.prop("langName", lang == null ? null : lang.getName());
      json.endObject();
    }
    json.endArray();
  }

  private void writeIssues(Result<Issue> result, Multimap<String, DefaultIssueComment> commentsByIssues, Map<String, User> usersByLogin, Map<String, ActionPlan> actionPlanByKeys,
    Map<String, ComponentDto> componentsByUuid, Map<String, ComponentDto> projectsByComponentUuid, @Nullable List<String> extraFields, JsonWriter json) {
    json.name("issues").beginArray();

    for (Issue issue : result.getHits()) {
      json.beginObject();

      String actionPlanKey = issue.actionPlanKey();
      ComponentDto file = componentsByUuid.get(issue.componentUuid());
      ComponentDto project = null, subProject = null;
      if (file != null) {
        project = projectsByComponentUuid.get(file.uuid());
        if (! file.projectUuid().equals(file.moduleUuid())) {
          subProject = componentsByUuid.get(file.moduleUuid());
        }
      }
      Duration debt = issue.debt();
      Date updateDate = issue.updateDate();

      json
        .prop("key", issue.key())
        .prop("component", file != null ? file.getKey() : null)
        // Only used for the compatibility with the Issues Java WS Client <= 4.4 used by Eclipse
        .prop("componentId", file != null ? file.getId() : null)
        .prop("project", project != null ? project.getKey() : null)
        .prop("subProject", subProject != null ? subProject.getKey() : null)
        .prop("rule", issue.ruleKey().toString())
        .prop("status", issue.status())
        .prop("resolution", issue.resolution())
        .prop("severity", issue.severity())
        .prop("message", issue.message())
        .prop("line", issue.line())
        .prop("debt", debt != null ? durations.encode(debt) : null)
        .prop("reporter", issue.reporter())
        .prop("assignee", issue.assignee())
        .prop("author", issue.authorLogin())
        .prop("actionPlan", actionPlanKey)
        .prop("creationDate", isoDate(issue.creationDate()))
        .prop("updateDate", isoDate(updateDate))
        // TODO Remove as part of Front-end rework on Issue Domain
        .prop("fUpdateAge", formatAgeDate(updateDate))
        .prop("closeDate", isoDate(issue.closeDate()));

      writeTags(issue, json);
      writeIssueComments(commentsByIssues.get(issue.key()), usersByLogin, json);
      writeIssueAttributes(issue, json);
      writeIssueExtraFields(issue, project != null ? project.getKey() : null, usersByLogin, actionPlanByKeys, extraFields, json);
      json.endObject();
    }

    json.endArray();
  }

  private void writeTags(Issue issue, JsonWriter json) {
    Collection<String> tags = issue.tags();
    if (tags != null && !tags.isEmpty()) {
      json.name("tags").beginArray();
      for (String tag: tags) {
        json.value(tag);
      }
      json.endArray();
    }
  }

  private void writeIssueComments(Collection<DefaultIssueComment> issueComments, Map<String, User> usersByLogin, JsonWriter json) {
    if (!issueComments.isEmpty()) {
      json.name("comments").beginArray();
      String login = UserSession.get().login();
      for (IssueComment comment : issueComments) {
        String userLogin = comment.userLogin();
        User user = userLogin != null ? usersByLogin.get(userLogin) : null;
        json.beginObject()
          .prop("key", comment.key())
          .prop("login", comment.userLogin())
          .prop("userName", user != null ? user.name() : null)
          .prop("htmlText", Markdown.convertToHtml(comment.markdownText()))
          .prop("markdown", comment.markdownText())
          .prop("updatable", login != null && login.equals(userLogin))
          .prop("createdAt", DateUtils.formatDateTime(comment.createdAt()))
          .endObject();
      }
      json.endArray();
    }
  }

  private void writeIssueAttributes(Issue issue, JsonWriter json) {
    if (!issue.attributes().isEmpty()) {
      json.name("attr").beginObject();
      for (Map.Entry<String, String> entry : issue.attributes().entrySet()) {
        json.prop(entry.getKey(), entry.getValue());
      }
      json.endObject();
    }
  }

  private void writeIssueExtraFields(Issue issue, @Nullable String projectKey, Map<String, User> usersByLogin, Map<String, ActionPlan> actionPlanByKeys,
    @Nullable List<String> extraFields,
    JsonWriter json) {
    if (extraFields != null) {
      if (extraFields.contains(ACTIONS_EXTRA_FIELD)) {
        actionsWriter.writeActions(issue, json);
      }

      if (extraFields.contains(TRANSITIONS_EXTRA_FIELD)) {
        actionsWriter.writeTransitions(issue, json);
      }

      writeAssigneeIfNeeded(issue, usersByLogin, extraFields, json);

      writeReporterIfNeeded(issue, usersByLogin, extraFields, json);

      writeActionPlanIfNeeded(issue, actionPlanByKeys, extraFields, json);
    }
  }

  private void writeAssigneeIfNeeded(Issue issue, Map<String, User> usersByLogin, List<String> extraFields, JsonWriter json) {
    String assignee = issue.assignee();
    if (extraFields.contains(ASSIGNEE_NAME_EXTRA_FIELD) && assignee != null) {
      User user = usersByLogin.get(assignee);
      json.prop(ASSIGNEE_NAME_EXTRA_FIELD, user != null ? user.name() : null);
    }
  }

  private void writeReporterIfNeeded(Issue issue, Map<String, User> usersByLogin, List<String> extraFields, JsonWriter json) {
    String reporter = issue.reporter();
    if (extraFields.contains(REPORTER_NAME_EXTRA_FIELD) && reporter != null) {
      User user = usersByLogin.get(reporter);
      json.prop(REPORTER_NAME_EXTRA_FIELD, user != null ? user.name() : null);
    }
  }

  private void writeActionPlanIfNeeded(Issue issue, Map<String, ActionPlan> actionPlanByKeys, List<String> extraFields, JsonWriter json) {
    String actionPlanKey = issue.actionPlanKey();
    if (extraFields.contains(ACTION_PLAN_NAME_EXTRA_FIELD) && actionPlanKey != null) {
      ActionPlan actionPlan = actionPlanByKeys.get(actionPlanKey);
      json.prop(ACTION_PLAN_NAME_EXTRA_FIELD, actionPlan != null ? actionPlan.name() : null);
    }
  }

  private void writeComponents(JsonWriter json, Collection<ComponentDto> components, Map<String, ComponentDto> projectsByComponentUuid) {
    json.name("components").beginArray();
    for (ComponentDto component : components) {
      ComponentDto project = projectsByComponentUuid.get(component.uuid());
      json.beginObject()
        .prop("uuid", component.uuid())
        .prop("key", component.key())
        .prop("id", component.getId())
        .prop("enabled", component.isEnabled())
        .prop("qualifier", component.qualifier())
        .prop("name", component.name())
        .prop("longName", component.longName())
        .prop("path", component.path())
        // On a root project, parentProjectId is null but projectId is equal to itself, which make no sense.
        .prop("projectId", (component.projectUuid() != null && component.parentProjectId() != null) ? project.getId() : null)
        // TODO should be renamed to parentProjectId
        .prop("subProjectId", component.parentProjectId())
        .endObject();
    }
    json.endArray();
  }

  private void writeProjects(JsonWriter json, List<ComponentDto> projects) {
    json.name("projects").beginArray();
    for (ComponentDto project : projects) {
      json.beginObject()
        .prop("uuid", project.uuid())
        .prop("key", project.key())
        .prop("id", project.getId())
        .prop("qualifier", project.qualifier())
        .prop("name", project.name())
        .prop("longName", project.longName())
        .endObject();
    }
    json.endArray();
  }

  private void writeUsers(JsonWriter json, Map<String, User> usersByLogin) {
    json.name("users").beginArray();
    for (User user : usersByLogin.values()) {
      json.beginObject()
        .prop("login", user.login())
        .prop("name", user.name())
        .prop("active", user.active())
        .prop("email", user.email())
        .endObject();
    }
    json.endArray();
  }

  private void writeLanguages(JsonWriter json) {
    json.name("languages").beginArray();
    for (Language language : languages.all()) {
      json.beginObject()
        .prop("key", language.getKey())
        .prop("name", language.getName())
        .endObject();
    }
    json.endArray();
  }

  private void writeActionPlans(JsonWriter json, Collection<ActionPlan> plans) {
    if (!plans.isEmpty()) {
      json.name("actionPlans").beginArray();
      for (ActionPlan actionPlan : plans) {
        Date deadLine = actionPlan.deadLine();
        Date updatedAt = actionPlan.updatedAt();

        json.beginObject()
          .prop("key", actionPlan.key())
          .prop("name", actionPlan.name())
          .prop("status", actionPlan.status())
          .prop("project", actionPlan.projectKey())
          .prop("userLogin", actionPlan.userLogin())
          .prop("deadLine", isoDate(deadLine))
          .prop("fDeadLine", formatDate(deadLine))
          .prop("createdAt", isoDate(actionPlan.createdAt()))
          .prop("fCreatedAt", formatDate(actionPlan.createdAt()))
          .prop("updatedAt", isoDate(actionPlan.updatedAt()))
          .prop("fUpdatedAt", formatDate(updatedAt))
          .endObject();
      }
      json.endArray();
    }
  }

  private Map<String, User> getUsersByLogin(List<String> userLogins) {
    Map<String, User> usersByLogin = newHashMap();
    for (User user : userFinder.findByLogins(userLogins)) {
      usersByLogin.put(user.login(), user);
    }
    return usersByLogin;
  }

  private Map<String, ActionPlan> getActionPlanByKeys(Collection<String> actionPlanKeys) {
    Map<String, ActionPlan> actionPlans = newHashMap();
    for (ActionPlan actionPlan : actionPlanService.findByKeys(actionPlanKeys)) {
      actionPlans.put(actionPlan.key(), actionPlan);
    }
    return actionPlans;
  }

  private Map<String, ComponentDto> getProjectsByComponentUuid(Collection<ComponentDto> components, Collection<ComponentDto> projects) {
    Map<String, ComponentDto> projectsByUuid = buildProjectsByUuid(projects);
    return buildProjectsByComponentUuid(components, projectsByUuid);
  }

  private Map<String, ComponentDto> buildProjectsByUuid(Collection<ComponentDto> projects) {
    Map<String, ComponentDto> projectsByUuid = newHashMap();
    for (ComponentDto project : projects) {
      if (project == null) {
        throw new IllegalStateException("Found a null project in issues");
      }
      if (project.uuid() == null) {
        throw new IllegalStateException("Project has no UUID: " + project.getKey());
      }
      projectsByUuid.put(project.uuid(), project);
    }
    return projectsByUuid;
  }

  private Map<String, ComponentDto> buildProjectsByComponentUuid(Collection<ComponentDto> components, Map<String, ComponentDto> projectsByUuid) {
    Map<String, ComponentDto> projectsByComponentUuid = newHashMap();
    for (ComponentDto component : components) {
      if (component.uuid() == null) {
        throw new IllegalStateException("Component has no UUID: " + component.getKey());
      }
      if (!projectsByUuid.containsKey(component.projectUuid())) {
        throw new IllegalStateException("Project cannot be found for component: " + component.getKey() + " / " + component.uuid());
      }
      projectsByComponentUuid.put(component.uuid(), projectsByUuid.get(component.projectUuid()));
    }
    return projectsByComponentUuid;
  }

  @CheckForNull
  private String isoDate(@Nullable Date date) {
    if (date != null) {
      return DateUtils.formatDateTime(date);
    }
    return null;
  }

  @CheckForNull
  private String formatDate(@Nullable Date date) {
    if (date != null) {
      return i18n.formatDateTime(UserSession.get().locale(), date);
    }
    return null;
  }

  @CheckForNull
  private String formatAgeDate(@Nullable Date date) {
    if (date != null) {
      return i18n.ageFromNow(UserSession.get().locale(), date);
    }
    return null;
  }

}
