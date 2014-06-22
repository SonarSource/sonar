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

import com.google.common.base.Preconditions;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Christian Droescher
 */
public class StringExtractor {

  private final Pattern pattern;

  public StringExtractor(Settings settings) {
    String pattern = settings.getString(CoreProperties.REGEX_PATTERN_FOR_CERT);
    this.pattern = Pattern.compile(pattern);
  }

  public String extractFromCertificate(StringExtractor.Context context) {
    X509Certificate certs[] = (X509Certificate[])
            context.getRequest().getAttribute("javax.servlet.request.X509Certificate");
    if (certs == null) return null;
    String subject = certs[0].getSubjectX500Principal().getName();
    Matcher matcher = pattern.matcher(subject);
    if (matcher.find()) {
      return matcher.group();
    }
    return null;
  }

  public static final class Context {
    private HttpServletRequest request;

    public Context(HttpServletRequest request) {
      Preconditions.checkNotNull(request);
      this.request = request;
    }

    public HttpServletRequest getRequest() {
      return request;
    }
  }
}
