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
package org.sonar.server.activity.index;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.core.activity.Activity;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.server.search.*;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.4
 */
public class ActivityIndex extends BaseIndex<Activity, ActivityDto, String> {

  public ActivityIndex(ActivityNormalizer normalizer, SearchClient node) {
    super(IndexDefinition.LOG, normalizer, node);
  }

  @Override
  protected String getKeyValue(String key) {
    return key;
  }

  @Override
  protected Map mapKey() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("path", ActivityNormalizer.LogFields.KEY.field());
    return mapping;
  }

  @Override
  protected Settings getIndexSettings() throws IOException {
    return ImmutableSettings.builder()
      .put("index.number_of_replicas", 0)
      .put("index.number_of_shards", 1)
      .put("analysis.analyzer.default.type", "keyword")
      .build();
  }

  @Override
  protected Map mapProperties() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    for (IndexField field : ActivityNormalizer.LogFields.ALL_FIELDS) {
      mapping.put(field.field(), mapField(field));
    }
    return mapping;
  }

  @Override
  protected Activity toDoc(final Map<String, Object> fields) {
    return new ActivityDoc(fields);
  }

  public Result<Activity> findAll() {
    SearchRequestBuilder request = getClient().prepareSearch(this.getIndexName())
      .setQuery(QueryBuilders.matchAllQuery())
      .setTypes(this.getIndexType())
      .setSize(Integer.MAX_VALUE);
    SearchResponse response = getClient().execute(request);
    return new Result<Activity>(this, response);
  }

  public SearchResponse search(ActivityQuery query, QueryContext options) {
    return search(query, options, null);
  }

  public SearchResponse search(ActivityQuery query, QueryContext options,
    @Nullable FilterBuilder domainFilter) {

    // Prepare query
    SearchRequestBuilder esSearch = getClient()
      .prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setIndices(this.getIndexName());

    // Integrate Pagination
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());

    // Sort Date Desc
    esSearch.addSort(ActivityNormalizer.LogFields.CREATED_AT.field(), SortOrder.DESC);

    AndFilterBuilder filter = FilterBuilders.andFilter();

    // implement Type Filtering
    OrFilterBuilder typeFilter = FilterBuilders.orFilter();
    for (Activity.Type type : query.getTypes()) {
      typeFilter.add(FilterBuilders.termFilter(ActivityNormalizer.LogFields.TYPE.field(), type));
    }
    filter.add(typeFilter);

    // Implement date Filter
    filter.add(FilterBuilders.rangeFilter(ActivityNormalizer.LogFields.CREATED_AT.field())
      .from(query.getSince())
      .to(query.getTo()));

    // Add any additional domain filter
    if (domainFilter != null) {
      filter.add(domainFilter);
    }

    esSearch.setQuery(QueryBuilders.filteredQuery(
      QueryBuilders.matchAllQuery(), filter));

    if (options.isScroll()) {
      esSearch.setSearchType(SearchType.SCAN);
      esSearch.setScroll(TimeValue.timeValueMinutes(3));
    }

    SearchResponse response = getClient().execute(esSearch);

    return response;
  }
}
