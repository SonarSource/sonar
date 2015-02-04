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
package org.sonar.server.issue.notification;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.plugins.emailnotifications.api.EmailMessage;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NewIssuesEmailTemplateTest {

  NewIssuesEmailTemplate template;

  @Mock
  DefaultI18n i18n;

  @Before
  public void setUp() {
    EmailSettings settings = mock(EmailSettings.class);
    when(settings.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    template = new NewIssuesEmailTemplate(settings, i18n);
  }

  @Test
  public void shouldNotFormatIfNotCorrectNotification() {
    Notification notification = new Notification("other-notif");
    EmailMessage message = template.format(notification);
    assertThat(message).isNull();
  }

  /**
   * <pre>
   * Subject: Project Struts, new issues
   * From: Sonar
   *
   * Project: Foo
   * 32 new issues
   *
   * See it in SonarQube: http://nemo.sonarsource.org/drilldown/measures/org.sonar.foo:foo?metric=new_violations
   * </pre>
   */
  @Test
  public void shouldFormatCommentAdded() {
    Notification notification = new NewIssuesNotification()
      .setFieldValue("count", "32")
      .setFieldValue("count-INFO", "1")
      .setFieldValue("count-MINOR", "3")
      .setFieldValue("count-MAJOR", "10")
      .setFieldValue("count-CRITICAL", "5")
      .setFieldValue("count-BLOCKER", "0")
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("projectUuid", "ABCDE")
      .setFieldValue("projectDate", "2010-05-18T14:50:45+0000");

    when(i18n.message(any(Locale.class), eq("severity.BLOCKER"), anyString())).thenReturn("Blocker");
    when(i18n.message(any(Locale.class), eq("severity.CRITICAL"), anyString())).thenReturn("Critical");
    when(i18n.message(any(Locale.class), eq("severity.MAJOR"), anyString())).thenReturn("Major");
    when(i18n.message(any(Locale.class), eq("severity.MINOR"), anyString())).thenReturn("Minor");
    when(i18n.message(any(Locale.class), eq("severity.INFO"), anyString())).thenReturn("Info");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("new-issues/org.apache:struts");
    assertThat(message.getSubject()).isEqualTo("Struts: new issues");

    // TODO datetime to be completed when test is isolated from JVM timezone
    assertThat(message.getMessage()).startsWith("" +
      "Project: Struts\n" +
      "\n" +
      "32 new issues\n" +
      "\n" +
      "   Blocker: 0   Critical: 5   Major: 10   Minor: 3   Info: 1\n" +
      "\n" +
      "See it in SonarQube: http://nemo.sonarsource.org/issues/search#projectUuids=ABCDE|createdAt=2010-05-1");
  }

  @Test
  public void shouldNotAddFooterIfMissingProperties() {
    Notification notification = new NewIssuesNotification()
      .setFieldValue("count", "32")
      .setFieldValue("projectName", "Struts");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessage()).doesNotContain("See it");
  }
}
