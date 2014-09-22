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
package org.sonar.server.rule.index;

import com.google.common.base.Preconditions;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.qualityprofile.index.ActiveRuleNormalizer;
import org.sonar.server.rule.Rule;
import org.sonar.server.search.*;

import javax.annotation.CheckForNull;

import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class RuleIndex extends BaseIndex<Rule, RuleDto, RuleKey> {

  public RuleIndex(RuleNormalizer normalizer, SearchClient client) {
    super(IndexDefinition.RULE, normalizer, client);
  }

  protected String getKeyValue(RuleKey key) {
    return key.toString();
  }

  @Override
  protected Settings getIndexSettings() throws IOException {
    return ImmutableSettings.builder()
      .put("index.number_of_replicas", 0)
      .put("index.number_of_shards", 1)
      .build();
  }

  @Override
  protected Map mapKey() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("path", RuleNormalizer.RuleField.KEY.field());
    return mapping;
  }

  @Override
  protected Map mapProperties() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    for (IndexField field : RuleNormalizer.RuleField.ALL_FIELDS) {
      mapping.put(field.field(), mapField(field));
    }
    return mapping;
  }

  private void setFields(QueryContext options, SearchRequestBuilder esSearch) {
    /* integrate Option's Fields */
    Set<String> fields = new HashSet<String>();
    if (!options.getFieldsToReturn().isEmpty()) {
      for (String fieldToReturn : options.getFieldsToReturn()) {
        if (!fieldToReturn.isEmpty()) {
          fields.add(fieldToReturn);
        }
      }
      // required field
      fields.add(RuleNormalizer.RuleField.KEY.field());
    } else {
      for (IndexField indexField : RuleNormalizer.RuleField.ALL_FIELDS) {
        fields.add(indexField.field());
      }
    }

    esSearch.setFetchSource(fields.toArray(new String[fields.size()]), null);
  }

  private void setFacets(QueryContext options, SearchRequestBuilder esSearch) {
    /* Integrate Facets */
    if (options.isFacet()) {
      this.setFacets(esSearch);
    }
  }

  private void setSorting(RuleQuery query, SearchRequestBuilder esSearch) {
    /* integrate Query Sort */
    String queryText = query.getQueryText();
    if (query.getSortField() != null) {
      FieldSortBuilder sort = SortBuilders.fieldSort(query.getSortField().sortField());
      if (query.isAscendingSort()) {
        sort.order(SortOrder.ASC);
      } else {
        sort.order(SortOrder.DESC);
      }
      esSearch.addSort(sort);
    } else if (queryText != null && !queryText.isEmpty()) {
      esSearch.addSort(SortBuilders.scoreSort());
    } else {
      esSearch.addSort(RuleNormalizer.RuleField.UPDATED_AT.sortField(), SortOrder.DESC);
      // deterministic sort when exactly the same updated_at (same millisecond)
      esSearch.addSort(RuleNormalizer.RuleField.KEY.sortField()
        , SortOrder.ASC);
    }
  }

  protected void setPagination(QueryContext options, SearchRequestBuilder esSearch) {
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());
  }

  private QueryBuilder termQuery(IndexField field, String query, float boost) {
    return QueryBuilders.multiMatchQuery(query,
      field.field(), field.field() + "." + IndexField.SEARCH_PARTIAL_SUFFIX)
      .operator(MatchQueryBuilder.Operator.AND)
      .boost(boost);
  }

  private QueryBuilder termAnyQuery(IndexField field, String query, float boost) {
    return QueryBuilders.multiMatchQuery(query,
      field.field(), field.field() + "." + IndexField.SEARCH_PARTIAL_SUFFIX)
      .operator(MatchQueryBuilder.Operator.OR)
      .boost(boost);
  }

  /* Build main query (search based) */
  protected QueryBuilder getQuery(RuleQuery query, QueryContext options) {

    // No contextual query case
    String queryText = query.getQueryText();
    if (queryText == null || queryText.isEmpty()) {
      return QueryBuilders.matchAllQuery();
    }

    // Build RuleBased contextual query
    BoolQueryBuilder qb = QueryBuilders.boolQuery();
    String queryString = query.getQueryText();

    // Human readable type of querying
    qb.should(QueryBuilders.simpleQueryString(query.getQueryText())
        .field(RuleNormalizer.RuleField.NAME.field() + "." + IndexField.SEARCH_WORDS_SUFFIX, 20f)
        .field(RuleNormalizer.RuleField.HTML_DESCRIPTION.field() + "." + IndexField.SEARCH_WORDS_SUFFIX, 3f)
        .defaultOperator(SimpleQueryStringBuilder.Operator.AND)
    ).boost(20f);

    // Match and partial Match queries
    qb.should(this.termQuery(RuleNormalizer.RuleField.KEY, queryString, 15f));
    qb.should(this.termQuery(RuleNormalizer.RuleField._KEY, queryString, 35f));
    qb.should(this.termQuery(RuleNormalizer.RuleField.LANGUAGE, queryString, 3f));
    qb.should(this.termQuery(RuleNormalizer.RuleField.CHARACTERISTIC, queryString, 5f));
    qb.should(this.termQuery(RuleNormalizer.RuleField.SUB_CHARACTERISTIC, queryString, 5f));
    qb.should(this.termQuery(RuleNormalizer.RuleField._TAGS, queryString, 10f));
    qb.should(this.termAnyQuery(RuleNormalizer.RuleField.CHARACTERISTIC, queryString, 1f));
    qb.should(this.termAnyQuery(RuleNormalizer.RuleField.SUB_CHARACTERISTIC, queryString, 1f));
    qb.should(this.termAnyQuery(RuleNormalizer.RuleField._TAGS, queryString, 1f));

    return qb;
  }

  /* Build main filter (match based) */
  protected FilterBuilder getFilter(RuleQuery query, QueryContext options) {

    BoolFilterBuilder fb = FilterBuilders.boolFilter();

    /* Add enforced filter on rules that are REMOVED */
    fb.mustNot(FilterBuilders
      .termFilter(RuleNormalizer.RuleField.STATUS.field(),
        RuleStatus.REMOVED.toString()));

    this.addTermFilter(fb, RuleNormalizer.RuleField.INTERNAL_KEY.field(), query.getInternalKey());
    this.addTermFilter(fb, RuleNormalizer.RuleField.RULE_KEY.field(), query.getRuleKey());
    this.addTermFilter(fb, RuleNormalizer.RuleField.LANGUAGE.field(), query.getLanguages());
    this.addTermFilter(fb, RuleNormalizer.RuleField.REPOSITORY.field(), query.getRepositories());
    this.addTermFilter(fb, RuleNormalizer.RuleField.SEVERITY.field(), query.getSeverities());
    this.addTermFilter(fb, RuleNormalizer.RuleField.KEY.field(), query.getKey());
    this.addTermFilter(fb, RuleNormalizer.RuleField._TAGS.field(), query.getTags());

    // Construct the debt filter on effective char and subChar
    Collection<String> debtCharacteristics = query.getDebtCharacteristics();
    if (debtCharacteristics != null && !debtCharacteristics.isEmpty()) {
      fb.must(
        FilterBuilders.orFilter(
          // Match only when NONE (overridden)
          FilterBuilders.andFilter(
            FilterBuilders.notFilter(
              FilterBuilders.termsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(), DebtCharacteristic.NONE)),
            FilterBuilders.orFilter(
              FilterBuilders.termsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(), debtCharacteristics),
              FilterBuilders.termsFilter(RuleNormalizer.RuleField.CHARACTERISTIC.field(), debtCharacteristics))
          ),

          // Match only when NOT NONE (not overridden)
          FilterBuilders.andFilter(
            FilterBuilders.orFilter(
              FilterBuilders.termsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(), ""),
              FilterBuilders.notFilter(FilterBuilders.existsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field()))),
            FilterBuilders.orFilter(
              FilterBuilders.termsFilter(RuleNormalizer.RuleField.DEFAULT_SUB_CHARACTERISTIC.field(), debtCharacteristics),
              FilterBuilders.termsFilter(RuleNormalizer.RuleField.DEFAULT_CHARACTERISTIC.field(), debtCharacteristics)))
        )
      );
    }

    // Debt char exist filter
    Boolean hasDebtCharacteristic = query.getHasDebtCharacteristic();
    if (hasDebtCharacteristic != null && hasDebtCharacteristic) {
      fb.mustNot(
        FilterBuilders.termsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(), DebtCharacteristic.NONE))
        .should(
          FilterBuilders.existsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field()))
        .should(
          FilterBuilders.existsFilter(RuleNormalizer.RuleField.DEFAULT_SUB_CHARACTERISTIC.field()));
    }

    if (query.getAvailableSince() != null) {
      fb.must(FilterBuilders.rangeFilter(RuleNormalizer.RuleField.CREATED_AT.field())
        .gte(query.getAvailableSince()));
    }

    Collection<RuleStatus> statusValues = query.getStatuses();
    if (statusValues != null && !statusValues.isEmpty()) {
      Collection<String> stringStatus = new ArrayList<String>();
      for (RuleStatus status : statusValues) {
        stringStatus.add(status.name());
      }
      this.addTermFilter(fb, RuleNormalizer.RuleField.STATUS.field(), stringStatus);
    }

    Boolean isTemplate = query.isTemplate();
    if (isTemplate != null) {
      this.addTermFilter(fb, RuleNormalizer.RuleField.IS_TEMPLATE.field(), Boolean.toString(isTemplate));
    }

    String template = query.templateKey();
    if (template != null) {
      this.addTermFilter(fb, RuleNormalizer.RuleField.TEMPLATE_KEY.field(), template);
    }

    // ActiveRule Filter (profile and inheritance)
    BoolFilterBuilder childrenFilter = FilterBuilders.boolFilter();
    this.addTermFilter(childrenFilter, ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field(), query.getQProfileKey());
    this.addTermFilter(childrenFilter, ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.field(), query.getInheritance());
    this.addTermFilter(childrenFilter, ActiveRuleNormalizer.ActiveRuleField.SEVERITY.field(), query.getActiveSeverities());

    // ChildQuery
    QueryBuilder childQuery;
    if (childrenFilter.hasClauses()) {
      childQuery = QueryBuilders.constantScoreQuery(childrenFilter);
    } else {
      childQuery = QueryBuilders.matchAllQuery();
    }

    /** Implementation of activation query */
    if (Boolean.TRUE.equals(query.getActivation())) {
      fb.must(FilterBuilders.hasChildFilter(IndexDefinition.ACTIVE_RULE.getIndexType(),
        childQuery));
    } else if (Boolean.FALSE.equals(query.getActivation())) {
      fb.mustNot(FilterBuilders.hasChildFilter(IndexDefinition.ACTIVE_RULE.getIndexType(),
        childQuery));
    }

    return fb;
  }

  protected void setFacets(SearchRequestBuilder query) {

    /* the Lang facet */
    query.addAggregation(AggregationBuilders
      .terms("languages")
      .field(RuleNormalizer.RuleField.LANGUAGE.field())
      .order(Terms.Order.count(false))
      .size(10)
      .minDocCount(1));

    /* the Tag facet */
    query.addAggregation(AggregationBuilders
      .terms("tags")
      .field(RuleNormalizer.RuleField._TAGS.field())
      .order(Terms.Order.count(false))
      .size(10)
      .minDocCount(1));

    /* the Repo facet */
    query.addAggregation(AggregationBuilders
      .terms("repositories")
      .field(RuleNormalizer.RuleField.REPOSITORY.field())
      .order(Terms.Order.count(false))
      .size(10)
      .minDocCount(1));

  }

  public Result<Rule> search(RuleQuery query, QueryContext options) {
    SearchRequestBuilder esSearch = getClient()
      .prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setIndices(this.getIndexName());

    if (options.isScroll()) {
      esSearch.setSearchType(SearchType.SCAN);
      esSearch.setScroll(TimeValue.timeValueMinutes(3));
    }

    setFacets(options, esSearch);
    setSorting(query, esSearch);
    setPagination(options, esSearch);
    setFields(options, esSearch);

    FilterBuilder fb = this.getFilter(query, options);
    QueryBuilder qb = this.getQuery(query, options);
    esSearch.setQuery(QueryBuilders.filteredQuery(qb, fb));

    SearchResponse esResult = getClient().execute(esSearch);

    return new Result<Rule>(this, esResult);
  }

  @Override
  protected Rule toDoc(Map<String, Object> fields) {
    Preconditions.checkNotNull(fields, "Cannot construct Rule with null response");
    return new RuleDoc(fields);
  }

  public Set<String> terms(String fields) {
    Set<String> tags = new HashSet<String>();
    String key = "_ref";

    SearchRequestBuilder request = this.getClient()
      .prepareSearch(this.getIndexName())
      .setQuery(QueryBuilders.matchAllQuery())
      .addAggregation(AggregationBuilders.terms(key)
        .field(fields)
        .size(Integer.MAX_VALUE)
        .minDocCount(1));

    SearchResponse esResponse = getClient().execute(request);

    Terms aggregation = esResponse.getAggregations().get(key);

    if (aggregation != null) {
      for (Terms.Bucket value : aggregation.getBuckets()) {
        tags.add(value.getKey());
      }
    }
    return tags;
  }

  /**
   * @deprecated please use getByKey(RuleKey key)
   */
  @Deprecated
  @CheckForNull
  public Rule getById(int id) {
    SearchRequestBuilder request = getClient().prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setQuery(QueryBuilders.termQuery(RuleNormalizer.RuleField.ID.field(), id))
      .setSize(1);
    SearchResponse response = getClient().execute(request);

    SearchHit hit = response.getHits().getAt(0);
    if (hit == null) {
      return null;
    } else {
      return toDoc(hit.getSource());
    }
  }

  /**
   * @deprecated please use getByKey(RuleKey key)
   */
  @Deprecated
  public List<Rule> getByIds(Collection<Integer> ids) {
    SearchRequestBuilder request = getClient().prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueSeconds(3L))
      .setSize(100)
      .setQuery(QueryBuilders.termsQuery(RuleNormalizer.RuleField.ID.field(), ids));
    SearchResponse scrollResp = getClient().execute(request);

    List<Rule> rules = newArrayList();
    while (true) {
      SearchScrollRequestBuilder scrollRequest = getClient()
        .prepareSearchScroll(scrollResp.getScrollId())
        .setScroll(TimeValue.timeValueSeconds(3L));

      scrollResp = getClient().execute(scrollRequest);

      for (SearchHit hit : scrollResp.getHits()) {
        rules.add(toDoc(hit.getSource()));
      }
      //Break condition: No hits are returned
      if (scrollResp.getHits().getHits().length == 0) {
        break;
      }
    }
    return rules;
  }
}
