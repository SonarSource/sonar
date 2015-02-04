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
package org.sonar.server.app;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class StartupLogsTest {

  Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
  Logger logger = mock(Logger.class);
  StartupLogs sut = new StartupLogs(logger);

  @Test
  public void logAjp() throws Exception {
    Connector connector = newConnector("AJP/1.3", "http");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});

    sut.log(tomcat);

    verify(logger).info("AJP/1.3 connector enabled on port 1234");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void logHttp() throws Exception {
    Connector connector = newConnector("HTTP/1.1", "http");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});

    sut.log(tomcat);

    verify(logger).info("HTTP connector enabled on port 1234");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void logHttps() throws Exception {
    Connector connector = mock(Connector.class, Mockito.RETURNS_DEEP_STUBS);
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});
    when(connector.getScheme()).thenReturn("https");
    when(connector.getProtocol()).thenReturn("HTTP/1.1");
    when(connector.getPort()).thenReturn(1234);
    AbstractHttp11JsseProtocol protocol = mock(AbstractHttp11JsseProtocol.class);
    when(connector.getProtocolHandler()).thenReturn(protocol);
    when(protocol.getCiphersUsed()).thenReturn(new String[] {"SSL_RSA", "TLS_RSA_WITH_RC4"});

    sut.log(tomcat);

    verify(logger).info("HTTPS connector enabled on port 1234 | ciphers=SSL_RSA,TLS_RSA_WITH_RC4");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void unsupported_connector() throws Exception {
    Connector connector = mock(Connector.class, Mockito.RETURNS_DEEP_STUBS);
    when(connector.getProtocol()).thenReturn("SPDY/1.1");
    when(connector.getScheme()).thenReturn("spdy");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});
    try {
      sut.log(tomcat);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  private Connector newConnector(String protocol, String schema) {
    Connector httpConnector = new Connector(protocol);
    httpConnector.setScheme(schema);
    httpConnector.setPort(1234);
    return httpConnector;
  }
}
