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
package org.sonar.server.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.security.Authenticator;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.LoginPasswordAuthenticator;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.utils.SonarException;

/**
 * @since 2.14
 */
public class SecurityRealmFactory implements ServerComponent, Startable {

  private final boolean ignoreStartupFailure;
  private final SecurityRealm realm;

  public SecurityRealmFactory(Settings settings,
      SecurityRealm[] realms,
      LoginPasswordAuthenticator[] loginPasswordAuthenticators) {
    ignoreStartupFailure = settings.getBoolean(CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE);

    SecurityRealm selectedRealm = selectExistingRealm(settings, realms);
    if (selectedRealm == null) {
      selectedRealm = selectLoginPasswordAuthenticator(settings, loginPasswordAuthenticators);
    }
    if (selectedRealm == null) {
      selectedRealm = selectCustomSecurityRealm(settings, realms);
    }
    realm = selectedRealm;
  }

  public SecurityRealmFactory(Settings settings, LoginPasswordAuthenticator[] authenticators) {
    this(settings, new SecurityRealm[0], authenticators);
  }

  public SecurityRealmFactory(Settings settings, SecurityRealm[] realms) {
    this(settings, realms, new LoginPasswordAuthenticator[0]);
  }

  public SecurityRealmFactory(Settings settings) {
    this(settings, new SecurityRealm[0], new LoginPasswordAuthenticator[0]);
  }

  @Override
  public void start() {
    Logger logger = LoggerFactory.getLogger(SecurityRealmFactory.class.getName());
    if (realm != null) {
      try {
        logger.info("Security realm: " + realm.getName());
        realm.init();
        logger.info("Security realm started");
      } catch (RuntimeException e) {
        if (ignoreStartupFailure) {
          logger.error("IGNORED - Security realm fails to start: " + e.getMessage());
        } else {
          throw new SonarException("Security realm fails to start: " + e.getMessage(), e);
        }
      }
    } else {
      logger.info("No security realm is set.");
    }
  }

  @Override
  public void stop() {
    // nothing
  }

  public SecurityRealm getRealm() {
    return realm;
  }

  private static SecurityRealm selectCustomSecurityRealm(Settings settings, SecurityRealm[] realms) {
    List<String> authenticatorNames = Arrays.asList(settings.getStringArray(CoreProperties.CORE_SECURITY_AUTHENTICATORS));
    List<String> userProviderNames = Arrays.asList(settings.getStringArray(CoreProperties.CORE_SECURITY_USER_PROVIDERS));
    List<String> groupProviderNames = Arrays.asList(settings.getStringArray(CoreProperties.CORE_SECURITY_GROUP_PROVIDERS));
    if (authenticatorNames.isEmpty() || userProviderNames.isEmpty() || groupProviderNames.isEmpty()) {
      return null;
    }
    List<Authenticator> authenticators = new ArrayList<Authenticator>();
    List<ExternalUsersProvider> userProviders = new ArrayList<ExternalUsersProvider>();
    List<ExternalGroupsProvider> groupProviders = new ArrayList<ExternalGroupsProvider>();
    for (SecurityRealm realm : realms) {
      selectAuthenticatorsByName(authenticatorNames, authenticators, realm);
      selectUserProvidersByName(userProviderNames, userProviders, realm);
      selectGroupProvidersByName(groupProviderNames, groupProviders, realm);
    }
    return new CustomSecurityRealm(authenticators, userProviders, groupProviders);
  }

  private static SecurityRealm selectLoginPasswordAuthenticator(Settings settings,
      LoginPasswordAuthenticator[] loginPasswordAuthenticators) {
    String className = settings.getString(CoreProperties.CORE_AUTHENTICATOR_CLASS);
    if (!StringUtils.isEmpty(className)) {
      LoginPasswordAuthenticator liPwAuthenticator = selectAuthenticator(loginPasswordAuthenticators, className);
      if (liPwAuthenticator == null) {
        throw new SonarException(String.format(
          "Authenticator '%s' not found. Please check the property '%s' in conf/sonar.properties", className, CoreProperties.CORE_AUTHENTICATOR_CLASS));
      }
      return new CompatibilityRealm(liPwAuthenticator);
    }
    return null;
  }

  private static SecurityRealm selectExistingRealm(Settings settings, SecurityRealm[] realms) {
    String realmName = settings.getString(CoreProperties.CORE_AUTHENTICATOR_REALM);
    SecurityRealm selectedRealm = null;
    if (!StringUtils.isEmpty(realmName)) {
      selectedRealm = selectRealm(realms, realmName);
      if (selectedRealm == null) {
        throw new SonarException(String.format(
          "Realm '%s' not found. Please check the property '%s' in conf/sonar.properties", realmName, CoreProperties.CORE_AUTHENTICATOR_REALM));
      }
    }
    return selectedRealm;
  }

  private static void selectGroupProvidersByName(List<String> groupProviderNames,
      List<ExternalGroupsProvider> groupProviders,
      SecurityRealm realm) {
    for (ExternalGroupsProvider realmGroupsProvider : realm.getGroupsProviders()) {
      if (groupProviderNames.contains(realmGroupsProvider.getClass().getName())) {
        groupProviders.add(realmGroupsProvider);
      }
    }
  }

  private static void selectUserProvidersByName(List<String> userProviderNames,
      List<ExternalUsersProvider> userProviders,
      SecurityRealm realm) {
    for (ExternalUsersProvider realmUsersProvider : realm.getUsersProviders()) {
      if (userProviderNames.contains(realmUsersProvider.getClass().getName())) {
        userProviders.add(realmUsersProvider);
      }
    }
  }

  private static void selectAuthenticatorsByName(List<String> authenticatorNames,
      List<Authenticator> authenticators,
      SecurityRealm realm) {
    for (Authenticator realmAuthenticator : realm.getAuthenticators()) {
      if (authenticatorNames.contains(realmAuthenticator.getClass().getName())) {
        authenticators.add(realmAuthenticator);
      }
    }
  }

  private static SecurityRealm selectRealm(SecurityRealm[] realms, String realmName) {
    for (SecurityRealm realm : realms) {
      if (StringUtils.equals(realmName, realm.getName())) {
        return realm;
      }
    }
    return null;
  }

  private static LoginPasswordAuthenticator selectAuthenticator(LoginPasswordAuthenticator[] authenticators, String className) {
    for (LoginPasswordAuthenticator lpa : authenticators) {
      if (lpa.getClass().getName().equals(className)) {
        return lpa;
      }
    }
    return null;
  }

}
