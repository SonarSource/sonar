/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.i18n;

import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;

import java.util.Locale;

/**
 *
 *
 *
 * EXPERIMENTAL - this feature will be fully implemented in version 2.10
 *
 * 
 *
 * The <code>I18n</code> Interface is the entry point for the internationalization of the Sonar application and plugins.<br>The corresponding implementation is located in the core plugin.
 * <p/>
 * I18n is managed in Sonar through the use of key-based resource bundles.
 * <br>
 * Though any key can be used, the following key-naming conventions, which are applied in the Sonar application and core plugins, are given as guidelines:
 *
 * @since 2.9
 */
public interface I18n extends ServerComponent, BatchComponent {

  /**
   * Searches the message of the <code>key</code> for the <code>locale</code> in the list of available bundles.
   * <br>
   * If not found in any bundle, <code>defaultText</code> is returned.
   * 
   * If additional parameters are given (in the objects list), the result is used as a message pattern 
   * to use in a MessageFormat object along with the given parameters.  
   *
   * @param locale the locale to translate into
   * @param key the key of the pattern to translate
   * @param defaultValue the default pattern returned when the key is not found in any bundle
   * @param parameters the parameters used to format the message from the translated pattern.
   * @return the message formatted with the translated pattern and the given parameters 
   */
  public abstract String message(final Locale locale, final String key, final String defaultValue, final Object... parameters);

  public abstract Locale getDefaultLocale();
}
