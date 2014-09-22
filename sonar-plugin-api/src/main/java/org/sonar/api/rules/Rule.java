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

package org.sonar.api.rules;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Cardinality;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "rules")
public class Rule {

  /**
   * @since 3.6
   */
  public static final String STATUS_BETA = "BETA";
  /**
   * @since 3.6
   */
  public static final String STATUS_DEPRECATED = "DEPRECATED";
  /**
   * @since 3.6
   */
  public static final String STATUS_READY = "READY";

  /**
   * For internal use only.
   *
   * @since 3.6
   */
  public static final String STATUS_REMOVED = "REMOVED";

  /**
   * List of available status
   *
   * @since 3.6
   */
  private static final Set<String> STATUS_LIST = ImmutableSet.of(STATUS_READY, STATUS_BETA, STATUS_DEPRECATED, STATUS_REMOVED);

  /**
   * @since 4.2
   */
  private static final String[] DEFAULT_TAGS = new String[0];

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  /**
   * The default priority given to a rule if not explicitly set
   */
  public static final RulePriority DEFAULT_PRIORITY = RulePriority.MAJOR;

  @Column(name = "name", updatable = true, nullable = true, length = 200)
  private String name;

  @Column(name = "plugin_rule_key", updatable = false, nullable = true, length = 200)
  private String key;

  @Column(name = "plugin_config_key", updatable = true, nullable = true, length = 500)
  private String configKey;

  @Column(name = "priority", updatable = true, nullable = true)
  @Enumerated(EnumType.ORDINAL)
  private RulePriority priority = DEFAULT_PRIORITY;

  @Column(name = "description", updatable = true, nullable = true, length = DatabaseProperties.MAX_TEXT_SIZE)
  private String description;

  @Column(name = "plugin_name", updatable = true, nullable = false)
  private String pluginName;

  @Enumerated(EnumType.STRING)
  @Column(name = "is_template", updatable = true, nullable = false)
  private boolean isTemplate = false;

  @Column(name = "status", updatable = true, nullable = true)
  private String status = STATUS_READY;

  @Column(name = "language", updatable = true, nullable = true)
  private String language;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "template_id", updatable = true, nullable = true)
  private Rule template = null;

  @Column(name = "characteristic_id", updatable = true, nullable = true)
  private Integer characteristicId;

  @Column(name = "default_characteristic_id", updatable = true, nullable = true)
  private Integer defaultCharacteristicId;

  @org.hibernate.annotations.Cascade({org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
  @OneToMany(mappedBy = "rule")
  private List<RuleParam> params = new ArrayList<RuleParam>();

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_at", updatable = true, nullable = true)
  private Date createdAt;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "updated_at", updatable = true, nullable = true)
  private Date updatedAt;

  @Transient
  private String defaultCharacteristicKey;
  @Transient
  private String defaultSubCharacteristicKey;
  @Transient
  private String characteristicKey;
  @Transient
  private String subCharacteristicKey;

  private transient String[] tags = DEFAULT_TAGS;

  /**
   * @deprecated since 2.3. Use the factory method {@link #create()}
   */
  @Deprecated
  public Rule() {
  }

  /**
   * Creates rule with minimum set of info
   *
   * @param pluginName the plugin name indicates which plugin the rule belongs to
   * @param key        the key should be unique within a plugin, but it is even more careful for the time being that it is unique across the
   *                   application
   * @deprecated since 2.3. Use the factory method {@link #create()}
   */
  @Deprecated
  public Rule(String pluginName, String key) {
    this.pluginName = pluginName;
    this.key = key;
    this.configKey = key;
  }

  public Integer getId() {
    return id;
  }

  /**
   * @deprecated since 2.3. visibility should be decreased to protected or package
   */
  @Deprecated
  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  /**
   * Sets the rule name
   */
  public Rule setName(String name) {
    this.name = removeNewLineCharacters(name);
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * Sets the rule key
   */
  public Rule setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @deprecated since 2.5 use {@link #getRepositoryKey()} instead
   */
  @Deprecated
  public String getPluginName() {
    return pluginName;
  }

  /**
   * @deprecated since 2.5 use {@link #setRepositoryKey(String)} instead
   */
  @Deprecated
  public Rule setPluginName(String pluginName) {
    this.pluginName = pluginName;
    return this;
  }

  public String getConfigKey() {
    return configKey;
  }

  /**
   * Sets the configuration key
   */
  public Rule setConfigKey(String configKey) {
    this.configKey = configKey;
    return this;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Sets the rule description
   */
  public Rule setDescription(String description) {
    this.description = StringUtils.strip(description);
    return this;
  }

  /**
   * @deprecated in 3.6. Replaced by {@link #setStatus(String status)}.
   */
  @Deprecated
  public Rule setEnabled(Boolean enabled) {
    throw new UnsupportedOperationException("No more supported since version 3.6.");
  }

  public Boolean isEnabled() {
    return !STATUS_REMOVED.equals(status);
  }

  public List<RuleParam> getParams() {
    return params;
  }

  public RuleParam getParam(String key) {
    for (RuleParam param : params) {
      if (StringUtils.equals(key, param.getKey())) {
        return param;
      }
    }
    return null;
  }

  /**
   * Sets the rule parameters
   */
  public Rule setParams(List<RuleParam> params) {
    this.params.clear();
    for (RuleParam param : params) {
      param.setRule(this);
      this.params.add(param);
    }
    return this;
  }

  public RuleParam createParameter() {
    RuleParam parameter = new RuleParam()
      .setRule(this);
    params.add(parameter);
    return parameter;
  }

  public RuleParam createParameter(String key) {
    RuleParam parameter = new RuleParam()
      .setKey(key)
      .setRule(this);
    params.add(parameter);
    return parameter;
  }

  /**
   * @deprecated since 2.5. See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  public Integer getCategoryId() {
    return null;
  }

  /**
   * @since 2.5
   */
  public RulePriority getSeverity() {
    return priority;
  }

  /**
   * @param severity severity to set, if null, uses the default priority.
   * @since 2.5
   */
  public Rule setSeverity(RulePriority severity) {
    if (severity == null) {
      this.priority = DEFAULT_PRIORITY;
    } else {
      this.priority = severity;
    }
    return this;
  }

  /**
   * @deprecated since 2.5 use {@link #getSeverity()} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public RulePriority getPriority() {
    return priority;
  }

  /**
   * Sets the rule priority. If null, uses the default priority
   *
   * @deprecated since 2.5 use {@link #setSeverity(RulePriority)} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public Rule setPriority(RulePriority priority) {
    return setSeverity(priority);
  }

  public String getRepositoryKey() {
    return pluginName;
  }

  public Rule setRepositoryKey(String s) {
    this.pluginName = s;
    return this;
  }

  public Rule setUniqueKey(String repositoryKey, String key) {
    return setRepositoryKey(repositoryKey).setKey(key).setConfigKey(key);
  }

  /**
   * @since 4.4
   */
  public boolean isTemplate() {
    return isTemplate;
  }

  /**
   * @since 4.4
   */
  public Rule setIsTemplate(boolean isTemplate) {
    this.isTemplate = isTemplate;
    return this;
  }

  /**
   * @deprecated since 4.4, use {@link #isTemplate()}
   */
  @Deprecated
  public Cardinality getCardinality() {
    return isTemplate ? Cardinality.MULTIPLE : Cardinality.SINGLE;
  }

  /**
   * @deprecated since 4.4, use {@link #setIsTemplate(boolean)}
   */
  @Deprecated
  public Rule setCardinality(Cardinality c) {
    this.isTemplate = Cardinality.MULTIPLE.equals(c);
    return this;
  }

  /**
   * @deprecated since 4.4, use {@link #getTemplate()}
   */
  @Deprecated
  public Rule getParent() {
    return template;
  }

  /**
   * @deprecated since 4.4, use {@link #setTemplate(Rule)}}
   */
  @Deprecated
  public Rule setParent(Rule parent) {
    this.template = parent;
    return this;
  }

  /**
   * @since 4.4
   */
  public Rule getTemplate() {
    return template;
  }

  /**
   * @since 4.4
   */
  public Rule setTemplate(Rule template) {
    this.template = template;
    return this;
  }

  /**
   * @since 3.6
   */
  public String getStatus() {
    return status;
  }

  /**
   * @since 3.6
   */
  public Rule setStatus(String status) {
    if (!STATUS_LIST.contains(status)) {
      throw new SonarException("The status of a rule can only contain : " + Joiner.on(", ").join(STATUS_LIST));
    }
    this.status = status;
    return this;
  }

  /**
   * @since 3.6
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * @since 3.6
   */
  public Rule setCreatedAt(Date d) {
    this.createdAt = d;
    return this;
  }

  /**
   * @since 3.6
   */
  public Date getUpdatedAt() {
    return updatedAt;
  }

  /**
   * @since 3.6
   */
  public Rule setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * @since 3.6
   */
  public String getLanguage() {
    return language;
  }

  /**
   * For internal use only.
   *
   * @since 3.6
   */
  public Rule setLanguage(String language) {
    this.language = language;
    return this;
  }

  /**
   * For definition of rule only
   */
  public String[] getTags() {
    return tags;
  }

  /**
   * For definition of rule only
   */
  public Rule setTags(String[] tags) {
    this.tags = tags;
    return this;
  }

  /**
   * For internal use only.
   *
   * @deprecated since 4.4, use {@link #getCharacteristicKey()}
   * @since 4.3
   */
  @CheckForNull
  @Deprecated
  public Integer getCharacteristicId() {
    return characteristicId;
  }

  /**
   * For internal use only.
   *
   * @deprecated since 4.4, use {@link #setCharacteristicKey(@Nullable String characteristicKey)}
   * @since 4.3
   */
  @Deprecated
  public Rule setCharacteristicId(@Nullable Integer characteristicId) {
    this.characteristicId = characteristicId;
    return this;
  }

  /**
   * For internal use only.
   *
   * @deprecated since 4.4, use {@link #getDefaultCharacteristicKey()}
   * @since 4.3
   */
  @CheckForNull
  @Deprecated
  public Integer getDefaultCharacteristicId() {
    return defaultCharacteristicId;
  }

  /**
   * For internal use only.
   *
   * @deprecated since 4.4, use {@link #setDefaultCharacteristicKey(@Nullable String defaultCharacteristicKey)}
   * @since 4.3
   */
  @Deprecated
  public Rule setDefaultCharacteristicId(@Nullable Integer defaultCharacteristicId) {
    this.defaultCharacteristicId = defaultCharacteristicId;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Rule)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    Rule other = (Rule) obj;
    return new EqualsBuilder()
      .append(pluginName, other.getRepositoryKey())
      .append(key, other.getKey())
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(pluginName)
      .append(key)
      .toHashCode();
  }

  @Override
  public String toString() {
    // Note that ReflectionToStringBuilder will not work here - see SONAR-3077
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("id", id)
      .append("name", name)
      .append("key", key)
      .append("configKey", configKey)
      .append("plugin", pluginName)
      .append("severity", priority)
      .append("isTemplate", isTemplate())
      .append("status", status)
      .append("language", language)
      .append("template", template)
      .toString();
  }

  @CheckForNull
  private String removeNewLineCharacters(@Nullable String text) {
    String removedCRLF = StringUtils.remove(text, "\n");
    removedCRLF = StringUtils.remove(removedCRLF, "\r");
    removedCRLF = StringUtils.remove(removedCRLF, "\n\r");
    removedCRLF = StringUtils.remove(removedCRLF, "\r\n");
    return removedCRLF;
  }

  public static Rule create() {
    return new Rule();
  }

  /**
   * Create with all required fields
   */
  public static Rule create(String repositoryKey, String key, String name) {
    return new Rule().setUniqueKey(repositoryKey, key).setName(name);
  }

  /**
   * Create with all required fields
   *
   * @since 2.10
   */
  public static Rule create(String repositoryKey, String key) {
    return new Rule().setUniqueKey(repositoryKey, key);
  }

  /**
   * @since 3.6
   */
  public RuleKey ruleKey() {
    return RuleKey.of(getRepositoryKey(), getKey());
  }

  /**
   * @since 4.4
   */
  @CheckForNull
  public String getDefaultCharacteristicKey() {
    return defaultCharacteristicKey;
  }

  /**
   * @since 4.4
   */
  public Rule setDefaultCharacteristicKey(@Nullable String defaultCharacteristicKey) {
    this.defaultCharacteristicKey = defaultCharacteristicKey;
    return this;
  }

  /**
   * @since 4.4
   */
  @CheckForNull
  public String getDefaultSubCharacteristicKey() {
    return defaultSubCharacteristicKey;
  }

  /**
   * @since 4.4
   */
  public Rule setDefaultSubCharacteristicKey(@Nullable String defaultSubCharacteristicKey) {
    this.defaultSubCharacteristicKey = defaultSubCharacteristicKey;
    return this;
  }

  /**
   * @since 4.4
   */
  @CheckForNull
  public String getCharacteristicKey() {
    return characteristicKey;
  }

  /**
   * @since 4.4
   */
  public Rule setCharacteristicKey(@Nullable String characteristicKey) {
    this.characteristicKey = characteristicKey;
    return this;
  }

  /**
   * @since 4.4
   */
  @CheckForNull
  public String getSubCharacteristicKey() {
    return subCharacteristicKey;
  }

  /**
   * @since 4.4
   */
  public Rule setSubCharacteristicKey(@Nullable String subCharacteristicKey) {
    this.subCharacteristicKey = subCharacteristicKey;
    return this;
  }
}
