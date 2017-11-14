/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarqube.ws.client.emails;

/*
 * THIS FILE HAS BEEN AUTOMATICALLY GENERATED
 */

import java.util.List;

/**
 * Test email configuration by sending an email<br>Requires 'Administer System' permission.
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/emails/send">Further information about this action online (including a response example)</a>
 * @since 6.1
 */
public class SendRequest {

  private String message;
  private String subject;
  private String to;

  /**
   * Content of the email
   *
   * This is a mandatory parameter.
   */
  public SendRequest setMessage(String message) {
    this.message = message;
    return this;
  }

  public String getMessage() {
    return message;
  }

  /**
   * Subject of the email
   *
   * Example value: "Test Message from SonarQube"
   */
  public SendRequest setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  public String getSubject() {
    return subject;
  }

  /**
   * Email address
   *
   * This is a mandatory parameter.
   * Example value: "john@doo.com"
   */
  public SendRequest setTo(String to) {
    this.to = to;
    return this;
  }

  public String getTo() {
    return to;
  }
}
