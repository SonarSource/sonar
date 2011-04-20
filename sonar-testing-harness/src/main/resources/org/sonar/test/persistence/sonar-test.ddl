create table ACTIVE_DASHBOARDS (
  ID INTEGER not null,
  DASHBOARD_ID INTEGER not null,
  USER_ID INTEGER,
  ORDER_INDEX INTEGER,
  primary key (id)
);
CREATE INDEX ACTIVE_DASHBOARDS_DASHBOARDID ON ACTIVE_DASHBOARDS (DASHBOARD_ID);
CREATE INDEX ACTIVE_DASHBOARDS_USERID ON ACTIVE_DASHBOARDS (USER_ID);

create table ACTIVE_FILTERS (
  ID INTEGER not null,
  FILTER_ID INTEGER,
  USER_ID INTEGER,
  ORDER_INDEX INTEGER,
  primary key (id)
);

create table ACTIVE_RULES (
  ID INTEGER not null,
  PROFILE_ID INTEGER not null,
  RULE_ID INTEGER not null,
  FAILURE_LEVEL INTEGER not null,
  INHERITANCE VARCHAR(10),
  primary key (id)
);

create table ACTIVE_RULE_CHANGES (
  ID INTEGER not null,
  USER_LOGIN VARCHAR(40) not null,
  PROFILE_ID INTEGER not null,
  PROFILE_VERSION INTEGER not null,
  RULE_ID INTEGER not null,
  CHANGE_DATE TIMESTAMP not null,
  ENABLED SMALLINT,
  OLD_SEVERITY INTEGER,
  NEW_SEVERITY INTEGER,
  primary key (id)
);
CREATE INDEX ACTIVE_RULE_CHANGES_PROFILEID ON ACTIVE_RULE_CHANGES (PROFILE_ID);

create table ACTIVE_RULE_PARAMETERS (
  ID INTEGER not null,
  ACTIVE_RULE_ID INTEGER not null,
  RULES_PARAMETER_ID INTEGER not null,
  VALUE VARCHAR(4000),
  primary key (id)
);

create table ACTIVE_RULE_PARAM_CHANGES (
  ID INTEGER not null,
  ACTIVE_RULE_CHANGE_ID INTEGER not null,
  RULES_PARAMETER_ID INTEGER not null,
  OLD_VALUE VARCHAR(4000),
  NEW_VALUE VARCHAR(4000),
  primary key (id)
);
CREATE INDEX ACTIVE_RULE_PARAM_CHANGES_RULECHANGEID ON ACTIVE_RULE_PARAM_CHANGES (ACTIVE_RULE_CHANGE_ID);

create table ALERTS (
  ID INTEGER not null,
  PROFILE_ID INTEGER,
  METRIC_ID INTEGER,
  OPERATOR VARCHAR(3),
  VALUE_ERROR VARCHAR(64),
  VALUE_WARNING VARCHAR(64),
  primary key (id)
);

create table ASYNC_MEASURE_SNAPSHOTS (
  ID INTEGER not null,
  PROJECT_MEASURE_ID INTEGER,
  MEASURE_DATE TIMESTAMP,
  SNAPSHOT_ID INTEGER,
  SNAPSHOT_DATE TIMESTAMP,
  METRIC_ID INTEGER,
  PROJECT_ID INTEGER,
  primary key (id)
);
CREATE INDEX ASYNC_M_S_MEASURE_ID ON ASYNC_MEASURE_SNAPSHOTS (PROJECT_MEASURE_ID);
CREATE INDEX ASYNC_M_S_PROJECT_METRIC ON ASYNC_MEASURE_SNAPSHOTS (PROJECT_ID, METRIC_ID);
CREATE INDEX ASYNC_M_S_SNAPSHOT_ID ON ASYNC_MEASURE_SNAPSHOTS (SNAPSHOT_ID);

create table CHARACTERISTICS (
  ID INTEGER not null,
  QUALITY_MODEL_ID INTEGER,
  KEE VARCHAR(100),
  NAME VARCHAR(100),
  RULE_ID INTEGER,
  DEPTH INTEGER,
  CHARACTERISTIC_ORDER INTEGER,
  DESCRIPTION VARCHAR(4000),
  ENABLED SMALLINT,
  primary key (id)
);

create table CHARACTERISTIC_EDGES (
  CHILD_ID INTEGER,
  PARENT_ID INTEGER
);

create table CHARACTERISTIC_PROPERTIES (
  ID INTEGER not null,
  CHARACTERISTIC_ID INTEGER,
  KEE VARCHAR(100),
  VALUE DECIMAL(30, 20),
  TEXT_VALUE VARCHAR(4000),
  primary key (id)
);

create table CRITERIA (
  ID INTEGER not null,
  FILTER_ID INTEGER,
  FAMILY VARCHAR(100),
  KEE VARCHAR(100),
  OPERATOR VARCHAR(20),
  VALUE DECIMAL(30, 20),
  TEXT_VALUE VARCHAR(256),
  VARIATION SMALLINT,
  primary key (id)
);

create table DASHBOARDS (
  ID INTEGER not null,
  USER_ID INTEGER,
  NAME VARCHAR(256),
  DESCRIPTION VARCHAR(1000),
  COLUMN_LAYOUT VARCHAR(20),
  SHARED SMALLINT,
  CREATED_AT TIMESTAMP,
  UPDATED_AT TIMESTAMP,
  primary key (id)
);

create table DEPENDENCIES (
  ID INTEGER not null,
  FROM_SNAPSHOT_ID INTEGER,
  FROM_RESOURCE_ID INTEGER,
  TO_SNAPSHOT_ID INTEGER,
  TO_RESOURCE_ID INTEGER,
  DEP_USAGE VARCHAR(30),
  DEP_WEIGHT INTEGER,
  PROJECT_SNAPSHOT_ID INTEGER,
  PARENT_DEPENDENCY_ID BIGINT,
  FROM_SCOPE VARCHAR(3),
  TO_SCOPE VARCHAR(3),
  primary key (id)
);
CREATE INDEX DEPS_FROM_SID ON DEPENDENCIES (FROM_SNAPSHOT_ID);
CREATE INDEX DEPS_TO_SID ON DEPENDENCIES (TO_SNAPSHOT_ID);

create table EVENTS (
  ID INTEGER not null,
  NAME VARCHAR(50),
  RESOURCE_ID INTEGER,
  SNAPSHOT_ID INTEGER,
  CATEGORY VARCHAR(50),
  EVENT_DATE TIMESTAMP,
  CREATED_AT TIMESTAMP,
  DESCRIPTION VARCHAR(3072),
  DATA VARCHAR(4000),
  primary key (id)
);
CREATE INDEX EVENTS_RESOURCE_ID ON EVENTS (RESOURCE_ID);
CREATE INDEX EVENTS_SNAPSHOT_ID ON EVENTS (SNAPSHOT_ID);

create table FILTERS (
  ID INTEGER not null,
  NAME VARCHAR(100),
  USER_ID INTEGER,
  SHARED SMALLINT,
  FAVOURITES SMALLINT,
  RESOURCE_ID INTEGER,
  DEFAULT_VIEW VARCHAR(20),
  PAGE_SIZE INTEGER,
  PERIOD_INDEX INTEGER,
  primary key (id)
);

create table FILTER_COLUMNS (
  ID INTEGER not null,
  FILTER_ID INTEGER,
  FAMILY VARCHAR(100),
  KEE VARCHAR(100),
  SORT_DIRECTION VARCHAR(5),
  ORDER_INDEX INTEGER,
  VARIATION SMALLINT,
  primary key (id)
);

create table GROUPS (
  ID INTEGER not null,
  NAME VARCHAR(40),
  DESCRIPTION VARCHAR(200),
  CREATED_AT TIMESTAMP,
  UPDATED_AT TIMESTAMP,
  primary key (id)
);

create table GROUPS_USERS (
  USER_ID INTEGER,
  GROUP_ID INTEGER
);
CREATE INDEX INDEX_GROUPS_USERS_ON_GROUP_ID ON GROUPS_USERS (GROUP_ID);
CREATE INDEX INDEX_GROUPS_USERS_ON_USER_ID ON GROUPS_USERS (USER_ID);

create table GROUP_ROLES (
  ID INTEGER not null,
  GROUP_ID INTEGER,
  RESOURCE_ID INTEGER,
  ROLE VARCHAR(64) not null,
  primary key (id)
);
CREATE INDEX GROUP_ROLES_GROUP ON GROUP_ROLES (GROUP_ID);
CREATE INDEX GROUP_ROLES_RESOURCE ON GROUP_ROLES (RESOURCE_ID);

create table MEASURE_DATA (
  ID INTEGER not null,
  MEASURE_ID INTEGER,
  SNAPSHOT_ID INTEGER,
  DATA BLOB,
  primary key (id)
);
CREATE INDEX MEASURE_DATA_MEASURE_ID ON MEASURE_DATA (MEASURE_ID);
CREATE INDEX M_DATA_SID ON MEASURE_DATA (SNAPSHOT_ID);

create table METRICS (
  ID INTEGER not null,
  NAME VARCHAR(64) not null,
  DESCRIPTION VARCHAR(255),
  DIRECTION INTEGER not null,
  DOMAIN VARCHAR(64),
  SHORT_NAME VARCHAR(64),
  QUALITATIVE SMALLINT,
  VAL_TYPE VARCHAR(8),
  USER_MANAGED SMALLINT,
  ENABLED SMALLINT,
  ORIGIN VARCHAR(3),
  WORST_VALUE DECIMAL(30, 20),
  BEST_VALUE DECIMAL(30, 20),
  OPTIMIZED_BEST_VALUE SMALLINT,
  HIDDEN SMALLINT,
  primary key (id)
);
CREATE UNIQUE INDEX METRICS_UNIQUE_NAME ON METRICS (NAME);

create table PARAMETERS (
  ID INTEGER not null,
  PARAM_KEY VARCHAR(100) not null,
  VALUE DECIMAL(30, 20) not null,
  VALUE2 DECIMAL(30, 20),
  primary key (id)
);

create table PLUGINS (
  ID INTEGER not null,
  PLUGIN_KEY VARCHAR(100),
  VERSION VARCHAR(100),
  NAME VARCHAR(100),
  DESCRIPTION VARCHAR(3000),
  ORGANIZATION VARCHAR(100),
  ORGANIZATION_URL VARCHAR(500),
  LICENSE VARCHAR(50),
  INSTALLATION_DATE TIMESTAMP,
  PLUGIN_CLASS VARCHAR(100),
  HOMEPAGE VARCHAR(500),
  CORE SMALLINT,
  CHILD_FIRST_CLASSLOADER SMALLINT,
  BASE_PLUGIN VARCHAR(100),
  primary key (id)
);

create table PLUGIN_FILES (
  ID INTEGER not null,
  PLUGIN_ID INTEGER,
  FILENAME VARCHAR(100),
  primary key (id)
);

create table PROJECTS (
  ID INTEGER not null,
  NAME VARCHAR(256),
  DESCRIPTION VARCHAR(2000),
  ENABLED SMALLINT not null,
  SCOPE VARCHAR(3),
  QUALIFIER VARCHAR(3),
  KEE VARCHAR(400),
  ROOT_ID INTEGER,
  PROFILE_ID INTEGER,
  LANGUAGE VARCHAR(5),
  COPY_RESOURCE_ID INTEGER,
  LONG_NAME VARCHAR(256),
  primary key (id)
);
CREATE INDEX PROJECTS_KEE ON PROJECTS (KEE);

create table PROJECT_LINKS (
  ID INTEGER not null,
  PROJECT_ID INTEGER not null,
  LINK_TYPE VARCHAR(20),
  NAME VARCHAR(128),
  HREF VARCHAR(2048) not null,
  primary key (id)
);

create table PROJECT_MEASURES (
  ID INTEGER not null,
  VALUE DECIMAL(30, 20),
  METRIC_ID INTEGER not null,
  SNAPSHOT_ID INTEGER,
  RULE_ID INTEGER,
  RULES_CATEGORY_ID INTEGER,
  TEXT_VALUE VARCHAR(96),
  TENDENCY INTEGER,
  MEASURE_DATE TIMESTAMP,
  PROJECT_ID INTEGER,
  ALERT_STATUS VARCHAR(5),
  ALERT_TEXT VARCHAR(4000),
  URL VARCHAR(2000),
  DESCRIPTION VARCHAR(4000),
  RULE_PRIORITY INTEGER,
  CHARACTERISTIC_ID INTEGER,
  VARIATION_VALUE_1 DECIMAL(30, 20),
  VARIATION_VALUE_2 DECIMAL(30, 20),
  VARIATION_VALUE_3 DECIMAL(30, 20),
  VARIATION_VALUE_4 DECIMAL(30, 20),
  VARIATION_VALUE_5 DECIMAL(30, 20),
  primary key (id)
);
CREATE INDEX MEASURES_SID_METRIC ON PROJECT_MEASURES (SNAPSHOT_ID, METRIC_ID);

create table PROPERTIES (
  ID INTEGER not null,
  PROP_KEY VARCHAR(512),
  RESOURCE_ID INTEGER,
  TEXT_VALUE CLOB,
  USER_ID INTEGER,
  primary key (id)
);
CREATE INDEX PROPERTIES_KEY ON PROPERTIES (PROP_KEY);

create table QUALITY_MODELS (
  ID INTEGER not null,
  NAME VARCHAR(100),
  primary key (id)
);

create table RULES (
  ID INTEGER not null,
  NAME VARCHAR(192) not null,
  PLUGIN_RULE_KEY VARCHAR(200) not null,
  PLUGIN_NAME VARCHAR(255) not null,
  DESCRIPTION CLOB,
  PRIORITY INTEGER,
  ENABLED SMALLINT,
  CARDINALITY VARCHAR(10),
  PARENT_ID INTEGER,
  PLUGIN_CONFIG_KEY VARCHAR(500),
  primary key (id)
);

create table RULES_CATEGORIES (
  ID INTEGER not null,
  NAME VARCHAR(255) not null,
  DESCRIPTION VARCHAR(1000) not null,
  primary key (id)
);

create table RULES_PARAMETERS (
  ID INTEGER not null,
  RULE_ID INTEGER not null,
  NAME VARCHAR(128) not null,
  PARAM_TYPE VARCHAR(512) not null,
  DEFAULT_VALUE VARCHAR(4000),
  DESCRIPTION VARCHAR(4000),
  primary key (id)
);
CREATE INDEX ALTERED_RULES_PARAMETERS_RULE_ID ON RULES_PARAMETERS (RULE_ID);

create table RULES_PROFILES (
  ID INTEGER not null,
  NAME VARCHAR(100) not null,
  DEFAULT_PROFILE SMALLINT,
  PROVIDED SMALLINT,
  LANGUAGE VARCHAR(16),
  PARENT_NAME VARCHAR(100),
  ENABLED SMALLINT,
  VERSION INTEGER not null,
  USED_PROFILE SMALLINT not null,
  primary key (id)
);

create table RULE_FAILURES (
  ID INTEGER not null,
  SNAPSHOT_ID INTEGER not null,
  RULE_ID INTEGER not null,
  FAILURE_LEVEL INTEGER not null,
  MESSAGE VARCHAR(4000),
  LINE INTEGER,
  COST DECIMAL(30, 20),
  CREATED_AT TIMESTAMP,
  CHECKSUM VARCHAR(1000),
  PERMANENT_ID INTEGER,
  SWITCHED_OFF SMALLINT,
  primary key (id)
);
CREATE INDEX RF_PERMANENT_ID ON RULE_FAILURES (PERMANENT_ID);
CREATE INDEX RULE_FAILURE_RULE_ID ON RULE_FAILURES (RULE_ID);
CREATE INDEX RULE_FAILURE_SNAPSHOT_ID ON RULE_FAILURES (SNAPSHOT_ID);

create table SCHEMA_MIGRATIONS (
  VERSION VARCHAR(256) not null
);
CREATE UNIQUE INDEX UNIQUE_SCHEMA_MIGRATIONS ON SCHEMA_MIGRATIONS (VERSION);

create table SNAPSHOTS (
  ID INTEGER not null,
  CREATED_AT TIMESTAMP,
  PROJECT_ID INTEGER not null,
  PARENT_SNAPSHOT_ID INTEGER,
  STATUS VARCHAR(4) not null,
  ISLAST SMALLINT not null,
  SCOPE VARCHAR(3),
  QUALIFIER VARCHAR(3),
  ROOT_SNAPSHOT_ID INTEGER,
  VERSION VARCHAR(60),
  PATH VARCHAR(96),
  DEPTH INTEGER,
  ROOT_PROJECT_ID INTEGER,
  PERIOD1_MODE VARCHAR(100),
  PERIOD1_PARAM VARCHAR(100),
  PERIOD1_DATE TIMESTAMP,
  PERIOD2_MODE VARCHAR(100),
  PERIOD2_PARAM VARCHAR(100),
  PERIOD2_DATE TIMESTAMP,
  PERIOD3_MODE VARCHAR(100),
  PERIOD3_PARAM VARCHAR(100),
  PERIOD3_DATE TIMESTAMP,
  PERIOD4_MODE VARCHAR(100),
  PERIOD4_PARAM VARCHAR(100),
  PERIOD4_DATE TIMESTAMP,
  PERIOD5_MODE VARCHAR(100),
  PERIOD5_PARAM VARCHAR(100),
  PERIOD5_DATE TIMESTAMP,
  primary key (id)
);
CREATE INDEX SNAPSHOTS_PARENT ON SNAPSHOTS (PARENT_SNAPSHOT_ID);
CREATE INDEX SNAPSHOTS_QUALIFIER ON SNAPSHOTS (QUALIFIER);
CREATE INDEX SNAPSHOTS_ROOT ON SNAPSHOTS (ROOT_SNAPSHOT_ID);
CREATE INDEX SNAPSHOT_PROJECT_ID ON SNAPSHOTS (PROJECT_ID);

create table SNAPSHOT_SOURCES (
  ID INTEGER not null,
  SNAPSHOT_ID INTEGER not null,
  DATA CLOB,
  primary key (id)
);
CREATE INDEX SNAP_SOURCES_SNAPSHOT_ID ON SNAPSHOT_SOURCES (SNAPSHOT_ID);

create table USERS (
  ID INTEGER not null,
  LOGIN VARCHAR(40),
  NAME VARCHAR(200),
  EMAIL VARCHAR(100),
  CRYPTED_PASSWORD VARCHAR(40),
  SALT VARCHAR(40),
  CREATED_AT TIMESTAMP,
  UPDATED_AT TIMESTAMP,
  REMEMBER_TOKEN VARCHAR(500),
  REMEMBER_TOKEN_EXPIRES_AT TIMESTAMP,
  primary key (id)
);

create table USER_ROLES (
  ID INTEGER not null,
  USER_ID INTEGER,
  RESOURCE_ID INTEGER,
  ROLE VARCHAR(64) not null,
  primary key (id)
);
CREATE INDEX USER_ROLES_RESOURCE ON USER_ROLES (RESOURCE_ID);
CREATE INDEX USER_ROLES_USER ON USER_ROLES (USER_ID);

create table WIDGETS (
  ID INTEGER not null,
  DASHBOARD_ID INTEGER not null,
  WIDGET_KEY VARCHAR(256) not null,
  NAME VARCHAR(256),
  DESCRIPTION VARCHAR(1000),
  COLUMN_INDEX INTEGER,
  ROW_INDEX INTEGER,
  CONFIGURED SMALLINT,
  CREATED_AT TIMESTAMP,
  UPDATED_AT TIMESTAMP,
  primary key (id)
);
CREATE INDEX WIDGETS_DASHBOARDS ON WIDGETS (DASHBOARD_ID);
CREATE INDEX WIDGETS_WIDGETKEY ON WIDGETS (WIDGET_KEY);

create table WIDGET_PROPERTIES (
  ID INTEGER not null,
  WIDGET_ID INTEGER not null,
  KEE VARCHAR(100),
  TEXT_VALUE VARCHAR(4000),
  VALUE_TYPE VARCHAR(20),
  primary key (id)
);
CREATE INDEX WIDGET_PROPERTIES_WIDGETS ON WIDGET_PROPERTIES (WIDGET_ID);

